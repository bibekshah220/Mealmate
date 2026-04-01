package com.example.mealmate.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.example.mealmate.R;
import com.example.mealmate.models.GroceryItem;

public class AddGroceryDialogFragment extends DialogFragment {

    private TextInputEditText itemNameInput;
    private TextInputEditText quantityInput;
    private TextInputEditText priceInput;
    private AutoCompleteTextView unitDropdown;
    private Button btnAdd;
    private Button btnCancel;
    private TextView dialogTitle;

    private GroceryItem editingItem;
    private GroceryDialogListener listener;

    public interface GroceryDialogListener {
        void onGroceryItemAdded(GroceryItem item);
        void onGroceryItemUpdated(GroceryItem item);
    }

    public static AddGroceryDialogFragment newInstance() {
        return new AddGroceryDialogFragment();
    }

    public static AddGroceryDialogFragment newInstance(GroceryItem item) {
        AddGroceryDialogFragment fragment = new AddGroceryDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("grocery_item", item);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            if (getParentFragment() instanceof GroceryDialogListener) {
                listener = (GroceryDialogListener) getParentFragment();
            } else if (context instanceof GroceryDialogListener) {
                listener = (GroceryDialogListener) context;
            } else {
                throw new ClassCastException(context.toString() + " must implement GroceryDialogListener");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement GroceryDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_grocery, null);

        // Initialize views
        itemNameInput = view.findViewById(R.id.item_name_input);
        quantityInput = view.findViewById(R.id.quantity_input);
        priceInput = view.findViewById(R.id.price_input);
        unitDropdown = view.findViewById(R.id.unit_dropdown);
        btnAdd = view.findViewById(R.id.btn_add);
        btnCancel = view.findViewById(R.id.btn_cancel);
        dialogTitle = view.findViewById(R.id.dialog_title);

        // Setup unit dropdown
        String[] units = new String[]{"piece", "kg", "g", "lb", "oz", "l", "ml", "cup", "tbsp", "tsp", "bunch"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                units
        );
        unitDropdown.setAdapter(adapter);

        // Check if we're editing an existing item
        if (getArguments() != null && getArguments().containsKey("grocery_item")) {
            editingItem = (GroceryItem) getArguments().getSerializable("grocery_item");
            populateFields(editingItem);
            dialogTitle.setText(R.string.edit_grocery_item);
            btnAdd.setText(R.string.update);
        } else {
            dialogTitle.setText(R.string.add_grocery_item);
            btnAdd.setText(R.string.add);
        }

        // Set up button actions
        btnCancel.setOnClickListener(v -> dismiss());
        btnAdd.setOnClickListener(v -> {
            if (validateInputs()) {
                saveGroceryItem();
                dismiss();
            }
        });

        builder.setView(view);
        return builder.create();
    }

    private void populateFields(GroceryItem item) {
        if (item != null) {
            itemNameInput.setText(item.getName());
            quantityInput.setText(String.valueOf(item.getQuantity()));
            unitDropdown.setText(item.getUnit(), false);
            
            if (item.getPrice() > 0) {
                priceInput.setText(String.valueOf(item.getPrice()));
            }
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String name = itemNameInput.getText().toString().trim();
        String quantityStr = quantityInput.getText().toString().trim();
        String unit = unitDropdown.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            itemNameInput.setError("Item name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(quantityStr)) {
            quantityInput.setError("Quantity is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(unit)) {
            unitDropdown.setError("Unit is required");
            isValid = false;
        }

        return isValid;
    }

    private void saveGroceryItem() {
        String name = itemNameInput.getText().toString().trim();
        double quantity = Double.parseDouble(quantityInput.getText().toString().trim());
        String unit = unitDropdown.getText().toString().trim();
        
        String priceStr = priceInput.getText().toString().trim();
        double price = TextUtils.isEmpty(priceStr) ? 0.0 : Double.parseDouble(priceStr);

        if (editingItem != null) {
            // Update existing item
            editingItem.setName(name);
            editingItem.setQuantity(quantity);
            editingItem.setUnit(unit);
            editingItem.setPrice(price);
            
            if (listener != null) {
                listener.onGroceryItemUpdated(editingItem);
            }
        } else {
            // Create new item
            GroceryItem newItem = new GroceryItem();
            newItem.setName(name);
            newItem.setQuantity(quantity);
            newItem.setUnit(unit);
            newItem.setPrice(price);
            newItem.setPurchased(false);
            
            if (listener != null) {
                listener.onGroceryItemAdded(newItem);
            }
        }
    }
} 