package com.jm.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import static com.jm.library.BaseView.TEXT_SIZE;

/**
 * 星期栏，如果你要使用星期栏自定义，切记XML使用 merge，不要使用LinearLayout
 * Created by huanghaibin on 2017/11/30.
 */
public class WeekBar extends View {

    /**
     * 每一项的宽度
     */
    protected int mItemWidth;

    /**
     * 高度
     */
    protected int mHeight;

    /**
     * Text的基线
     */
    protected float mTextBaseLine;

    /**
     * 当前月份日期的笔
     */
    protected Paint mWeekTextPaint = new Paint();

    protected int mWeekStart;

    private CalendarViewDelegate mDelegate;

    public WeekBar(Context context) {
        super(context);
    }

    public WeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 传递属性
     *
     * @param delegate delegate
     */
    void setup(CalendarViewDelegate delegate) {
        this.mDelegate = delegate;
        setTextSize(delegate.getWeekTextSize());
        setTextColor(delegate.getWeekTextColor());
        setBackgroundColor(delegate.getWeekBackground());
        setHeight();
        initPaint(getContext());
    }

    private void setHeight() {
        if (mDelegate != null) {
            mHeight = mDelegate.getWeekBarHeight();
        } else {
            mHeight = CalendarUtil.dipToPx(getContext(), 40);
        }
        Paint.FontMetrics metrics = mWeekTextPaint.getFontMetrics();
        mTextBaseLine = mHeight / 2 - metrics.descent + (metrics.bottom - metrics.top) / 2;
    }

    private void initPaint(Context context) {
        mWeekTextPaint.setAntiAlias(true);
        mWeekTextPaint.setTextAlign(Paint.Align.CENTER);
        mWeekTextPaint.setColor(0xFF111111);
        mWeekTextPaint.setFakeBoldText(true);
        mWeekTextPaint.setTextSize(CalendarUtil.dipToPx(context, TEXT_SIZE));
    }

    /**
     * 设置文本颜色，使用自定义布局需要重写这个方法，避免出问题
     * 如果这里报错了，请确定你自定义XML文件跟布局是不是使用merge，而不是LinearLayout
     *
     * @param color color
     */
    protected void setTextColor(int color) {
        mWeekTextPaint.setColor(color);
        invalidate();
    }

    /**
     * 设置文本大小
     *
     * @param size size
     */
    protected void setTextSize(int size) {
        mWeekTextPaint.setTextSize(CalendarUtil.dipToPx(getContext(), size));
        invalidate();
    }

    /**
     * 日期选择事件，这里提供这个回调，可以方便定制WeekBar需要
     *
     * @param calendar  calendar 选择的日期
     * @param weekStart 周起始
     * @param isClick   isClick 点击
     */
    protected void onDateSelected(Calendar calendar, int weekStart, boolean isClick) {

    }

    /**
     * 当周起始发生变化，使用自定义布局需要重写这个方法，避免出问题
     *
     * @param weekStart 周起始
     */
    protected void onWeekStartChange(int weekStart) {
        mWeekStart = weekStart;
    }

    /**
     * 通过View的位置和周起始获取星期的对应坐标
     *
     * @param calendar  calendar
     * @param weekStart weekStart
     * @return 通过View的位置和周起始获取星期的对应坐标
     */
    protected int getViewIndexByCalendar(Calendar calendar, int weekStart) {
        int week = calendar.getWeek() + 1;
        if (weekStart == CalendarViewDelegate.WEEK_START_WITH_SUN) {
            return week - 1;
        }
        if (weekStart == CalendarViewDelegate.WEEK_START_WITH_MON) {
            return week == CalendarViewDelegate.WEEK_START_WITH_SUN ? 6 : week - 2;
        }
        return week == CalendarViewDelegate.WEEK_START_WITH_SAT ? 0 : week;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mDelegate != null) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mDelegate.getWeekBarHeight(), MeasureSpec.EXACTLY);
        } else {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(CalendarUtil.dipToPx(getContext(), 40), MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mItemWidth = (getWidth() - 2 * mDelegate.getCalendarPadding()) / 7;
        String[] weeks = getContext().getResources().getStringArray(R.array.week_string_array);
        float baselineY = mTextBaseLine;
        for (int i = 0; i < weeks.length; i++) {
            int x = i * mItemWidth + mDelegate.getCalendarPadding();
            int cx = x + mItemWidth / 2;
            canvas.drawText(getWeekString(i, mWeekStart), cx, baselineY, mWeekTextPaint);
        }
    }

    /**
     * @param index     index
     * @param weekStart weekStart
     * @return 或者周文本
     */
    private String getWeekString(int index, int weekStart) {
        String[] weeks = getContext().getResources().getStringArray(R.array.week_string_array);
        if (weekStart == CalendarViewDelegate.WEEK_START_WITH_SUN) {
            return weeks[index];
        }
        if (weekStart == CalendarViewDelegate.WEEK_START_WITH_MON) {
            return weeks[index == 6 ? 0 : index + 1];
        }
        return weeks[index == 0 ? 6 : index - 1];
    }
}
