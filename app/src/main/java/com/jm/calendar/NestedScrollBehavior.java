package com.jm.calendar;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;

import com.jm.library.CalendarView;

/**
 * Created by tujiong on 2018/8/6.
 */
public class NestedScrollBehavior extends CoordinatorLayout.Behavior<NestedScrollView> {

    public NestedScrollBehavior() {
        super();
    }

    public NestedScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, NestedScrollView child, int layoutDirection) {
        parent.onLayoutChild(child, layoutDirection);
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view instanceof CalendarView) {
                child.offsetTopAndBottom(view.getHeight());
            }
        }
        return true;
    }

}
