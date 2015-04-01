/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 * Copyright (C) 2015 Haruki Hasegawa <h6a.h4i.0@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.astuetz;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.astuetz.pagerslidingtabstrip.R;

import java.util.Locale;

public class PagerSlidingTabStrip extends HorizontalScrollView {

	public interface IconTabProvider {
		int getPageIconResId(int position);
	}

	public interface OnTabClickListener {
		boolean onClick(View v, int position);
	}

	public static final int INDICATOR_POSITION_TOP = 0;
	public static final int INDICATOR_POSITION_BOTTOM = 1;

	private OnClickListener mTabOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			handleTabOnClick(v);
		}
	};

	private LinearLayout.LayoutParams defaultTabLayoutParams;
	private LinearLayout.LayoutParams expandedTabLayoutParams;

	private final PageListener pageListener = new PageListener();
	private OnPageChangeListener delegatePageListener;
	private OnTabClickListener tabClickListener;

	private LinearLayout tabsContainer;
	private ViewPager pager;

	private int tabCount;

	private int currentPosition = 0;
	private float currentPositionOffset = 0f;

	private Paint rectPaint;
	private Paint dividerPaint;

	private int indicatorColor = 0xFF666666;
	private int underlineColor = 0x1A000000;
	private int overlineColor = 0x1A000000;
	private int dividerColor = 0x1A000000;

	private boolean shouldExpand = false;
	private boolean textAllCaps = true;

	private int scrollOffset = 52;
	private boolean scrollToCenter = false;
	private int indicatorHeight = 8;
	private int underlineHeight = 2;
	private int overlineHeight = 0;
	private int dividerPadding = 12;
	private int tabPadding = 24;
	private int dividerWidth = 1;

	private int tabTextSize = 12;
	private ColorStateList tabTextColor = null;
	private Typeface tabTypeface = null;
	private int tabTypefaceStyle = Typeface.BOLD;
	private int indicatorPosition = INDICATOR_POSITION_BOTTOM;

	private int lastScrollX = -1;
	private boolean isScrollingByDrag = false;

	private int tabBackgroundResId = R.drawable.background_tab;

	private Locale locale;
	private boolean layoutFinished;

	public PagerSlidingTabStrip(Context context) {
		this(context, null);
	}

	public PagerSlidingTabStrip(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PagerSlidingTabStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		setFillViewport(true);
		setWillNotDraw(false);

		tabsContainer = new LinearLayout(context);
		tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
		tabsContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		addView(tabsContainer);

		DisplayMetrics dm = getResources().getDisplayMetrics();

		scrollOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm);
		indicatorHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indicatorHeight, dm);
		underlineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, underlineHeight, dm);
		overlineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, overlineHeight, dm);
		dividerPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerPadding, dm);
		tabPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm);
		dividerWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm);
		tabTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, tabTextSize, dm);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagerSlidingTabStrip);

		// get system attrs
		tabTextSize = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_android_textSize, tabTextSize);
		tabTextColor = a.getColorStateList(R.styleable.PagerSlidingTabStrip_android_textColor);

		// get custom attrs
		indicatorColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsIndicatorColor, indicatorColor);
		underlineColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsUnderlineColor, underlineColor);
		overlineColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsOverlineColor, overlineColor);
		dividerColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsDividerColor, dividerColor);
		indicatorHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsIndicatorHeight, indicatorHeight);
		underlineHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsUnderlineHeight, underlineHeight);
		overlineHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsOverlineHeight, overlineHeight);
		dividerPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsDividerPadding, dividerPadding);
		tabPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsTabPaddingLeftRight, tabPadding);
		tabBackgroundResId = a.getResourceId(R.styleable.PagerSlidingTabStrip_pstsTabBackground, tabBackgroundResId);
		shouldExpand = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsShouldExpand, shouldExpand);
		scrollOffset = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsScrollOffset, scrollOffset);
		scrollToCenter = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsScrollToCenter, scrollToCenter);
		textAllCaps = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsTextAllCaps, textAllCaps);
		indicatorPosition = getIndicatorPositionFromTypedArray(a, indicatorPosition);

		a.recycle();

		rectPaint = new Paint();
		rectPaint.setAntiAlias(true);
		rectPaint.setStyle(Style.FILL);

		dividerPaint = new Paint();
		dividerPaint.setAntiAlias(true);
		dividerPaint.setStrokeWidth(dividerWidth);

		defaultTabLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		expandedTabLayoutParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);

		if (locale == null) {
			locale = getResources().getConfiguration().locale;
		}
	}

	public void setViewPager(ViewPager pager) {
		this.pager = pager;

		if (pager.getAdapter() == null) {
			throw new IllegalStateException("ViewPager does not have adapter instance.");
		}

		pager.setOnPageChangeListener(pageListener);

		notifyDataSetChanged();
	}

	public void setOnPageChangeListener(OnPageChangeListener listener) {
		this.delegatePageListener = listener;
	}

	public void setOnTabClickListener(OnTabClickListener listener) {
		this.tabClickListener = listener;
	}

	public void notifyDataSetChanged() {

		tabsContainer.removeAllViews();

		tabCount = pager.getAdapter().getCount();

		for (int i = 0; i < tabCount; i++) {

			if (pager.getAdapter() instanceof IconTabProvider) {
				addIconTab(i, ((IconTabProvider) pager.getAdapter()).getPageIconResId(i));
			} else {
				addTextTab(i, pager.getAdapter().getPageTitle(i).toString());
			}

		}

		updateTabStyles();

		getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				ViewTreeObserverCompat.removeOnGlobalLayoutListener(getViewTreeObserver(), this);

				layoutFinished = true;
				currentPosition = pager.getCurrentItem();
				scrollToChild(currentPosition, 0, false);
				updateSelection(currentPosition);
			}
		});

	}

	private void addTextTab(final int position, String title) {

		TabTextView tab = new TabTextView(getContext());
		tab.setText(title);
		tab.setGravity(Gravity.CENTER);

		addTab(position, tab);
	}

	private void addIconTab(final int position, int resId) {

		ImageButton tab = new ImageButton(getContext());
		tab.setImageResource(resId);

		addTab(position, tab);

	}

	private void addTab(final int position, View tab) {
		tab.setFocusable(true);
		tab.setOnClickListener(mTabOnClickListener);

		tab.setPadding(tabPadding, 0, tabPadding, 0);
		tabsContainer.addView(tab, position, shouldExpand ? expandedTabLayoutParams : defaultTabLayoutParams);
	}

	private void updateTabStyles() {

		for (int i = 0; i < tabCount; i++) {

			View v = tabsContainer.getChildAt(i);

			v.setBackgroundResource(tabBackgroundResId);

			if (v instanceof TabTextView) {

				TabTextView tab = (TabTextView) v;
				tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize);
				tab.setTypeface(tabTypeface, tabTypefaceStyle);
				if (tabTextColor != null) {
					tab.setTextColor(tabTextColor);
				}

				tab.setAllCaps(textAllCaps);
			}
		}
	}

	private void scrollToChild(int position, float positionOffset, boolean smoothly) {
		if (tabCount == 0) {
			return;
		}

		final View currentTab = tabsContainer.getChildAt(position);
		int newScrollX = currentTab.getLeft() + (int)(currentTab.getWidth() * positionOffset);

		if (scrollToCenter) {
			final int nextPosition = position + 1;
			final View nextTab = (nextPosition < tabCount) ? tabsContainer.getChildAt(nextPosition) : null;
			final int parentWidth = getWidth();
			final int width1 = currentTab.getWidth();
			final int width2 = (nextTab != null) ? nextTab.getWidth() : width1;
			final float interpolatedWidth = lerp((float) width1, (float) width2, positionOffset);

			newScrollX -= (int)((parentWidth - interpolatedWidth) * 0.5f);
		} else {
			newScrollX -= scrollOffset;
		}

		newScrollX = Math.max(0, newScrollX);

		if (newScrollX != lastScrollX) {
			lastScrollX = newScrollX;

			if (smoothly && layoutFinished) {
				smoothScrollTo(newScrollX, 0);
			} else {
				scrollTo(newScrollX, 0);
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (isInEditMode() || tabCount == 0) {
			return;
		}

		final int height = getHeight();

		// draw indicator line
		if (!isTransparent(indicatorColor) && indicatorHeight > 0) {
			float left;
			float right;
			float top = 0;

			if (!isScrollingByDrag) {
				// default: line below selected tab
				final int selectedPosition = pager.getCurrentItem();
				final View currentTab = tabsContainer.getChildAt(selectedPosition);
				left = currentTab.getLeft();
				right = currentTab.getRight();
			} else {
				// if there is an offset, start interpolating left and right coordinates between current and next tab
				final int nextPosition = currentPosition + 1;
				final View currentTab = tabsContainer.getChildAt(currentPosition);
				final View nextTab = (nextPosition < tabCount) ? tabsContainer.getChildAt(nextPosition) : null;
				final float currentTabLeft = currentTab.getLeft();
				final float currentTabRight = currentTab.getRight();
				final float nextTabLeft = (nextTab != null) ? nextTab.getLeft() : currentTabLeft;
				final float nextTabRight = (nextTab != null) ? nextTab.getRight() : currentTabRight;

				left = (int) lerp((float) currentTabLeft, (float) nextTabLeft, currentPositionOffset);
				right = (int) lerp((float) currentTabRight, (float) nextTabRight, currentPositionOffset);
			}

			if (indicatorPosition == INDICATOR_POSITION_BOTTOM) {
				top = height - indicatorHeight;
			} else if (indicatorPosition == INDICATOR_POSITION_TOP) {
				top = 0;
			}

			rectPaint.setColor(indicatorColor);
			canvas.drawRect(left, top, right, top + indicatorHeight, rectPaint);
		}

		// draw underline
		if (!isTransparent(underlineColor) && underlineHeight > 0) {
			rectPaint.setColor(underlineColor);
			canvas.drawRect(0, height - underlineHeight, tabsContainer.getWidth(), height, rectPaint);
		}

		// draw overline
		if (!isTransparent(overlineColor) && overlineHeight > 0) {
			rectPaint.setColor(overlineColor);
			canvas.drawRect(0, 0, tabsContainer.getWidth(), overlineHeight, rectPaint);
		}

		// draw divider
		if (!isTransparent(dividerColor)) {
			dividerPaint.setColor(dividerColor);
			for (int i = 0; i < tabCount - 1; i++) {
				View tab = tabsContainer.getChildAt(i);
				canvas.drawLine(tab.getRight(), dividerPadding, tab.getRight(), height - dividerPadding, dividerPaint);
			}
		}
	}

	private class PageListener implements OnPageChangeListener {

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			currentPosition = position;
			currentPositionOffset = positionOffset;

			if (isScrollingByDrag) {
				scrollToChild(position, positionOffset, false);
			}

			invalidate();

			if (delegatePageListener != null) {
				delegatePageListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
			}
		}

		@Override
		public void onPageScrollStateChanged(int state) {

			switch (state) {
				case ViewPager.SCROLL_STATE_IDLE:
					scrollToChild(pager.getCurrentItem(), 0, false);
					isScrollingByDrag = false;
					lastScrollX = -1;
					break;
				case ViewPager.SCROLL_STATE_DRAGGING:
					isScrollingByDrag = true;
					break;
				case ViewPager.SCROLL_STATE_SETTLING:
					break;
			}

			if (delegatePageListener != null) {
				delegatePageListener.onPageScrollStateChanged(state);
			}
		}

		@Override
		public void onPageSelected(int position) {
			if (!isScrollingByDrag) {
				scrollToChild(position, 0, true);
			}

			updateSelection(position);
			
			if (delegatePageListener != null) {
				delegatePageListener.onPageSelected(position);
			}
		}

	}
	
	public void updateSelection(int position) {
		for (int i = 0; i < tabCount; ++i) {
			View tv = tabsContainer.getChildAt(i);
			tv.setSelected(i == position);
		}
	}

	public void setIndicatorColor(int indicatorColor) {
		this.indicatorColor = indicatorColor;
		invalidate();
	}

	public void setIndicatorColorResource(int resId) {
		this.indicatorColor = getResources().getColor(resId);
		invalidate();
	}

	public int getIndicatorColor() {
		return this.indicatorColor;
	}

	public void setIndicatorHeight(int indicatorLineHeightPx) {
		this.indicatorHeight = indicatorLineHeightPx;
		invalidate();
	}

	public int getIndicatorHeight() {
		return indicatorHeight;
	}

	public void setUnderlineColor(int underlineColor) {
		this.underlineColor = underlineColor;
		invalidate();
	}

	public void setUnderlineColorResource(int resId) {
		this.underlineColor = getResources().getColor(resId);
		invalidate();
	}

	public int getUnderlineColor() {
		return underlineColor;
	}

	public void setOverineColor(int overlineColor) {
		this.overlineColor = overlineColor;
		invalidate();
	}

	public void setOverlineColorResource(int resId) {
		this.overlineColor = getResources().getColor(resId);
		invalidate();
	}

	public int getOverlineColor() {
		return overlineColor;
	}

	public void setDividerColor(int dividerColor) {
		this.dividerColor = dividerColor;
		invalidate();
	}

	public void setDividerColorResource(int resId) {
		this.dividerColor = getResources().getColor(resId);
		invalidate();
	}

	public int getDividerColor() {
		return dividerColor;
	}

	public void setUnderlineHeight(int underlineHeightPx) {
		this.underlineHeight = underlineHeightPx;
		invalidate();
	}

	public int getUnderlineHeight() {
		return underlineHeight;
	}


	public void setOverlineHeight(int overlineHeightPx) {
		this.overlineHeight = overlineHeightPx;
		invalidate();
	}

	public int getOverlineHeight() {
		return overlineHeight;
	}

	public void setDividerPadding(int dividerPaddingPx) {
		this.dividerPadding = dividerPaddingPx;
		invalidate();
	}

	public int getDividerPadding() {
		return dividerPadding;
	}

	public void setScrollOffset(int scrollOffsetPx) {
		this.scrollOffset = scrollOffsetPx;
		invalidate();
	}

	public int getScrollOffset() {
		return scrollOffset;
	}

	public void setShouldExpand(boolean shouldExpand) {
		this.shouldExpand = shouldExpand;
		requestLayout();
	}

	public boolean getShouldExpand() {
		return shouldExpand;
	}

	public boolean isTextAllCaps() {
		return textAllCaps;
	}

	public void setAllCaps(boolean textAllCaps) {
		this.textAllCaps = textAllCaps;
		updateTabStyles();
	}

	public void setTextSize(int textSizePx) {
		this.tabTextSize = textSizePx;
		updateTabStyles();
	}

	public int getTextSize() {
		return tabTextSize;
	}

	public void setTextColor(ColorStateList textColor) {
		this.tabTextColor = textColor;
		updateTabStyles();
	}

	public void setTextColorResource(int resId) {
		this.tabTextColor = getResources().getColorStateList(resId);
		updateTabStyles();
	}

	public ColorStateList getTextColor() {
		return tabTextColor;
	}

	public void setTypeface(Typeface typeface, int style) {
		this.tabTypeface = typeface;
		this.tabTypefaceStyle = style;
		updateTabStyles();
	}

	public void setTabBackground(int resId) {
		this.tabBackgroundResId = resId;
	}

	public int getTabBackground() {
		return tabBackgroundResId;
	}

	public void setTabPaddingLeftRight(int paddingPx) {
		this.tabPadding = paddingPx;
		updateTabStyles();
	}

	public int getTabPaddingLeftRight() {
		return tabPadding;
	}

	public void setIndicatorPosition(int indicatorPosition) {
		if (!isValidIndicatorPosition(indicatorPosition)) {
			throw new IllegalArgumentException(
				"Invalid indicator position specified: " + indicatorPosition);
		}
		this.indicatorPosition = indicatorPosition;
	}

	public int getIndicatorPosition() {
		return indicatorPosition;
	}

	private static boolean isTransparent(int color) {
		return (color & 0xFF000000) == 0;
	}

	private static boolean isValidIndicatorPosition(int indicatorPosition) {
		return (indicatorPosition == INDICATOR_POSITION_TOP ||
			indicatorPosition == INDICATOR_POSITION_BOTTOM);
	}

	private static int getIndicatorPositionFromTypedArray(TypedArray a, int defValue) {
		final int value = a.getInteger(R.styleable.PagerSlidingTabStrip_pstsIndicatorPosition, Integer.MIN_VALUE);
		if (isValidIndicatorPosition(value)) {
			return value;
		} else {
			return defValue;
		}
	}

	private static float lerp(float a, float b, float proportion) {
		return (a * (1.0f - proportion)) + (b * proportion);
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		currentPosition = savedState.currentPosition;
		requestLayout();
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.currentPosition = currentPosition;
		return savedState;
	}

	private void handleTabOnClick(View v) {
		int n = tabsContainer.getChildCount();

		for (int i = 0; i < n; i++) {
			if (v == tabsContainer.getChildAt(i)) {
				if (tabClickListener != null) {
					if (tabClickListener.onClick(v, i)) {
						// handled
						break;
					}
				}

				// set current page
				pager.setCurrentItem(i);
				break;
			}
		}
	}

	static class SavedState extends BaseSavedState {
		int currentPosition;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			currentPosition = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(currentPosition);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

}
