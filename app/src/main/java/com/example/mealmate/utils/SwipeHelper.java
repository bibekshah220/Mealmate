package com.example.mealmate.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealmate.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SwipeHelper extends ItemTouchHelper.SimpleCallback {

    private final int leftColor;
    private final int rightColor;
    private final Drawable leftIcon;
    private final Drawable rightIcon;
    private final Paint paint = new Paint();
    private final Map<Integer, List<UnderlayButton>> buttonsBuffer = new HashMap<>();
    private final Context context;
    private RecyclerView recyclerView;
    private List<UnderlayButton> buttons;
    private int swipedPos = -1;
    private float swipeThreshold = 0.5f;
    private boolean swipeBack = false;

    public SwipeHelper(Context context, int leftDrawableId, int rightDrawableId, int leftColorId, int rightColorId) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        this.leftIcon = ContextCompat.getDrawable(context, leftDrawableId);
        this.rightIcon = ContextCompat.getDrawable(context, rightDrawableId);
        this.leftColor = ContextCompat.getColor(context, leftColorId);
        this.rightColor = ContextCompat.getColor(context, rightColorId);
        this.buttons = new ArrayList<>();
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (swipedPos >= 0 && e.getAction() == MotionEvent.ACTION_DOWN) {
                    // Reset swipe when touching down on any part of recyclerView
                    setItemsClickable(true);
                    swipeBack = false;
                    swipedPos = -1;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false; // We don't want to support moving items in the list
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();
        swipedPos = pos;

        if (buttonsBuffer.containsKey(pos)) {
            buttons = buttonsBuffer.get(pos);
        } else {
            buttons.clear();
        }

        if (direction == ItemTouchHelper.LEFT) {
            // Handle left swipe (delete)
            onLeftSwipe(viewHolder.getAdapterPosition());
        } else if (direction == ItemTouchHelper.RIGHT) {
            // Handle right swipe (mark as purchased)
            onRightSwipe(viewHolder.getAdapterPosition());
        }

        notifyItemChanged(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            setItemsClickable(false);

            View itemView = viewHolder.itemView;
            float height = (float) itemView.getBottom() - itemView.getTop();
            float width = height / 3;

            if (dX > 0) {
                // Swiping right (purchase)
                paint.setColor(rightColor);
                RectF background = new RectF(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom());
                c.drawRect(background, paint);

                if (rightIcon != null) {
                    int iconMargin = (int) ((itemView.getHeight() - rightIcon.getIntrinsicHeight()) / 2);
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + rightIcon.getIntrinsicHeight();
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = iconLeft + rightIcon.getIntrinsicWidth();

                    rightIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    rightIcon.draw(c);
                }
            } else if (dX < 0) {
                // Swiping left (delete)
                paint.setColor(leftColor);
                RectF background = new RectF(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                c.drawRect(background, paint);

                if (leftIcon != null) {
                    int iconMargin = (int) ((itemView.getHeight() - leftIcon.getIntrinsicHeight()) / 2);
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + leftIcon.getIntrinsicHeight();
                    int iconRight = itemView.getRight() - iconMargin;
                    int iconLeft = iconRight - leftIcon.getIntrinsicWidth();

                    leftIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    leftIcon.draw(c);
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void setItemsClickable(boolean isClickable) {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            recyclerView.getChildAt(i).setClickable(isClickable);
        }
    }

    protected void notifyItemChanged(int position) {
        recyclerView.getAdapter().notifyItemChanged(position);
    }

    // Abstract methods to be implemented by the calling class
    public abstract void onLeftSwipe(int position);
    public abstract void onRightSwipe(int position);

    // Button definition for the swipe-revealed areas
    public static class UnderlayButton {
        private final String text;
        private final int imageResId;
        private final int color;
        private final ButtonClickListener clickListener;

        public UnderlayButton(String text, int imageResId, int color, ButtonClickListener clickListener) {
            this.text = text;
            this.imageResId = imageResId;
            this.color = color;
            this.clickListener = clickListener;
        }

        public interface ButtonClickListener {
            void onClick();
        }
    }
} 