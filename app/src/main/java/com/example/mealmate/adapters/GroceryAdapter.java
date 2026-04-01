package com.example.mealmate.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealmate.R;
import com.example.mealmate.models.GroceryItem;
import com.example.mealmate.views.StrikethroughTextView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.GroceryViewHolder> {

    private final Context context;
    private List<GroceryItem> groceryItems;
    private final GroceryListener listener;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    public GroceryAdapter(Context context, List<GroceryItem> groceryItems, GroceryListener listener) {
        this.context = context;
        this.groceryItems = groceryItems;
        this.listener = listener;
    }

    /**
     * Updates the list of grocery items and notifies the adapter
     * @param groceryItems New list of grocery items
     */
    public void setGroceryItems(List<GroceryItem> groceryItems) {
        this.groceryItems = groceryItems;
    }

    @NonNull
    @Override
    public GroceryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grocery, parent, false);
        return new GroceryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroceryViewHolder holder, int position) {
        GroceryItem item = groceryItems.get(position);
        
        holder.itemName.setText(item.getName());
        
        String quantityText = String.format(Locale.getDefault(), "%.1f %s", 
                item.getQuantity(), 
                item.getUnit() != null && !item.getUnit().isEmpty() ? item.getUnit() : "");
        holder.itemQuantity.setText(quantityText);
        
        if (item.getPrice() > 0) {
            holder.itemPrice.setVisibility(View.VISIBLE);
            holder.itemPrice.setText(currencyFormat.format(item.getPrice()));
        } else {
            holder.itemPrice.setVisibility(View.GONE);
        }
        
        holder.checkBox.setChecked(item.isPurchased());
        
        // Apply strikethrough effect on text if item is purchased
        if (item.isPurchased()) {
            holder.itemName.setStrikethrough(true);
            holder.itemName.setAlpha(0.5f);
            holder.itemQuantity.setAlpha(0.5f);
            holder.itemPrice.setAlpha(0.5f);
        } else {
            holder.itemName.setStrikethrough(false);
            holder.itemName.setAlpha(1.0f);
            holder.itemQuantity.setAlpha(1.0f);
            holder.itemPrice.setAlpha(1.0f);
        }
        
        // Apply special styling for manually added items
        if (item.isAddedManually() && !item.isPurchased()) {
            // Use a light version of the accent color as background
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.highlight_color));
            Log.d("GroceryAdapter", "Highlighting manually added item: " + item.getName());
        } else {
            // Reset to default background
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }
    }

    @Override
    public int getItemCount() {
        return groceryItems != null ? groceryItems.size() : 0;
    }

    public GroceryItem getItemAt(int position) {
        return groceryItems.get(position);
    }

    public class GroceryViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final StrikethroughTextView itemName;
        final TextView itemQuantity;
        final TextView itemPrice;
        final ImageButton deleteButton;
        final ImageButton editButton;

        public GroceryViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_grocery);
            itemName = itemView.findViewById(R.id.txt_grocery_name);
            itemQuantity = itemView.findViewById(R.id.txt_grocery_quantity);
            itemPrice = itemView.findViewById(R.id.txt_grocery_price);
            deleteButton = itemView.findViewById(R.id.btn_delete);
            editButton = itemView.findViewById(R.id.btn_edit);
            
            checkBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onGroceryCheckedChanged(groceryItems.get(position), checkBox.isChecked());
                }
            });
            
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onGroceryDeleted(groceryItems.get(position));
                }
            });
            
            editButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onGroceryEdit(groceryItems.get(position));
                }
            });
        }
    }

    public interface GroceryListener {
        void onGroceryCheckedChanged(GroceryItem item, boolean isChecked);
        void onGroceryDeleted(GroceryItem item);
        void onGroceryEdit(GroceryItem item);
    }
} 