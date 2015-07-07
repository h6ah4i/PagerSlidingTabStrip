package com.astuetz;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TabTextView extends View {
    @SuppressWarnings("unused")
    private static final String TAG = "TabTextView";

    private static final Pattern BADGE_TEXT_PATTERN = Pattern.compile("^(.*)\\s*<(.*)>\\s*$"); // ex.) "Title <123>"
    private static final float BADGE_TEXT_FONT_SCALE = 0.85f;
    private static final float BADGE_LEFT_SPACE_DP = 4.0f;
    private static final float BADGE_TEXT_VERTICAL_PADDING_DP = 2.0f;

    private String mText = "";
    private String mRenderText = "";
    private String mBadgeText;
    private TextPaint mTextPaint;
    private TextPaint mBadgeTextPaint;
    private int mTextColor;
    private int mCurrentTextColor;
    private ColorStateList mTextColorStateList;
    private int mGravity;
    private Rect mClipBounds;
    private Rect mGravityRect;
    private Rect mMeasuredBounds;
    private int mMeasuredTextLineHeight = -1;
    private int mMeasuredTextWidth = -1;
    private int mMeasuredBadgeLineHeight = -1;
    private int mMeasuredBadgeTextWidth = -1;
    private Paint.FontMetricsInt mTextFontMetrics;
    private Paint.FontMetricsInt mBadgeFontMetrics;
    private int mMinWidth = 0;
    private int mMinHeight = 0;
    private boolean mAllCaps = false;
    private RectF mTempRectF = new RectF();
    private int mBadgeLeftSpace;
    private int mBadgeVerticalPadding;
    private int mBadgeTextColor = Color.WHITE;

    public TabTextView(Context context) {
        super(context);
        init();
    }

    void init() {
        final Resources res = getResources();
        float density = res.getDisplayMetrics().density;

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.density = density;
        mTextPaint.setTextSize(15.0f);
        mBadgeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mBadgeTextPaint.density = density;
        mBadgeTextPaint.setTextSize(15.0f * BADGE_TEXT_FONT_SCALE);
        mTextColor = mCurrentTextColor = Color.WHITE;
        mTextColorStateList = null;
        mGravity = Gravity.TOP | Gravity.LEFT;
        mClipBounds = new Rect();
        mGravityRect = new Rect();
        mMeasuredBounds = new Rect();
        mBadgeLeftSpace = (int) (BADGE_LEFT_SPACE_DP * density);
        mBadgeVerticalPadding = (int) (BADGE_TEXT_VERTICAL_PADDING_DP * density);

        updateTextBounds();
    }

    public CharSequence getText() {
        return mText;
    }

    public Paint getPaint() {
        return mTextPaint;
    }

    public void setTextColor(int color) {
        if (mTextColor == color)
            return;

        mTextColor = color;
        mCurrentTextColor = color;
        mTextColorStateList = null;
        invalidate();
    }

    /**
     * Set the default text size to the given value, interpreted as "scaled
     * pixel" units. This size is adjusted based on the current density and user
     * font size preference.
     *
     * @param size The scaled pixel size.
     * @attr ref android.R.styleable#TextView_textSize
     */
    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    /**
     * Set the default text size to a given unit and value. See
     * {@link TypedValue} for the possible dimension units.
     *
     * @param unit The desired dimension unit.
     * @param size The desired size in the given units.
     * @attr ref android.R.styleable#TextView_textSize
     */
    public void setTextSize(int unit, float size) {
        Context c = getContext();
        Resources r;

        if (c == null)
            r = Resources.getSystem();
        else
            r = c.getResources();

        setRawTextSize(TypedValue.applyDimension(
                unit, size, r.getDisplayMetrics()));
    }

    private void setRawTextSize(float size) {
        if (size != mTextPaint.getTextSize()) {
            mTextPaint.setTextSize(size);
            mBadgeTextPaint.setTextSize(size * BADGE_TEXT_FONT_SCALE);

            updateTextBounds();
            requestLayout();
            invalidate();
        }
    }

    private void requestLayoutIfNeeded() {
        final ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null && lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            requestLayout();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        applyCurrentDrawableStateTextColor();
    }

    private void applyCurrentDrawableStateTextColor() {
        if (mTextColorStateList != null) {
            mCurrentTextColor = mTextColorStateList.getColorForState(getDrawableState(), 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final String text = mRenderText;
        final TextPaint textPaint = mTextPaint;
        final int textWidth = mMeasuredTextWidth;
        final TextPaint badgePaint = mBadgeTextPaint;
        final int badgeWidth;
        final int badgeAndSpaceWidth;

        if (mMeasuredBadgeTextWidth >= 0) {
            badgeWidth = mMeasuredBadgeTextWidth + (mMeasuredBadgeLineHeight + mBadgeVerticalPadding * 2);
            badgeAndSpaceWidth = badgeWidth + mBadgeLeftSpace;
        } else {
            badgeWidth = 0;
            badgeAndSpaceWidth = 0;
        }

        canvas.getClipBounds(mClipBounds);

        {
            mMeasuredBounds.left = 0;
            mMeasuredBounds.top = 0;
            mMeasuredBounds.right = getMeasuredWidth();
            mMeasuredBounds.bottom = getMeasuredHeight();
            // mClipBounds.left = 0;
            // mClipBounds.top = 0;
            Gravity.apply(
                    mGravity,
                    textWidth + badgeAndSpaceWidth,
                    mMeasuredTextLineHeight,
                    mMeasuredBounds, mGravityRect);
            Gravity.applyDisplay(mGravity, mMeasuredBounds, mGravityRect);

            final Paint.FontMetricsInt fm = getTextFontMetricsInt();
            final int textLeft = mGravityRect.left;
            final int textBottom = mGravityRect.bottom - (fm.descent + fm.leading);

            textPaint.setColor(mCurrentTextColor);
            canvas.drawText(text, textLeft, textBottom, textPaint);
        }

        if (badgeWidth > 0) {
            int r = mGravityRect.bottom;
            RectF rect = mTempRectF;

            int left = mGravityRect.right - badgeWidth;
            int top = mGravityRect.top - mBadgeVerticalPadding;
            int bottom = mGravityRect.bottom + mBadgeVerticalPadding;

            rect.set(left, top, left + badgeWidth, bottom);

            canvas.drawRoundRect(rect, r, r, textPaint);

            mMeasuredBounds.left = left;
            mMeasuredBounds.top = top;
            mMeasuredBounds.right = left + badgeWidth;
            mMeasuredBounds.bottom = bottom;

            Gravity.apply(
                    Gravity.CENTER,
                    mMeasuredBadgeTextWidth, mMeasuredBadgeLineHeight,
                    mMeasuredBounds, mGravityRect);

            final Paint.FontMetricsInt fm = getBadgeFontMetricsInt();
            final int textLeft = mGravityRect.left;
            final int textBottom = mGravityRect.bottom - (fm.descent + fm.leading);

            badgePaint.setColor(mBadgeTextColor);

            canvas.drawText(mBadgeText, textLeft, textBottom, badgePaint);
        }
    }

    private Paint.FontMetricsInt getTextFontMetricsInt() {
        if (mTextFontMetrics == null) {
            mTextFontMetrics = mTextPaint.getFontMetricsInt();
        } else {
            mTextPaint.getFontMetricsInt(mTextFontMetrics);
        }
        return mTextFontMetrics;
    }

    private Paint.FontMetricsInt getBadgeFontMetricsInt() {
        if (mBadgeFontMetrics == null) {
            mBadgeFontMetrics = mBadgeTextPaint.getFontMetricsInt();
        } else {
            mBadgeTextPaint.getFontMetricsInt(mBadgeFontMetrics);
        }
        return mBadgeFontMetrics;
    }

    private void updateTextBounds() {
        {
            final Paint paint = mTextPaint;
            final Paint.FontMetricsInt fm = getTextFontMetricsInt();
            final String text = mRenderText;

            mMeasuredTextLineHeight = getLineHeight(fm);
            mMeasuredTextWidth = getTextWidth(paint, text);
        }

        if (mBadgeText != null) {
            final Paint paint = mBadgeTextPaint;
            final Paint.FontMetricsInt fm = getBadgeFontMetricsInt();
            final String text = mBadgeText;

            mMeasuredBadgeLineHeight = getLineHeight(fm);
            mMeasuredBadgeTextWidth = getTextWidth(paint, text);
        } else {
            mMeasuredBadgeTextWidth = -1;
        }
    }

    public void setGravity(int gravity) {
        if (gravity != mGravity) {
            mGravity = gravity;
            invalidate();
        }
    }

    public void setText(CharSequence text) {
        text = (text == null) ? "" : text;

        if (mText.equals(text))
            return;

        setText(text, mAllCaps, true);

        updateTextBounds();
        requestLayoutIfNeeded();
        invalidate();
    }

    private void setText(CharSequence text, boolean allCaps, boolean updateBadge) {
        if (updateBadge) {
            Matcher m = BADGE_TEXT_PATTERN.matcher(text);
            if (m.find()) {
                mText = m.group(1);
                mBadgeText = m.group(2);
            } else {
                mText = text.toString();
                mBadgeText = null;
            }
        } else {
            mText = text.toString();
        }

        mRenderText = (allCaps) ? mText.toUpperCase() : mText;

        updateTextBounds();
        requestLayoutIfNeeded();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateTextBounds();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mMeasuredTextWidth <= 0 || mMeasuredTextLineHeight <= 0) {
            updateTextBounds();
        }

        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        int result = 0;
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = mMeasuredTextWidth + getPaddingLeft() + getPaddingRight();

            if (mMeasuredBadgeTextWidth >= 0) {
                int badgeWidth = mMeasuredBadgeTextWidth + (mMeasuredBadgeLineHeight + mBadgeVerticalPadding * 2);
                result += mBadgeLeftSpace + badgeWidth;
            }

            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }

        // clipping
        result = Math.max(result, mMinWidth);

        return result;
    }

    private int measureHeight(int measureSpec) {
        int result = 0;
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = mMeasuredTextLineHeight + getPaddingTop() + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }

        // clipping
        result = Math.max(result, mMinHeight);

        return result;
    }

    private static TextUtils.TruncateAt getTruncateAt(TypedArray ta, int id, TextUtils.TruncateAt defValue) {
        final int value = ta.getInt(id, -1);
        final TextUtils.TruncateAt[] table = new TextUtils.TruncateAt[]{
                null,
                TextUtils.TruncateAt.START,
                TextUtils.TruncateAt.MIDDLE,
                TextUtils.TruncateAt.END,
                TextUtils.TruncateAt.MARQUEE
        };

        if (value >= 0 && value < table.length) {
            return table[value];
        } else {
            return defValue;
        }
    }

    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setTypeface(tf);

            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            boolean fakeBold = (need & Typeface.BOLD) != 0;
            float skewX = (need & Typeface.ITALIC) != 0 ? -0.25f : 0;
            mTextPaint.setFakeBoldText(fakeBold);
            mTextPaint.setTextSkewX(skewX);
            mBadgeTextPaint.setFakeBoldText(fakeBold);
            mBadgeTextPaint.setTextSkewX(skewX);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            mBadgeTextPaint.setFakeBoldText(false);
            mBadgeTextPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    private void setTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);
            mBadgeTextPaint.setTypeface(tf);

            requestLayout();
            invalidate();
        }
    }

    public void setAllCaps(boolean allCaps) {
        if (mAllCaps == allCaps) {
            return;
        }
        mAllCaps = allCaps;
        setText(mText, mAllCaps, false);
    }

    public void setTextColor(ColorStateList textColor) {
        if (mTextColorStateList == textColor) {
            return;
        }
        mTextColorStateList = textColor;
        applyCurrentDrawableStateTextColor();
        invalidate();
    }

    private static int getTextWidth(Paint paint, String text) {
        return (int) Math.ceil(paint.measureText(text));
    }

    private static int getLineHeight(Paint.FontMetricsInt fm) {
        return fm.descent - fm.ascent + fm.leading;
    }
}
