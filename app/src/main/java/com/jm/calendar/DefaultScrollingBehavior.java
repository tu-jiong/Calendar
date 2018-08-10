package com.jm.calendar;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.jm.library.CalendarView;

import java.util.List;

/**
 * Created by tujiong on 2018/8/8.
 */
public class DefaultScrollingBehavior extends CoordinatorLayout.Behavior {

    private int mTempTopBottomOffset = Integer.MIN_VALUE;

    public DefaultScrollingBehavior() {
        super();
    }

    public DefaultScrollingBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onMeasureChild(CoordinatorLayout parent, View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        final int childLpHeight = child.getLayoutParams().height;
        if (childLpHeight == ViewGroup.LayoutParams.MATCH_PARENT
                || childLpHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {

            final List<View> dependencies = parent.getDependencies(child);
            final CalendarView dependency = findDependencyCalendarView(dependencies);
            if (dependency != null) {
                if (ViewCompat.getFitsSystemWindows(dependency)
                        && !ViewCompat.getFitsSystemWindows(child)) {
                    ViewCompat.setFitsSystemWindows(child, true);
                    if (ViewCompat.getFitsSystemWindows(child)) {
                        child.requestLayout();
                        return true;
                    }
                }

                int availableHeight = View.MeasureSpec.getSize(parentHeightMeasureSpec);
                if (availableHeight == 0) {
                    availableHeight = parent.getHeight();
                }
                final int height = availableHeight - dependency.getWeekBarHeight() - dependency.getItemHeight();
                final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height,
                        childLpHeight == ViewGroup.LayoutParams.MATCH_PARENT
                                ? View.MeasureSpec.EXACTLY
                                : View.MeasureSpec.AT_MOST);
                parent.onMeasureChild(child, parentWidthMeasureSpec,
                        widthUsed, heightMeasureSpec, heightUsed);
                return true;
            }
        }
        return false;
    }

    private CalendarView findDependencyCalendarView(List<View> list) {
        if (list.isEmpty()) {
            return null;
        }
        for (View view : list) {
            if (view instanceof CalendarView) {
                return (CalendarView) view;
            }
        }
        return null;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        parent.onLayoutChild(child, layoutDirection);

        CalendarView dependency = findDependencyCalendarView(parent.getDependencies(child));
        if (dependency != null) {
            ViewCompat.offsetTopAndBottom(child, dependency.getHeight());
        }

        int layoutTop = child.getTop();
        if (mTempTopBottomOffset != Integer.MIN_VALUE) {
            ViewCompat.offsetTopAndBottom(child, mTempTopBottomOffset - layoutTop);
        }

        return true;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof CalendarView;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof CalendarView) {
            ViewGroup.LayoutParams params = dependency.getLayoutParams();
            if (params instanceof CoordinatorLayout.LayoutParams) {
                CoordinatorLayout.LayoutParams clp = (CoordinatorLayout.LayoutParams) params;
                CoordinatorLayout.Behavior behavior = clp.getBehavior();
                if (behavior instanceof DefaultBehavior) {
                    DefaultBehavior tb = (DefaultBehavior) behavior;
                    scroll(child, (CalendarView) dependency, tb.mOffset);
                }
            }
        }
        return true;
    }

    private void scroll(View child, CalendarView dependency, int dy) {
        int top = child.getTop();
        int min = dependency.getWeekBarHeight() + dependency.getItemHeight();
        int max = dependency.getHeight();

        if (dy < min - top) {
            dy = min - top;
        }
        if (dy > max - top) {
            dy = max - top;
        }
        if (top >= min && top <= max) {
            ViewCompat.offsetTopAndBottom(child, dy);
        }

        mTempTopBottomOffset = child.getTop();
    }
}
