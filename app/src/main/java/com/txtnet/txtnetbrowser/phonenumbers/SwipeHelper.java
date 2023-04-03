package com.txtnet.txtnetbrowser.phonenumbers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class SwipeHelper extends ItemTouchHelper.SimpleCallback {

    public static final int BUTTON_WIDTH = 300;
    private RecyclerView recyclerView;
    private List<UnderlayButton> buttons;
    private GestureDetector gestureDetector;
    private int swipedPos = -1;
    private float swipeThreshold = 0.5f;
    private Map<Integer, List<UnderlayButton>> buttonsBuffer;
    private Queue<Integer> recoverQueue;

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            for (UnderlayButton button : buttons){
                if(button.onClick(e.getX(), e.getY()))
                    break;
            }

            return true;
        }
    };

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent e) {
            if (swipedPos < 0) return false;
            Point point = new Point((int) e.getRawX(), (int) e.getRawY());

            RecyclerView.ViewHolder swipedViewHolder = recyclerView.findViewHolderForAdapterPosition(swipedPos);
            View swipedItem = swipedViewHolder.itemView;
            Rect rect = new Rect();
            swipedItem.getGlobalVisibleRect(rect);

            if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_UP ||e.getAction() == MotionEvent.ACTION_MOVE) {
                if (rect.top < point.y && rect.bottom > point.y)
                    gestureDetector.onTouchEvent(e);
                else {
                    recoverQueue.add(swipedPos);
                    swipedPos = -1;
                    recoverSwipedItem();
                }
            }
            return false;
        }
    };
    private Context context;
    public SwipeHelper(Context context, RecyclerView recyclerView) {
        super(0, ItemTouchHelper.LEFT);
        this.context = context;
        this.recyclerView = recyclerView;
        this.buttons = new ArrayList<>();
        this.gestureDetector = new GestureDetector(context, gestureListener);
        this.recyclerView.setOnTouchListener(onTouchListener);
        buttonsBuffer = new HashMap<>();
        recoverQueue = new LinkedList<Integer>(){
            @Override
            public boolean add(Integer o) {
                if (contains(o))
                    return false;
                else
                    return super.add(o);
            }
        };

        attachSwipe();
    }


    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();

        if (swipedPos != pos)
            recoverQueue.add(swipedPos);

        swipedPos = pos;

        if (buttonsBuffer.containsKey(swipedPos))
            buttons = buttonsBuffer.get(swipedPos);
        else
            buttons.clear();

        buttonsBuffer.clear();
        swipeThreshold = 0.5f * buttons.size() * BUTTON_WIDTH;
        recoverSwipedItem();
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return swipeThreshold;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return 0.1f * defaultValue;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return 5.0f * defaultValue;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        int pos = viewHolder.getAdapterPosition();
        float translationX = dX;
        View itemView = viewHolder.itemView;

        if (pos < 0){
            swipedPos = pos;
            return;
        }

        if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){
            if(dX < 0) {
                List<UnderlayButton> buffer = new ArrayList<>();

                if (!buttonsBuffer.containsKey(pos)){
                    instantiateUnderlayButton(viewHolder, buffer);
                    buttonsBuffer.put(pos, buffer);
                }
                else {
                    buffer = buttonsBuffer.get(pos);
                }

                translationX = dX * buffer.size() * BUTTON_WIDTH / itemView.getWidth();
                drawButtons(c, itemView, buffer, pos, translationX);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive);
    }

    private synchronized void recoverSwipedItem(){
        while (!recoverQueue.isEmpty()){
            int pos = recoverQueue.poll();
            if (pos > -1) {
                recyclerView.getAdapter().notifyItemChanged(pos);
            }
        }
    }

    private void drawButtons(Canvas c, View itemView, List<UnderlayButton> buffer, int pos, float dX){
        float right = itemView.getRight();
        float dButtonWidth = (-1) * dX / buffer.size();

        for (UnderlayButton button : buffer) {
            float left = right - dButtonWidth;
            button.onDraw(
                    c,
                    new RectF(
                            left,
                            itemView.getTop(),
                            right,
                            itemView.getBottom()
                    ),
                    pos
            );

            right = left;
        }
    }

    public void attachSwipe(){
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public abstract void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons);

    public static class UnderlayButton {
        private String text;
        private int imageResId;
        private int color;
        private int pos;
        private RectF clickRegion;
        private UnderlayButtonClickListener clickListener;

        public UnderlayButton(String text, int imageResId, int color, UnderlayButtonClickListener clickListener) {
            this.text = text;
            this.imageResId = imageResId;
            this.color = color;
            this.clickListener = clickListener;
        }

        public boolean onClick(float x, float y){
            if (clickRegion != null && clickRegion.contains(x, y)){
                clickListener.onClick(pos);
                return true;
            }

            return false;
        }

        public void onDraw(Canvas c, RectF rect, int pos){
            Paint p = new Paint();

            // Draw background
            p.setColor(color);
            c.drawRect(rect, p);

            // Draw Text
            p.setColor(Color.WHITE);
            p.setTextSize(12);

            Rect r = new Rect();
            float cHeight = rect.height();
            float cWidth = rect.width();
            p.setTextAlign(Paint.Align.LEFT);
            p.getTextBounds(text, 0, text.length(), r);
            float x = cWidth / 2f - r.width() / 2f - r.left;
            float y = cHeight / 2f + r.height() / 2f - r.bottom;
            c.drawText(text, rect.left + x, rect.top + y, p);

            clickRegion = rect;
            this.pos = pos;
        }
    }

    public interface UnderlayButtonClickListener {
        void onClick(int pos);
    }
}