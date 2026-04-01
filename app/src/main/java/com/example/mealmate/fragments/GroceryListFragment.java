package com.example.mealmate.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.mealmate.R;
import com.example.mealmate.adapters.GroceryAdapter;
import com.example.mealmate.models.GroceryItem;
import com.example.mealmate.models.Recipe;
import com.example.mealmate.utils.ActivityTracker;
import com.example.mealmate.utils.ShakeDetector;
import com.example.mealmate.utils.SwipeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroceryListFragment extends Fragment implements AddGroceryDialogFragment.GroceryDialogListener, GroceryAdapter.GroceryListener {

    private static final String TAG = "GroceryListFragment";

    private RecyclerView toBuyRecyclerView;
    private RecyclerView purchasedRecyclerView;
    private RecyclerView manualItemsRecyclerView;
    private TextView noBuyItemsText;
    private TextView noPurchasedItemsText;
    private TextView noManualItemsText;
    private TextView totalCostText;
    private FloatingActionButton fabAddGrocery;

    private GroceryAdapter toBuyAdapter;
    private GroceryAdapter purchasedAdapter;
    private GroceryAdapter manualItemsAdapter;

    private List<GroceryItem> toBuyItems;
    private List<GroceryItem> purchasedItems;
    private List<GroceryItem> manualItems;
    
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grocery_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize views
        toBuyRecyclerView = view.findViewById(R.id.to_buy_recycler_view);
        purchasedRecyclerView = view.findViewById(R.id.purchased_recycler_view);
        manualItemsRecyclerView = view.findViewById(R.id.manual_items_recycler_view);
        noBuyItemsText = view.findViewById(R.id.no_to_buy_items_text);
        noPurchasedItemsText = view.findViewById(R.id.no_purchased_items_text);
        noManualItemsText = view.findViewById(R.id.no_manual_items_text);
        totalCostText = view.findViewById(R.id.total_cost_text);
        fabAddGrocery = view.findViewById(R.id.fab_add_grocery);

        // Initialize lists
        toBuyItems = new ArrayList<>();
        purchasedItems = new ArrayList<>();
        manualItems = new ArrayList<>();

        // Setup adapters and recyclerviews
        setupRecyclerViews();
        
        // Setup swipe gestures
        setupSwipeGestures();
        
        // Setup shake detector
        setupShakeDetector();

        // Setup FAB click listener
        fabAddGrocery.setOnClickListener(v -> showAddOptions());
        
        // Add share button to the to-buy section
        View shareButton = view.findViewById(R.id.btn_share_grocery);
        if (shareButton != null) {
            shareButton.setOnClickListener(v -> shareGroceryList());
        }
        
        // Load grocery items
        loadGroceryItems();
    }

    private void setupRecyclerViews() {
        // To Buy RecyclerView
        toBuyAdapter = new GroceryAdapter(requireContext(), toBuyItems, this);
        toBuyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        toBuyRecyclerView.setAdapter(toBuyAdapter);

        // Manual Items RecyclerView
        manualItemsAdapter = new GroceryAdapter(requireContext(), manualItems, this);
        manualItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        manualItemsRecyclerView.setAdapter(manualItemsAdapter);

        // Purchased RecyclerView
        purchasedAdapter = new GroceryAdapter(requireContext(), purchasedItems, this);
        purchasedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        purchasedRecyclerView.setAdapter(purchasedAdapter);
    }

    private void loadGroceryItems() {
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        
        Log.d(TAG, "Loading grocery items for user: " + userId);
        
        db.collection("groceryItems")
                .whereEqualTo("userId", userId)
                // Remove the orderBy which requires the composite index
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        toBuyItems.clear();
                        purchasedItems.clear();
                        manualItems.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            GroceryItem item = document.toObject(GroceryItem.class);
                            item.setId(document.getId());
                            
                            if (item.isPurchased()) {
                                purchasedItems.add(item);
                            } else if (item.isAddedManually()) {
                                manualItems.add(item);
                                Log.d(TAG, "Added manually added item: " + item.getName());
                            } else {
                                toBuyItems.add(item);
                            }
                        }
                        
                        Log.d(TAG, "Loaded items - To Buy: " + toBuyItems.size() + 
                              ", Manual: " + manualItems.size() + 
                              ", Purchased: " + purchasedItems.size());

                        // If all lists are empty, add some dummy data
                        if (toBuyItems.isEmpty() && purchasedItems.isEmpty() && manualItems.isEmpty()) {
                            Log.d(TAG, "No items found, adding dummy data");
                            addDummyGroceryItems();
                        } else {
                            updateUI();
                        }
                    } else {
                        Log.w(TAG, "Error getting grocery items", task.getException());
                        Toast.makeText(requireContext(), "Error loading grocery list", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper method to sort items by name
    private void sortItemsByName(List<GroceryItem> items) {
        if (items != null && !items.isEmpty()) {
            items.sort((item1, item2) -> item1.getName().compareToIgnoreCase(item2.getName()));
        }
    }

    private void updateUI() {
        // Sort items by name before updating the adapters
        sortItemsByName(toBuyItems);
        sortItemsByName(manualItems);
        sortItemsByName(purchasedItems);
        
        // Set the lists on adapters (to ensure referential integrity)
        toBuyAdapter.setGroceryItems(toBuyItems);
        manualItemsAdapter.setGroceryItems(manualItems);
        purchasedAdapter.setGroceryItems(purchasedItems);
        
        // Notify adapters
        toBuyAdapter.notifyDataSetChanged();
        manualItemsAdapter.notifyDataSetChanged();
        purchasedAdapter.notifyDataSetChanged();
        
        // Show/hide empty state text views
        noBuyItemsText.setVisibility(toBuyItems.isEmpty() ? View.VISIBLE : View.GONE);
        noManualItemsText.setVisibility(manualItems.isEmpty() ? View.VISIBLE : View.GONE);
        noPurchasedItemsText.setVisibility(purchasedItems.isEmpty() ? View.VISIBLE : View.GONE);
        
        // Calculate and display total cost
        calculateTotalCost();
    }

    private void calculateTotalCost() {
        double totalCost = 0.0;
        
        for (GroceryItem item : toBuyItems) {
            totalCost += item.getPrice() * item.getQuantity();
        }
        
        for (GroceryItem item : manualItems) {
            totalCost += item.getPrice() * item.getQuantity();
        }
        
        totalCostText.setText(String.format(getString(R.string.total_cost), totalCost));
    }

    private void showAddOptions() {
        String[] options = {getString(R.string.add_manually), getString(R.string.add_from_recipes)};
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.add_grocery_item))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Add manually
                        showAddGroceryDialog();
                    } else {
                        // Add from recipes
                        showRecipeSelectionDialog();
                    }
                })
                .show();
    }

    private void showAddGroceryDialog() {
        AddGroceryDialogFragment dialog = AddGroceryDialogFragment.newInstance();
        dialog.show(getChildFragmentManager(), "AddGroceryDialog");
    }

    private void showEditGroceryDialog(GroceryItem item) {
        AddGroceryDialogFragment dialog = AddGroceryDialogFragment.newInstance(item);
        dialog.show(getChildFragmentManager(), "EditGroceryDialog");
    }

    private void showRecipeSelectionDialog() {
        // Fetch recipes from Firebase
        if (currentUser == null) return;
        
        db.collection("recipes")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<Recipe> recipes = new ArrayList<>();
                        List<String> recipeNames = new ArrayList<>();
                        boolean[] selectedRecipes = new boolean[task.getResult().size()];
                        
                        int i = 0;
                        for (DocumentSnapshot doc : task.getResult()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe.setId(doc.getId());
                                recipes.add(recipe);
                                recipeNames.add(recipe.getName());
                                selectedRecipes[i] = false;
                                i++;
                            }
                        }
                        
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.select_recipes_for_grocery)
                                .setMultiChoiceItems(recipeNames.toArray(new String[0]), selectedRecipes, 
                                        (dialog, which, isChecked) -> selectedRecipes[which] = isChecked)
                                .setPositiveButton(R.string.add, (dialog, which) -> {
                                    List<Recipe> selectedRecipesList = new ArrayList<>();
                                    for (int j = 0; j < selectedRecipes.length; j++) {
                                        if (selectedRecipes[j]) {
                                            selectedRecipesList.add(recipes.get(j));
                                        }
                                    }
                                    addIngredientsFromRecipes(selectedRecipesList);
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    } else {
                        Toast.makeText(requireContext(), R.string.no_recipes, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addIngredientsFromRecipes(List<Recipe> selectedRecipes) {
        if (selectedRecipes.isEmpty() || currentUser == null) return;
        
        // Map to consolidate duplicate ingredients
        Map<String, GroceryItem> ingredientMap = new HashMap<>();
        
        // Process each recipe's ingredients
        for (Recipe recipe : selectedRecipes) {
            if (recipe.getIngredients() != null) {
                for (String ingredient : recipe.getIngredients()) {
                    // Simple parsing, assumes format like "2 cups flour" or "1 kg rice"
                    String[] parts = ingredient.trim().split("\\s+", 3);
                    
                    if (parts.length >= 2) {
                        try {
                            double quantity = Double.parseDouble(parts[0]);
                            String unit = parts.length >= 3 ? parts[1] : "piece";
                            String name = parts.length >= 3 ? parts[2] : parts[1];
                            
                            // Check if ingredient already in map
                            String key = name.toLowerCase() + "_" + unit.toLowerCase();
                            if (ingredientMap.containsKey(key)) {
                                GroceryItem existingItem = ingredientMap.get(key);
                                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                            } else {
                                GroceryItem item = new GroceryItem();
                                item.setName(name);
                                item.setQuantity(quantity);
                                item.setUnit(unit);
                                item.setPrice(0.0);
                                item.setPurchased(false);
                                item.setAddedManually(false);
                                item.setUserId(currentUser.getUid());
                                
                                ingredientMap.put(key, item);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Could not parse quantity from ingredient: " + ingredient);
                        }
                    } else {
                        // If format not as expected, just add as is
                        GroceryItem item = new GroceryItem();
                        item.setName(ingredient);
                        item.setQuantity(1);
                        item.setUnit("piece");
                        item.setPrice(0.0);
                        item.setPurchased(false);
                        item.setAddedManually(false);
                        item.setUserId(currentUser.getUid());
                        
                        String key = ingredient.toLowerCase() + "_piece";
                        ingredientMap.put(key, item);
                    }
                }
            }
        }
        
        // Add all items to Firestore
        for (GroceryItem item : ingredientMap.values()) {
            addGroceryItemToFirestore(item);
        }
    }

    private void addGroceryItemToFirestore(GroceryItem item) {
        if (currentUser == null) return;
        
        db.collection("groceryItems")
                .add(item)
                .addOnSuccessListener(documentReference -> {
                    item.setId(documentReference.getId());
                    
                    // Add to the appropriate list based on item properties
                    if (item.isPurchased()) {
                        purchasedItems.add(item);
                    } else if (item.isAddedManually()) {
                        manualItems.add(item);
                    } else {
                        toBuyItems.add(item);
                    }
                    
                    updateUI();
                    Toast.makeText(requireContext(), R.string.grocery_item_added, Toast.LENGTH_SHORT).show();
                    showItemAddedNotification(item);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding grocery item", e);
                    Toast.makeText(requireContext(), R.string.error_adding_grocery, Toast.LENGTH_SHORT).show();
                });
    }

    private void updateGroceryItemInFirestore(GroceryItem item) {
        if (currentUser == null || item.getId() == null) return;
        
        db.collection("groceryItems")
                .document(item.getId())
                .set(item)
                .addOnSuccessListener(aVoid -> {
                    updateItemInLists(item);
                    updateUI();
                    
                    String message = item.isPurchased() ? 
                            getString(R.string.grocery_item_purchased) : 
                            getString(R.string.grocery_item_updated);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating grocery item", e);
                    Toast.makeText(requireContext(), R.string.error_updating_grocery, Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteGroceryItem(GroceryItem item) {
        if (currentUser == null || item.getId() == null) return;
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_grocery_item)
                .setMessage(R.string.confirm_delete_grocery)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    db.collection("groceryItems")
                            .document(item.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                removeItemFromLists(item);
                                updateUI();
                                Toast.makeText(requireContext(), R.string.grocery_item_deleted, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting grocery item", e);
                                Toast.makeText(requireContext(), R.string.error_deleting_grocery, Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateItemInLists(GroceryItem item) {
        // Remove from all lists first
        removeItemFromLists(item);
        
        // Add to appropriate list - purchased status takes priority over manually added status
        if (item.isPurchased()) {
            purchasedItems.add(item);
        } else if (item.isAddedManually()) {
            manualItems.add(item);
        } else {
            toBuyItems.add(item);
        }
    }

    private void removeItemFromLists(GroceryItem item) {
        // Remove from to-buy list
        for (int i = 0; i < toBuyItems.size(); i++) {
            if (toBuyItems.get(i).getId().equals(item.getId())) {
                toBuyItems.remove(i);
                break;
            }
        }
        
        // Remove from manual items list
        for (int i = 0; i < manualItems.size(); i++) {
            if (manualItems.get(i).getId().equals(item.getId())) {
                manualItems.remove(i);
                break;
            }
        }
        
        // Remove from purchased list
        for (int i = 0; i < purchasedItems.size(); i++) {
            if (purchasedItems.get(i).getId().equals(item.getId())) {
                purchasedItems.remove(i);
                break;
            }
        }
    }

    // AddGroceryDialogFragment.GroceryDialogListener implementation
    @Override
    public void onGroceryItemAdded(GroceryItem item) {
        if (currentUser == null) return;
        
        item.setUserId(currentUser.getUid());
        // Mark this item as added manually
        item.setAddedManually(true);
        
        // Add to Firestore - this will handle adding to the correct list
        addGroceryItemToFirestore(item);
    }

    @Override
    public void onGroceryItemUpdated(GroceryItem item) {
        updateGroceryItemInFirestore(item);
    }

    // GroceryAdapter.GroceryListener implementation
    @Override
    public void onGroceryCheckedChanged(GroceryItem item, boolean isChecked) {
        // Toggle purchased status
        item.setPurchased(isChecked);
        updateGroceryItemInFirestore(item);
    }

    @Override
    public void onGroceryDeleted(GroceryItem item) {
        deleteGroceryItem(item);
    }

    @Override
    public void onGroceryEdit(GroceryItem item) {
        showEditGroceryDialog(item);
    }

    private void shareGroceryList() {
        if (toBuyItems.isEmpty() && manualItems.isEmpty()) {
            Toast.makeText(requireContext(), "No items to share", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] options = {"Share via apps", "Send SMS"};
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.share_grocery_list)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Share via general sharing
                        shareViaApps();
                    } else {
                        // Send SMS option
                        showSendSmsDialog();
                    }
                })
                .show();
    }
    
    private void shareViaApps() {
        String message = createGroceryListMessage();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Grocery List");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        
        startActivity(Intent.createChooser(shareIntent, "Share Grocery List"));
    }
    
    private void showSendSmsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sms_grocery, null);
        EditText phoneNumberInput = dialogView.findViewById(R.id.phone_number_input);
        TextView messagePreview = dialogView.findViewById(R.id.message_preview);
        Button copyButton = dialogView.findViewById(R.id.btn_copy_to_clipboard);
        
        String message = createGroceryListMessage();
        messagePreview.setText(message);
        
        copyButton.setOnClickListener(v -> {
            copyToClipboard(message);
            Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        });
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.send_grocery_sms)
                .setView(dialogView)
                .setPositiveButton(R.string.send, (dialog, which) -> {
                    String phoneNumber = phoneNumberInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        sendSms(phoneNumber, message);
                    } else {
                        Toast.makeText(requireContext(), R.string.enter_phone_number, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Grocery List", text);
        clipboard.setPrimaryClip(clip);
    }
    
    private void sendSms(String phoneNumber, String message) {
        try {
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + phoneNumber));
            smsIntent.putExtra("sms_body", message);
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.sms_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error sending SMS", e);
        }
    }
    
    private String createGroceryListMessage() {
        StringBuilder messageBuilder = new StringBuilder("GROCERY LIST TO BUY:\n");
        messageBuilder.append("--------------------------------\n\n");
        double totalCost = 0;
        
        // Add regular to-buy items
        if (!toBuyItems.isEmpty()) {
            messageBuilder.append("ITEMS FROM RECIPES:\n");
            for (GroceryItem item : toBuyItems) {
                double itemTotal = item.getPrice() * item.getQuantity();
                totalCost += itemTotal;
                
                messageBuilder.append(String.format("• %s (%.1f %s)", 
                        item.getName(), 
                        item.getQuantity(), 
                        item.getUnit()));
                
                if (item.getPrice() > 0) {
                    messageBuilder.append(String.format(" - $%.2f", itemTotal));
                }
                
                messageBuilder.append("\n");
            }
        }
        
        // Add manually added items
        if (!manualItems.isEmpty()) {
            messageBuilder.append("\nMANUALLY ADDED ITEMS:\n");
            for (GroceryItem item : manualItems) {
                double itemTotal = item.getPrice() * item.getQuantity();
                totalCost += itemTotal;
                
                messageBuilder.append(String.format("• %s (%.1f %s)", 
                        item.getName(), 
                        item.getQuantity(), 
                        item.getUnit()));
                
                if (item.getPrice() > 0) {
                    messageBuilder.append(String.format(" - $%.2f", itemTotal));
                }
                
                messageBuilder.append("\n");
            }
        }
        
        messageBuilder.append("\n");
        messageBuilder.append(String.format("Total Cost: $%.2f", totalCost));
        messageBuilder.append("\n\n--------------------------------\n");
        messageBuilder.append("Happy shopping! 🛒");
        
        return messageBuilder.toString();
    }

    /**
     * Adds dummy grocery items to both toBuy and purchased sections for demonstration
     */
    private void addDummyGroceryItems() {
        if (currentUser == null) return;
        
        // Items to Buy from recipes (non-manual items)
        addDummyItemToFirestore("Milk", 1.0, "gallon", 3.99, false, false);
        addDummyItemToFirestore("Bread", 1.0, "loaf", 2.49, false, false);
        
        // Manually added items
        addDummyItemToFirestore("Chicken Breast", 2.0, "lb", 7.99, false, true);
        addDummyItemToFirestore("Bananas", 6.0, "piece", 0.99, false, true);
        addDummyItemToFirestore("Spinach", 1.0, "bag", 2.99, false, true);
        
        // Purchased Items
        addDummyItemToFirestore("Rice", 5.0, "lb", 8.99, true, false);
        addDummyItemToFirestore("Tomatoes", 4.0, "piece", 3.20, true, false);
        addDummyItemToFirestore("Olive Oil", 1.0, "bottle", 8.99, true, false);
    }
    
    private void addDummyItemToFirestore(String name, double quantity, String unit, double price, boolean purchased, boolean addedManually) {
        GroceryItem item = new GroceryItem();
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setPrice(price);
        item.setPurchased(purchased);
        item.setAddedManually(addedManually);
        item.setUserId(currentUser.getUid());
        
        db.collection("groceryItems")
                .add(item)
                .addOnSuccessListener(documentReference -> {
                    item.setId(documentReference.getId());
                    if (purchased) {
                        purchasedItems.add(item);
                    } else if (item.isAddedManually()) {
                        manualItems.add(item);
                    } else {
                        toBuyItems.add(item);
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding dummy grocery item", e);
                });
    }

    private void showItemAddedNotification(GroceryItem item) {
        // Create and show a Snackbar with item details
        View rootView = getView();
        if (rootView != null) {
            String message = getString(R.string.added_to_grocery_list, item.getName());
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
            
            // Add an action to jump to the item in the list
            snackbar.setAction(R.string.view, v -> {
                // Determine which RecyclerView and list to use
                final RecyclerView targetRecyclerView;
                final List<GroceryItem> targetList;
                
                if (item.isPurchased()) {
                    targetRecyclerView = purchasedRecyclerView;
                    targetList = purchasedItems;
                } else if (item.isAddedManually()) {
                    targetRecyclerView = manualItemsRecyclerView;
                    targetList = manualItems;
                } else {
                    targetRecyclerView = toBuyRecyclerView;
                    targetList = toBuyItems;
                }
                
                // Find position of the item in the list
                final int position = targetList.indexOf(item);
                if (position != -1) {
                    // First, update the UI to ensure lists are sorted and adapters updated
                    updateUI();
                    
                    // Scroll to the item position
                    targetRecyclerView.smoothScrollToPosition(position);
                    
                    // Use a handler to wait for scrolling to complete before highlighting
                    new android.os.Handler().postDelayed(() -> {
                        // Find the item view after scrolling has completed
                        View itemView = targetRecyclerView.getLayoutManager().findViewByPosition(position);
                        if (itemView != null) {
                            // Create a fade-in/fade-out animation for the highlight
                            int highlightColor = getResources().getColor(R.color.highlight_color);
                            int transparentColor = getResources().getColor(android.R.color.transparent);
                            
                            // Store original background
                            itemView.setBackgroundColor(highlightColor);
                            
                            // Animate back to transparent over 800ms
                            itemView.animate()
                                .setDuration(800)
                                .withEndAction(() -> 
                                    itemView.setBackgroundColor(transparentColor))
                                .start();
                        }
                    }, 300); // Wait 300ms for smooth scroll to complete
                }
            });
            
            snackbar.show();
        }
        
        // Track the activity for analytics
        ActivityTracker.trackGroceryActivity(
            requireContext(),
            getString(R.string.activity_added_grocery_item, item.getName()),
            getString(R.string.activity_added_grocery_item_desc, item.getQuantity(), item.getUnit())
        );
    }

    private void setupSwipeGestures() {
        // Swipe helper for to-buy items
        SwipeHelper toBuySwipeHelper = new SwipeHelper(
                requireContext(),
                R.drawable.ic_delete_sweep,
                R.drawable.ic_check_circle,
                R.color.swipe_delete_background,
                R.color.swipe_purchase_background
        ) {
            @Override
            public void onLeftSwipe(int position) {
                // Delete action
                GroceryItem item = toBuyAdapter.getItemAt(position);
                showDeleteConfirmation(item);
            }

            @Override
            public void onRightSwipe(int position) {
                // Mark as purchased action
                GroceryItem item = toBuyAdapter.getItemAt(position);
                item.setPurchased(true);
                updateGroceryItemInFirestore(item);
                
                showPurchaseConfirmation(item);
            }
        };
        toBuySwipeHelper.attachToRecyclerView(toBuyRecyclerView);
        
        // Swipe helper for manual items
        SwipeHelper manualSwipeHelper = new SwipeHelper(
                requireContext(),
                R.drawable.ic_delete_sweep,
                R.drawable.ic_check_circle,
                R.color.swipe_delete_background,
                R.color.swipe_purchase_background
        ) {
            @Override
            public void onLeftSwipe(int position) {
                // Delete action
                GroceryItem item = manualItemsAdapter.getItemAt(position);
                showDeleteConfirmation(item);
            }

            @Override
            public void onRightSwipe(int position) {
                // Mark as purchased action
                GroceryItem item = manualItemsAdapter.getItemAt(position);
                item.setPurchased(true);
                updateGroceryItemInFirestore(item);
                
                showPurchaseConfirmation(item);
            }
        };
        manualSwipeHelper.attachToRecyclerView(manualItemsRecyclerView);
        
        // Swipe helper for purchased items (only delete)
        SwipeHelper purchasedSwipeHelper = new SwipeHelper(
                requireContext(),
                R.drawable.ic_delete_sweep,
                R.drawable.ic_delete_sweep, // We don't use right swipe for purchased items
                R.color.swipe_delete_background,
                R.color.swipe_delete_background
        ) {
            @Override
            public void onLeftSwipe(int position) {
                // Delete action
                GroceryItem item = purchasedAdapter.getItemAt(position);
                showDeleteConfirmation(item);
            }

            @Override
            public void onRightSwipe(int position) {
                // Not used for purchased items
            }
            
            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // Only allow left swipe (delete) for purchased items
                return ItemTouchHelper.LEFT;
            }
        };
        purchasedSwipeHelper.attachToRecyclerView(purchasedRecyclerView);
    }
    
    private void setupShakeDetector() {
        // Get the sensor manager and accelerometer
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        // Initialize the shake detector
        shakeDetector = new ShakeDetector();
        shakeDetector.setOnShakeListener(() -> {
            // Navigate to add grocery item when device is shaken
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Shake detected! Add an item.", Toast.LENGTH_SHORT).show();
                    showAddGroceryDialog();
                });
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Register the shake detector when the fragment resumes
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // Unregister the shake detector when the fragment pauses
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
    }
    
    private void showDeleteConfirmation(GroceryItem item) {
        Snackbar.make(
                requireView(),
                "Delete " + item.getName() + "?",
                Snackbar.LENGTH_LONG
        ).setAction("UNDO", v -> {
            // Refresh the adapter to undo the visual change
            updateUI();
        }).addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    // Delete the item if the user didn't press UNDO
                    deleteGroceryItem(item);
                }
            }
        }).show();
    }
    
    private void showPurchaseConfirmation(GroceryItem item) {
        Snackbar.make(
                requireView(),
                "Marked " + item.getName() + " as purchased",
                Snackbar.LENGTH_LONG
        ).setAction("UNDO", v -> {
            // Undo the purchase action
            item.setPurchased(false);
            updateGroceryItemInFirestore(item);
        }).show();
    }
} 