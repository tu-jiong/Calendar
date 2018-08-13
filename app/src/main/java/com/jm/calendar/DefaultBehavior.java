package com.jm.calendar;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.jm.library.CalendarView;
import com.jm.library.MonthViewPager;

/**
 * Created by tujiong on 2018/8/8.
 */
public class DefaultBehavior extends CoordinatorLayout.Behavior<CalendarView> {

    public int mOffset;
    private int mTempTopBottomOffset = Integer.MIN_VALUE;
    private int mDownX;
    private int mDownY;
    private int mLastX;
    private int mLastY;
    private int mTouchSlop;
    private boolean isAnim;

    public DefaultBehavior() {
        super();
    }

    public DefaultBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, CalendarView child, int layoutDirection) {
        parent.onLayoutChild(child, layoutDirection);

        int layoutTop = child.getMonthPager().getTop();
        if (mTempTopBottomOffset != Integer.MIN_VALUE) {
            ViewCompat.offsetTopAndBottom(child.getMonthPager(), mTempTopBottomOffset - layoutTop);
        }

        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, CalendarView child, MotionEvent ev) {
        final int action = ev.getAction();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = mDownX = x;
                mLastY = mDownY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = x - mLastX;
                int dy = y - mLastY;
                int range = child.getHeight() - child.getWeekBarHeight() - child.getItemHeight();
                int top = child.getMonthPager().getTop();
                int min = child.getWeekBarHeight() - range;
                int max = child.getWeekBarHeight();

                if ((dy < 0 && top <= min) || (dy > 0 && top >= max)) {
                    return false;
                }

//                if (dispatchTouchEvent(parent, dy)) {
//                    return false;
//                }

                if (Math.abs(dx) < Math.abs(dy) && Math.abs(dy) > mTouchSlop) {
                    mLastX = x;
                    mLastY = y;
                    return true;
                }

                break;
        }
        return super.onInterceptTouchEvent(parent, child, ev);
    }

    private boolean dispatchTouchEvent(CoordinatorLayout parent, float dy) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) child;
                return (dy > 0 && recyclerView.canScrollVertically(-1)) || (dy < 0 && recyclerView.canScrollVertically(1));
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, CalendarView child, MotionEvent ev) {
        int action = ev.getAction();
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = mDownX = x;
                mLastY = mDownY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                mOffset = y - mLastY;
                scroll(child, mOffset);
                mLastX = x;
                mLastY = y;
                parent.dispatchDependentViewsChanged(child);
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_UP:
                int range = child.getHeight() - child.getWeekBarHeight() - child.getItemHeight();
                int top = child.getMonthPager().getTop();
                int min = child.getWeekBarHeight() - range;
                int max = child.getWeekBarHeight();

                if (y - mDownY < 0) {
                    fling(parent, child, top, min);
                } else {
                    fling(parent, child, top, max);
                }
                break;
        }
        return true;
    }

    private void fling(final CoordinatorLayout parent, final CalendarView child, int start, int end) {
        if (isAnim) {
            return;
        }
        isAnim = true;
        ValueAnimator valueAnimator = ValueAnimator.ofInt(start, end);
        valueAnimator.setDuration(300);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                mOffset = value - child.getMonthPager().getTop();
                scroll(child, mOffset);
                parent.dispatchDependentViewsChanged(child);
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnim = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.start();
    }

    private void scroll(CalendarView view, int dy) {
        int range = view.getHeight() - view.getWeekBarHeight() - view.getItemHeight();
        int top = view.getMonthPager().getTop();
        int min = view.getWeekBarHeight() - range;
        int max = view.getWeekBarHeight();

        if (dy < min - top) {
            dy = min - top;
        }
        if (dy > max - top) {
            dy = max - top;
        }

        if (top >= min && top <= max) {
            ViewCompat.offsetTopAndBottom(view.getMonthPager(), dy);
        }
        MonthViewPager monthPager = view.getMonthPager();

        top = monthPager.getTop();
        int positionInMonth = view.getPositionInMonth();

        int line = (positionInMonth + 7) / 7;

        int scrollRange = (line - 1) * view.getItemHeight();
        if (top < view.getWeekBarHeight() - scrollRange) {
            view.showWeek();
        } else {
            view.hideWeek();
        }

        mTempTopBottomOffset = view.getMonthPager().getTop();
    }
}
