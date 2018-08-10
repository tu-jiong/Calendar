/*
 * Copyright (C) 2016 huanghaibin_dev <huanghaibin_dev@163.com>
 * WebSite https://github.com/MiracleTimes-Dev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jm.library;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

/**
 * 日历布局
 * 各个类使用包权限，避免不必要的public
 */
@SuppressWarnings("unused")
public class CalendarView extends FrameLayout {

    /**
     * 使用google官方推荐的方式抽取自定义属性
     */
    private final CalendarViewDelegate mDelegate;

    /**
     * 自定义自适应高度的ViewPager
     */
    private MonthViewPager mMonthPager;

    /**
     * 日历周视图
     */
    private WeekViewPager mWeekPager;

    /**
     * 星期栏
     */
    private WeekBar mWeekBar;

    private boolean isAnimating;

    private static final int ACTIVE_POINTER = 1;
    private static final int INVALID_POINTER = -1;

    private int mActivePointerId;
    private float downY;
    private float mLastY;
    private int mTouchSlop;
    private int mMaximumVelocity;

    private VelocityTracker mVelocityTracker;

    public CalendarView(@NonNull Context context) {
        this(context, null);
    }

    public CalendarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDelegate = new CalendarViewDelegate(context, attrs);
        init(context);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mVelocityTracker = VelocityTracker.obtain();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    /**
     * 初始化
     *
     * @param context context
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_calendar_view, this, true);
        FrameLayout frameContent = findViewById(R.id.frameContent);
        this.mWeekPager = findViewById(R.id.vp_week);
        this.mWeekPager.setup(mDelegate);

        if (TextUtils.isEmpty(mDelegate.getWeekBarClass())) {
            this.mWeekBar = new WeekBar(getContext());
        } else {
            try {
                Class<?> cls = Class.forName(mDelegate.getWeekBarClass());
                Constructor constructor = cls.getConstructor(Context.class);
                mWeekBar = (WeekBar) constructor.newInstance(getContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        frameContent.addView(mWeekBar, 2);
        mWeekBar.setup(mDelegate);
        mWeekBar.onWeekStartChange(mDelegate.getWeekStart());

        this.mMonthPager = findViewById(R.id.vp_month);
        this.mMonthPager.mWeekPager = mWeekPager;
        this.mMonthPager.mWeekBar = mWeekBar;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) this.mMonthPager.getLayoutParams();
        params.setMargins(0, mDelegate.getWeekBarHeight(), 0, 0);
        mWeekPager.setLayoutParams(params);

        mDelegate.mInnerListener = new OnInnerDateSelectedListener() {
            /**
             * 月视图选择事件
             * @param calendar calendar
             * @param isClick  是否是点击
             */
            @Override
            public void onMonthDateSelected(Calendar calendar, boolean isClick) {
                if (calendar.getYear() == mDelegate.getCurrentDay().getYear() &&
                        calendar.getMonth() == mDelegate.getCurrentDay().getMonth()
                        && mMonthPager.getCurrentItem() != mDelegate.mCurrentMonthViewItem) {
                    return;
                }
                mDelegate.mIndexCalendar = calendar;
                if (mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_DEFAULT || isClick) {
                    mDelegate.mSelectedCalendar = calendar;
                }
                mWeekPager.updateSelected(mDelegate.mIndexCalendar, false);
                mMonthPager.updateSelected();
                if (mWeekBar != null &&
                        (mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_DEFAULT || isClick)) {
                    mWeekBar.onDateSelected(calendar, mDelegate.getWeekStart(), isClick);
                }
            }

            /**
             * 周视图选择事件
             * @param calendar calendar
             * @param isClick 是否是点击
             */
            @Override
            public void onWeekDateSelected(Calendar calendar, boolean isClick) {
                mDelegate.mIndexCalendar = calendar;
                if (mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_DEFAULT || isClick
                        || mDelegate.mIndexCalendar.equals(mDelegate.mSelectedCalendar)) {
                    mDelegate.mSelectedCalendar = calendar;
                }
                int y = calendar.getYear() - mDelegate.getMinYear();
                int position = 12 * y + mDelegate.mIndexCalendar.getMonth() - mDelegate.getMinYearMonth();
                mWeekPager.updateSingleSelect();
                mMonthPager.setCurrentItem(position, false);
                mMonthPager.updateSelected();
                if (mWeekBar != null &&
                        (mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_DEFAULT
                                || isClick
                                || mDelegate.mIndexCalendar.equals(mDelegate.mSelectedCalendar))) {
                    mWeekBar.onDateSelected(calendar, mDelegate.getWeekStart(), isClick);
                }
            }
        };

        mDelegate.mSelectedCalendar = mDelegate.createCurrentDate();
        mWeekBar.onDateSelected(mDelegate.mSelectedCalendar, mDelegate.getWeekStart(), false);

        mMonthPager.setup(mDelegate);
        mMonthPager.setCurrentItem(mDelegate.mCurrentMonthViewItem);
        mWeekPager.updateSelected(mDelegate.mSelectedCalendar, false);
    }

    /**
     * 设置日期范围
     *
     * @param minYear      最小年份
     * @param minYearMonth 最小年份对应月份
     * @param maxYear      最大月份
     * @param maxYearMonth 最大月份对应月份
     */
    public void setRange(int minYear, int minYearMonth,
                         int maxYear, int maxYearMonth) {
        mDelegate.setRange(minYear, minYearMonth,
                maxYear, maxYearMonth);
        mWeekPager.notifyDataSetChanged();
        mMonthPager.notifyDataSetChanged();
        if (CalendarUtil.isCalendarInRange(mDelegate.mSelectedCalendar, mDelegate)) {
            scrollToCalendar(mDelegate.mSelectedCalendar.getYear(),
                    mDelegate.mSelectedCalendar.getMonth(),
                    mDelegate.mSelectedCalendar.getDay());
        } else {
            scrollToCurrent();
        }

    }

    /**
     * 获取当天
     *
     * @return 返回今天
     */
    public int getCurDay() {
        return mDelegate.getCurrentDay().getDay();
    }

    /**
     * 获取本月
     *
     * @return 返回本月
     */
    public int getCurMonth() {
        return mDelegate.getCurrentDay().getMonth();
    }

    /**
     * 获取本年
     *
     * @return 返回本年
     */
    public int getCurYear() {
        return mDelegate.getCurrentDay().getYear();
    }


    /**
     * 打开日历年月份快速选择
     *
     * @param year 年
     */
    @SuppressWarnings("deprecation")
    public void showYearSelectLayout(final int year) {
        showSelectLayout(year);
    }

    /**
     * 打开日历年月份快速选择
     * 请使用 showYearSelectLayout(final int year) 代替，这个没什么，越来越规范
     *
     * @param year 年
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public void showSelectLayout(final int year) {
        mWeekPager.setVisibility(GONE);
        mDelegate.isShowYearSelectedLayout = true;

        mWeekBar.animate()
                .translationY(-mWeekBar.getHeight())
                .setInterpolator(new LinearInterpolator())
                .setDuration(260)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mWeekBar.setVisibility(GONE);
                    }
                });

        mMonthPager.animate()
                .scaleX(0)
                .scaleY(0)
                .setDuration(260)
                .setInterpolator(new LinearInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                });
    }

    /**
     * 关闭年月视图选择布局
     */
    public void closeYearSelectLayout() {
        int position = 12 * (mDelegate.mSelectedCalendar.getYear() - mDelegate.getMinYear()) +
                mDelegate.mSelectedCalendar.getMonth() - mDelegate.getMinYearMonth();
        closeSelectLayout(position);
        mDelegate.isShowYearSelectedLayout = false;
    }

    /**
     * 关闭日历布局，同时会滚动到指定的位置
     *
     * @param position 某一年
     */
    private void closeSelectLayout(final int position) {
        mWeekBar.setVisibility(VISIBLE);
        if (position == mMonthPager.getCurrentItem()) {
            if (mDelegate.mDateSelectedListener != null) {
                mDelegate.mDateSelectedListener.onDateSelected(mDelegate.mSelectedCalendar, false);
            }
        } else {
            mMonthPager.setCurrentItem(position, false);
        }
        mWeekBar.animate()
                .translationY(0)
                .setInterpolator(new LinearInterpolator())
                .setDuration(280)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mWeekBar.setVisibility(VISIBLE);
                    }
                });
        mMonthPager.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(180)
                .setInterpolator(new LinearInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mMonthPager.setVisibility(VISIBLE);
                        mMonthPager.clearAnimation();
                    }
                });
    }

    /**
     * 滚动到当前
     */
    public void scrollToCurrent() {
        scrollToCurrent(false);
    }

    /**
     * 滚动到当前
     *
     * @param smoothScroll smoothScroll
     */
    public void scrollToCurrent(boolean smoothScroll) {
        if (!CalendarUtil.isCalendarInRange(mDelegate.getCurrentDay(), mDelegate)) {
            return;
        }
        mDelegate.mSelectedCalendar = mDelegate.createCurrentDate();
        mDelegate.mIndexCalendar = mDelegate.mSelectedCalendar;
        mWeekBar.onDateSelected(mDelegate.mSelectedCalendar, mDelegate.getWeekStart(), false);
        if (mMonthPager.getVisibility() == VISIBLE) {
            mMonthPager.scrollToCurrent(smoothScroll);
            mWeekPager.updateSelected(mDelegate.mIndexCalendar, false);
        } else {
            mWeekPager.scrollToCurrent(smoothScroll);
        }
    }


    /**
     * 滚动到下一个月
     */
    public void scrollToNext() {
        scrollToNext(false);
    }

    /**
     * 滚动到下一个月
     *
     * @param smoothScroll smoothScroll
     */
    public void scrollToNext(boolean smoothScroll) {
        if (mWeekPager.getVisibility() == VISIBLE) {
            mWeekPager.setCurrentItem(mWeekPager.getCurrentItem() + 1, smoothScroll);
        } else {
            mMonthPager.setCurrentItem(mMonthPager.getCurrentItem() + 1, smoothScroll);
        }

    }

    /**
     * 滚动到上一个月
     */
    public void scrollToPre() {
        scrollToPre(false);
    }

    /**
     * 滚动到上一个月
     *
     * @param smoothScroll smoothScroll
     */
    public void scrollToPre(boolean smoothScroll) {
        if (mWeekPager.getVisibility() == VISIBLE) {
            mWeekPager.setCurrentItem(mWeekPager.getCurrentItem() - 1, smoothScroll);
        } else {
            mMonthPager.setCurrentItem(mMonthPager.getCurrentItem() - 1, smoothScroll);
        }
    }

    /**
     * 滚动到选择的日历
     */
    public void scrollToSelectCalendar() {
        scrollToCalendar(mDelegate.mSelectedCalendar.getYear(),
                mDelegate.mSelectedCalendar.getMonth(),
                mDelegate.mSelectedCalendar.getDay(),
                false);
    }

    /**
     * 滚动到指定日期
     *
     * @param year  year
     * @param month month
     * @param day   day
     */
    public void scrollToCalendar(int year, int month, int day) {
        scrollToCalendar(year, month, day, false);
    }

    /**
     * 滚动到指定日期
     *
     * @param year         year
     * @param month        month
     * @param day          day
     * @param smoothScroll smoothScroll
     */
    @SuppressWarnings("all")
    public void scrollToCalendar(int year, int month, int day, boolean smoothScroll) {
        if (mWeekPager.getVisibility() == VISIBLE) {
            mWeekPager.scrollToCalendar(year, month, day, smoothScroll);
        } else {
            mMonthPager.scrollToCalendar(year, month, day, smoothScroll);
        }
    }

    /**
     * 设置月视图
     *
     * @param cls MonthView.class
     */
    public void setMonthView(Class<?> cls) {
        if (cls == null) {
            return;
        }
        String monthViewClassPath = cls.getName();
        if (monthViewClassPath.equals(mDelegate.getMonthViewClass()) &&
                !TextUtils.isEmpty(mDelegate.getMonthViewClass())) {
            return;
        }
        mDelegate.setMonthViewClass(monthViewClassPath);
        mMonthPager.updateMonthViewClass();
    }

    /**
     * 设置周视图
     *
     * @param cls WeekView.class
     */
    public void setWeekView(Class<?> cls) {
        if (cls == null) {
            return;
        }
        String weekViewClassPath = cls.getName();
        if (weekViewClassPath.equals(mDelegate.getWeekViewClass()) &&
                !TextUtils.isEmpty(mDelegate.getWeekViewClass())) {
            return;
        }
        mDelegate.setWeekViewClass(weekViewClassPath);
        mWeekPager.updateWeekViewClass();
    }

    /**
     * 设置周栏视图
     *
     * @param cls WeekBar.class
     */
    public void setWeekBar(Class<?> cls) {
        if (cls == null) {
            return;
        }
        String weekBarClassPath = cls.getName();
        if (weekBarClassPath.equals(mDelegate.getWeekBarClass()) &&
                !TextUtils.isEmpty(mDelegate.getWeekBarClass())) {
            return;
        }
        mDelegate.setWeekBarClass(weekBarClassPath);
        FrameLayout frameContent = findViewById(R.id.frameContent);
        frameContent.removeView(mWeekBar);

        try {
            Constructor constructor = cls.getConstructor(Context.class);
            mWeekBar = (WeekBar) constructor.newInstance(getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        frameContent.addView(mWeekBar, 2);
        mWeekBar.setup(mDelegate);
        mWeekBar.onWeekStartChange(mDelegate.getWeekStart());
        this.mMonthPager.mWeekBar = mWeekBar;
        mWeekBar.onDateSelected(mDelegate.mSelectedCalendar, mDelegate.getWeekStart(), false);
    }

    /**
     * 年份改变事件
     *
     * @param listener listener
     */
    public void setOnYearChangeListener(OnYearChangeListener listener) {
        this.mDelegate.mYearChangeListener = listener;
    }

    /**
     * 月份改变事件
     *
     * @param listener listener
     */
    public void setOnMonthChangeListener(OnMonthChangeListener listener) {
        this.mDelegate.mMonthChangeListener = listener;
        if (mDelegate.mMonthChangeListener != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    mDelegate.mMonthChangeListener.onMonthChange(mDelegate.mSelectedCalendar.getYear(),
                            mDelegate.mSelectedCalendar.getMonth());
                }
            });
        }
    }

    /**
     * 设置日期选中事件
     *
     * @param listener 日期选中事件
     */
    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.mDelegate.mDateSelectedListener = listener;
        if (mDelegate.mDateSelectedListener != null) {
            if (!CalendarUtil.isCalendarInRange(mDelegate.mSelectedCalendar, mDelegate)) {
                return;
            }
            post(new Runnable() {
                @Override
                public void run() {
                    mDelegate.mDateSelectedListener.onDateSelected(mDelegate.mSelectedCalendar, false);
                }
            });
        }
    }

    /**
     * 日期长按事件
     *
     * @param listener listener
     */
    public void setOnDateLongClickListener(OnDateLongClickListener listener) {
        this.mDelegate.mDateLongClickListener = listener;
    }


    /**
     * 日期长按事件
     *
     * @param preventLongPressedSelect 防止长按选择日期
     * @param listener                 listener
     */
    public void setOnDateLongClickListener(OnDateLongClickListener listener, boolean preventLongPressedSelect) {
        this.mDelegate.mDateLongClickListener = listener;
        this.mDelegate.setPreventLongPressedSelected(preventLongPressedSelect);
    }


    /**
     * 视图改变事件
     *
     * @param listener listener
     */
    public void setOnViewChangeListener(OnViewChangeListener listener) {
        this.mDelegate.mViewChangeListener = listener;
    }

    /**
     * 初始化时初始化日历卡默认选择位置
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        int diff = CalendarUtil.getMonthViewStartDiff(mDelegate.mSelectedCalendar, mDelegate.getWeekStart());
        int size = diff + mDelegate.mSelectedCalendar.getDay() - 1;
        mDelegate.setPositionInMonth(size);
    }


    /**
     * 标记哪些日期有事件
     * 推荐使用 public void setSchemeDate(Map<String, Calendar> mSchemeDates)
     *
     * @param mSchemeDate mSchemeDate 通过自己的需求转换即可
     */
    @Deprecated
    public void setSchemeDate(List<Calendar> mSchemeDate) {
        this.mDelegate.mSchemeDate = mSchemeDate;
        this.mDelegate.mSchemeDatesMap = null;
        this.mDelegate.clearSelectedScheme();
        this.mDelegate.setSchemeType(CalendarViewDelegate.SCHEME_TYPE_LIST);
        this.mMonthPager.updateScheme();
        this.mWeekPager.updateScheme();
    }

    /**
     * 标记哪些日期有事件
     * 如果标记的日期数量很大，mSchemeDatesMap.size()>10000?,请使用这个
     * key=Calendar.toString();
     * 优势？Android的用户相应时间一旦大于16ms,UI就会卡顿，Map在数据增长量巨大时，查找性能上不会损耗，
     * List的性能就会差很大，List.contains()会遍历整个数组，性能太差
     *
     * @param mSchemeDates mSchemeDatesMap 通过自己的需求转换即可
     */
    public void setSchemeDate(Map<String, Calendar> mSchemeDates) {
        this.mDelegate.mSchemeDatesMap = mSchemeDates;
        this.mDelegate.mSchemeDate = null;
        this.mDelegate.clearSelectedScheme();
        this.mDelegate.setSchemeType(CalendarViewDelegate.SCHEME_TYPE_MAP);
        this.mMonthPager.updateScheme();
        this.mWeekPager.updateScheme();
    }

    /**
     * 清空日期标记
     */
    public void clearSchemeDate() {
        this.mDelegate.mSchemeDatesMap = null;
        this.mDelegate.mSchemeDate = null;
        this.mDelegate.clearSelectedScheme();
        mMonthPager.updateScheme();
        mWeekPager.updateScheme();
    }


    /**
     * 移除某天的标记
     * 这个API是安全的，无效try cache
     *
     * @param calendar calendar
     */
    public void removeSchemeDate(Calendar calendar) {
        if (calendar == null) {
            return;
        }
        if (mDelegate.getSchemeType() == CalendarViewDelegate.SCHEME_TYPE_LIST) {
            if (mDelegate.mSchemeDate == null || mDelegate.mSchemeDate.size() == 0) {
                return;
            }
            if (mDelegate.mSchemeDate.contains(calendar)) {
                mDelegate.mSchemeDate.remove(calendar);
            }
        } else {
            if (mDelegate.mSchemeDatesMap == null || mDelegate.mSchemeDatesMap.size() == 0) {
                return;
            }
            if (mDelegate.mSchemeDatesMap.containsKey(calendar.toString())) {
                mDelegate.mSchemeDatesMap.remove(calendar.toString());
            }
        }
        if (mDelegate.mSelectedCalendar.equals(calendar)) {
            mDelegate.clearSelectedScheme();
        }

        mMonthPager.updateScheme();
        mWeekPager.updateScheme();
    }

    /**
     * 设置背景色
     *
     * @param yearViewBackground 年份卡片的背景色
     * @param weekBackground     星期栏背景色
     * @param lineBg             线的颜色
     */
    public void setBackground(int yearViewBackground, int weekBackground, int lineBg) {
        mWeekBar.setBackgroundColor(weekBackground);
    }


    /**
     * 设置文本颜色
     *
     * @param currentDayTextColor      今天字体颜色
     * @param curMonthTextColor        当前月份字体颜色
     * @param otherMonthColor          其它月份字体颜色
     * @param curMonthLunarTextColor   当前月份农历字体颜色
     * @param otherMonthLunarTextColor 其它农历字体颜色
     */
    public void setTextColor(
            int currentDayTextColor,
            int curMonthTextColor,
            int otherMonthColor,
            int curMonthLunarTextColor,
            int otherMonthLunarTextColor) {
        mDelegate.setTextColor(currentDayTextColor, curMonthTextColor, otherMonthColor, curMonthLunarTextColor, otherMonthLunarTextColor);
    }

    /**
     * 设置选择的效果
     *
     * @param selectedThemeColor     选中的标记颜色
     * @param selectedTextColor      选中的字体颜色
     * @param selectedLunarTextColor 选中的农历字体颜色
     */
    public void setSelectedColor(int selectedThemeColor, int selectedTextColor, int selectedLunarTextColor) {
        mDelegate.setSelectColor(selectedThemeColor, selectedTextColor, selectedLunarTextColor);
    }

    /**
     * 定制颜色
     *
     * @param selectedThemeColor 选中的标记颜色
     * @param schemeColor        标记背景色
     */
    public void setThemeColor(int selectedThemeColor, int schemeColor) {
        mDelegate.setThemeColor(selectedThemeColor, schemeColor);
    }

    /**
     * 设置标记的色
     *
     * @param schemeLunarTextColor 标记农历颜色
     * @param schemeColor          标记背景色
     * @param schemeTextColor      标记字体颜色
     */
    public void setSchemeColor(int schemeColor, int schemeTextColor, int schemeLunarTextColor) {
        mDelegate.setSchemeColor(schemeColor, schemeTextColor, schemeLunarTextColor);
    }

    /**
     * 设置年视图的颜色
     *
     * @param yearViewMonthTextColor 年视图月份颜色
     * @param yearViewDayTextColor   年视图天的颜色
     * @param yarViewSchemeTextColor 年视图标记颜色
     */
    public void setYearViewTextColor(int yearViewMonthTextColor, int yearViewDayTextColor, int yarViewSchemeTextColor) {
        mDelegate.setYearViewTextColor(yearViewMonthTextColor, yearViewDayTextColor, yarViewSchemeTextColor);
    }

    /**
     * 设置星期栏的背景和字体颜色
     *
     * @param weekBackground 背景色
     * @param weekTextColor  字体颜色
     */
    public void setWeeColor(int weekBackground, int weekTextColor) {
        mWeekBar.setBackgroundColor(weekBackground);
        mWeekBar.setTextColor(weekTextColor);
    }

    /**
     * 默认选择模式
     */
    public void setSelectDefaultMode() {
        if (mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_DEFAULT) {
            return;
        }
        mDelegate.mSelectedCalendar = mDelegate.mIndexCalendar;
        mDelegate.setSelectMode(CalendarViewDelegate.SELECT_MODE_DEFAULT);
        mWeekBar.onDateSelected(mDelegate.mSelectedCalendar, mDelegate.getWeekStart(), false);
        mMonthPager.updateDefaultSelect();
        mWeekPager.updateDefaultSelect();

    }

    /**
     * 单选模式
     */
    public void setSelectSingleMode() {
        if (mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_SINGLE) {
            return;
        }
        mDelegate.setSelectMode(CalendarViewDelegate.SELECT_MODE_SINGLE);
        mWeekPager.updateSelected();
        mMonthPager.updateSelected();
    }

    /**
     * 设置星期日周起始
     */
    public void setWeekStarWithSun() {
        setWeekStart(CalendarViewDelegate.WEEK_START_WITH_SUN);
    }

    /**
     * 设置星期一周起始
     */
    public void setWeekStarWithMon() {
        setWeekStart(CalendarViewDelegate.WEEK_START_WITH_MON);
    }

    /**
     * 设置星期六周起始
     */
    public void setWeekStarWithSat() {
        setWeekStart(CalendarViewDelegate.WEEK_START_WITH_SAT);
    }

    /**
     * 设置周起始
     * CalendarViewDelegate.WEEK_START_WITH_SUN
     * CalendarViewDelegate.WEEK_START_WITH_MON
     * CalendarViewDelegate.WEEK_START_WITH_SAT
     *
     * @param weekStart 周起始
     */
    private void setWeekStart(int weekStart) {
        if (weekStart != CalendarViewDelegate.WEEK_START_WITH_SUN &&
                weekStart != CalendarViewDelegate.WEEK_START_WITH_MON &&
                weekStart != CalendarViewDelegate.WEEK_START_WITH_SAT)
            return;
        if (weekStart == mDelegate.getWeekStart())
            return;
        mDelegate.setWeekStart(weekStart);
        mWeekBar.onWeekStartChange(weekStart);
        mWeekBar.onDateSelected(mDelegate.mSelectedCalendar, weekStart, false);
        mWeekPager.updateWeekStart();
        mMonthPager.updateWeekStart();
    }

    /**
     * 是否是单选模式
     *
     * @return isSingleSelectMode
     */
    public boolean isSingleSelectMode() {
        return mDelegate.getSelectMode() == CalendarViewDelegate.SELECT_MODE_SINGLE;
    }

    /**
     * 设置显示模式为全部
     */
    public void setAllMode() {
        setShowMode(CalendarViewDelegate.MODE_ALL_MONTH);
    }

    /**
     * 设置显示模式为仅当前月份
     */
    public void setOnlyCurrentMode() {
        setShowMode(CalendarViewDelegate.MODE_ONLY_CURRENT_MONTH);
    }

    /**
     * 设置显示模式为填充
     */
    public void setFixMode() {
        setShowMode(CalendarViewDelegate.MODE_FIT_MONTH);
    }

    /**
     * 设置显示模式
     * CalendarViewDelegate.MODE_ALL_MONTH
     * CalendarViewDelegate.MODE_ONLY_CURRENT_MONTH
     * CalendarViewDelegate.MODE_FIT_MONTH
     *
     * @param mode 月视图显示模式
     */
    private void setShowMode(int mode) {
        if (mode != CalendarViewDelegate.MODE_ALL_MONTH &&
                mode != CalendarViewDelegate.MODE_ONLY_CURRENT_MONTH &&
                mode != CalendarViewDelegate.MODE_FIT_MONTH)
            return;
        if (mDelegate.getMonthViewShowMode() == mode)
            return;
        mDelegate.setMonthViewShowMode(mode);
        mWeekPager.updateShowMode();
        mMonthPager.updateShowMode();
        mWeekPager.notifyDataSetChanged();
    }

    /**
     * 更新界面，
     * 重新设置颜色等都需要调用该方法
     */
    public void update() {
        mWeekBar.onWeekStartChange(mDelegate.getWeekStart());
        mMonthPager.updateScheme();
        mWeekPager.updateScheme();
    }

    /**
     * 更新周视图
     */
    public void updateWeekBar() {
        mWeekBar.onWeekStartChange(mDelegate.getWeekStart());
    }


    /**
     * 更新当前日期
     */
    public void updateCurrentDate() {
        mDelegate.updateCurrentDay();
        mMonthPager.updateCurrentDate();
        mWeekPager.updateCurrentDate();
    }

    /**
     * 获取选择的日期
     *
     * @return 获取选择的日期
     */
    public Calendar getSelectedCalendar() {
        return mDelegate.mSelectedCalendar;
    }

    public int getItemHeight() {
        return mDelegate == null ? 0 : mDelegate.getCalendarItemHeight();
    }

    public int getWeekBarHeight() {
        return mWeekBar == null ? 0 : mWeekBar.getHeight();
    }

    public void showWeek() {
        onShowWeekView();
        PagerAdapter adapter = mWeekPager.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        mWeekPager.setVisibility(VISIBLE);
    }

    public void hideWeek() {
        onShowMonthView();
        mWeekPager.setVisibility(GONE);
    }

    /**
     * 周视图显示事件
     */
    private void onShowWeekView() {
        if (mWeekPager.getVisibility() == VISIBLE) {
            return;
        }
        if (mDelegate.mViewChangeListener != null) {
            mDelegate.mViewChangeListener.onViewChange(false);
        }
    }

    /**
     * 周视图显示事件
     */
    private void onShowMonthView() {
        if (mMonthPager.getVisibility() == VISIBLE) {
            return;
        }
        if (mDelegate.mViewChangeListener != null) {
            mDelegate.mViewChangeListener.onViewChange(true);
        }
    }

    public MonthViewPager getMonthPager() {
        return mMonthPager;
    }

    public int getPositionInMonth() {
        return mDelegate.getPositionInMonth();
    }

    /**
     * 年份改变事件，快速年份切换
     */
    public interface OnYearChangeListener {
        void onYearChange(int year);
    }

    /**
     * 月份切换事件
     */
    public interface OnMonthChangeListener {
        void onMonthChange(int year, int month);
    }

    /**
     * 内部日期选择，不暴露外部使用
     * 主要是用于更新日历位置
     */
    interface OnInnerDateSelectedListener {
        /**
         * 月视图点击
         *
         * @param calendar calendar
         * @param isClick  是否是点击
         */
        void onMonthDateSelected(Calendar calendar, boolean isClick);

        /**
         * 周视图点击
         *
         * @param calendar calendar
         * @param isClick  是否是点击
         */
        void onWeekDateSelected(Calendar calendar, boolean isClick);
    }

    /**
     * 外部日期选择事件
     */
    public interface OnDateSelectedListener {
        /**
         * 日期选择事件
         *
         * @param calendar calendar
         * @param isClick  isClick
         */
        void onDateSelected(Calendar calendar, boolean isClick);
    }


    /**
     * 外部日期长按事件
     */
    public interface OnDateLongClickListener {
        /**
         * 日期长按事件
         *
         * @param calendar calendar
         */
        void onDateLongClick(Calendar calendar);
    }

    /**
     * 视图改变事件
     */
    public interface OnViewChangeListener {
        /**
         * 视图改变事件
         *
         * @param isMonthView isMonthView是否是月视图
         */
        void onViewChange(boolean isMonthView);
    }

}
