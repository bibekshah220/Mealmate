package com.example.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealmate.R;
import com.example.mealmate.models.ActivityItem;

import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder> {

    private final Context context;
    private final List<ActivityItem> activityItems;

    public RecentActivityAdapter(Context context, List<ActivityItem> activityItems) {
        this.context = context;
        this.activityItems = activityItems;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityItem activity = activityItems.get(position);
        
        holder.titleTextView.setText(activity.getTitle());
        holder.timeTextView.setText(activity.getFormattedTime());
        
        // Set icon based on activity type
        switch (activity.getType()) {
            case "recipe":
                holder.iconImageView.setImageResource(R.drawable.ic_recipe);
                break;
            case "meal_plan":
                holder.iconImageView.setImageResource(R.drawable.ic_meal_plan);
                break;
            case "grocery":
                holder.iconImageView.setImageResource(R.drawable.ic_grocery);
                break;
            default:
                holder.iconImageView.setImageResource(R.drawable.ic_recipe);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return activityItems.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView titleTextView;
        TextView timeTextView;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.img_activity_icon);
            titleTextView = itemView.findViewById(R.id.tv_activity_title);
            timeTextView = itemView.findViewById(R.id.tv_activity_time);
        }
    }
} 