package com.jm.calendar;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.jm.library.CalendarView;

/**
 * Created by ldf on 17/6/15.
 */

public class CalendarBehavior extends CoordinatorLayout.Behavior<CalendarView> {

    private static final int ACTIVE_POINTER = 1;
    private static final int INVALID_POINTER = -1;

    private int mActivePointerId;
    private float downY;
    private float mLastY;
    private int mTouchSlop;
    private int mMaximumVelocity;

    private VelocityTracker mVelocityTracker;

    public CalendarBehavior() {
        super();
    }

    public CalendarBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mVelocityTracker = VelocityTracker.obtain();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, CalendarView child, int layoutDirection) {
        parent.onLayoutChild(child, layoutDirection);
        return true;
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, CalendarView child, MotionEvent ev) {
        int action = ev.getAction();
        float y = ev.getY();
        mVelocityTracker.addMovement(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                mLastY = downY = y;
                return true;
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int idx = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, idx);
                if (mActivePointerId == 0) {
                    //核心代码：就是让下面的 dy = y- mLastY == 0，避免抖动
                    mLastY = MotionEventCompat.getY(ev, mActivePointerId);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE:
                getPointerIndex(ev, mActivePointerId);
                if (mActivePointerId == INVALID_POINTER) {
                    //如果切换了手指，那把mLastY换到最新手指的y坐标即可，核心就是让下面的 dy== 0，避免抖动
                    mLastY = y;
                    mActivePointerId = ACTIVE_POINTER;
                }

                float dy = y - mLastY;
                //向上滑动，并且contentView平移到最大距离，显示周视图
                if (dy < 0 && child.getHeight() == child.getItemHeight() + child.getWeekBarHeight()) {
                    child.showWeek();
                    return false;
                }
                child.hideWeek();

                //向下滑动，并且contentView已经完全到底部
                if (dy > 0) {
                    translation(child, dy);
                    return super.onTouchEvent(parent, child, ev);
                }

                //向上滑动，并且contentView已经平移到最大距离，则contentView平移到最大的距离
                if (dy < 0) {
                    translation(child, dy);
                    return super.onTouchEvent(parent, child, ev);
                }
                //否则按比例平移
//                translationViewPager();
                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:

            case MotionEvent.ACTION_POINTER_UP:
                int pointerIndex = getPointerIndex(ev, mActivePointerId);
                if (mActivePointerId == INVALID_POINTER)
                    break;
                mLastY = MotionEventCompat.getY(ev, pointerIndex);
                break;
            case MotionEvent.ACTION_UP:

                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                float mYVelocity = velocityTracker.getYVelocity();
                if (child.getHeight() == child.getItemHeight()
                        || child.getHeight() == child.getItemHeight() * 6) {
                    break;
                }
                if (Math.abs(mYVelocity) >= 800) {
                    if (mYVelocity < 0) {
                        child.shrink();
                    } else {
                        child.expand();
                    }
                    return super.onTouchEvent(parent, child, ev);
                }
                if (ev.getY() - downY > 0) {
                    child.expand();
                } else {
                    child.shrink();
                }
                break;
        }
        return super.onTouchEvent(parent, child, ev);
    }

    private void translation(CalendarView child, float dy) {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params != null) {
            int height = (int) (child.getHeight() + dy);
            int minHeight = child.getItemHeight() + child.getWeekBarHeight();
            if (height < minHeight) {
                height = minHeight;
            }
            int fullHeight = child.getFullHeight();
            if (height > fullHeight) {
                height = fullHeight;
            }
            params.height = height;
            child.setLayoutParams(params);
        }
    }

    private int getPointerIndex(MotionEvent ev, int id) {
        int activePointerIndex = MotionEventCompat.findPointerIndex(ev, id);
        if (activePointerIndex == -1) {
            mActivePointerId = INVALID_POINTER;
        }
        return activePointerIndex;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, CalendarView child, MotionEvent ev) {
        final int action = ev.getAction();
        float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                mLastY = downY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                int height = child.getHeight();
                int max = child.getFullHeight();
                int min = child.getItemHeight() + child.getWeekBarHeight();
                if ((dy < 0 && height <= min) || (dy > 0 && height >= max)) {
                    return false;
                }
                if (dispatchTouchEvent(parent, dy)) {
                    return false;
                }
                 /*
                   如果向上滚动，且ViewPager已经收缩，不拦截事件
                 */
                if (dy < 0 && child.getHeight() == child.getItemHeight() + child.getWeekBarHeight()) {
                    return false;
                }
                /*
                 * 如果向下滚动，有 2 种情况处理 且y在ViewPager下方
                 * 1、RecyclerView 或者其它滚动的View，当mContentView滚动到顶部时，拦截事件
                 * 2、非滚动控件，直接拦截事件
                 */
                if (dy > 0 && child.getHeight() == child.getFullHeight()) {
                    return false;
                }
                if (Math.abs(dy) > mTouchSlop) {//大于mTouchSlop开始拦截事件，ContentView和ViewPager得到CANCEL事件
                    if ((dy > 0 && child.getHeight() < child.getFullHeight())
                            || (dy < 0 && child.getHeight() > child.getItemHeight() + child.getWeekBarHeight())) {
                        mLastY = y;
                        return true;
                    }
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
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int first = layoutManager.findFirstCompletelyVisibleItemPosition();
                int last = layoutManager.findLastCompletelyVisibleItemPosition();
                return (dy > 0 && last == recyclerView.getAdapter().getItemCount() - 1) || (dy < 0 && first == 0);
            }
        }
        return false;
    }
}
