package com.astuetz;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

public class TabTextView extends View {
    @SuppressWarnings("unused")
    private static final String TAG = "TabTextView";

    private String mText = "";
    private String mRenderText = "";
    private TextPaint mTextPaint;
    private int mTextColor;
    private int mCurrentTextColor;
    private ColorStateList mTextColorStateList;
    private int mGravity;
    private Rect mClipBounds;
    private Rect mGravityRect;
    private Rect mMeasuredBounds;
    private int mMeasuredLineHeight = -1;
    private int mMeasuredTextWidth = -1;
    private Paint.FontMetricsInt mFontMetrics;
    private int mMinWidth = 0;
    private int mMinHeight = 0;
    private boolean mAllCaps = false;

    public TabTextView(Context context) {
        super(context);
        init();
    }

    void init() {
        final Resources res = getResources();

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.density = res.getDisplayMetrics().density;
        mTextPaint.setTextSize(15.0f);
        mTextColor = mCurrentTextColor = Color.WHITE;
        mTextColorStateList = null;
        mGravity = Gravity.TOP | Gravity.LEFT;
        mClipBounds = new Rect();
        mGravityRect = new Rect();
        mMeasuredBounds = new Rect();

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
        final TextPaint paint = mTextPaint;
        final int textWidth = mMeasuredTextWidth;

        paint.setColor(mCurrentTextColor);

        canvas.getClipBounds(mClipBounds);

        mMeasuredBounds.left = 0;
        mMeasuredBounds.top = 0;
        mMeasuredBounds.right = getMeasuredWidth();
        mMeasuredBounds.bottom = getMeasuredHeight();
        // mClipBounds.left = 0;
        // mClipBounds.top = 0;
        Gravity.apply(
                mGravity,
                textWidth, mMeasuredLineHeight,
                mMeasuredBounds, mGravityRect);
        Gravity.applyDisplay(mGravity, mMeasuredBounds, mGravityRect);

        final Paint.FontMetricsInt fm = getFontMetricsInt();
        final int textLeft = mGravityRect.left;
        final int textBottom = mGravityRect.bottom - (fm.descent + fm.leading);

        canvas.drawText(text, textLeft, textBottom, paint);
    }

    private Paint.FontMetricsInt getFontMetricsInt() {
        if (mFontMetrics == null) {
            mFontMetrics = mTextPaint.getFontMetricsInt();
        } else {
            mTextPaint.getFontMetricsInt(mFontMetrics);
        }
        return mFontMetrics;
    }

    private void updateTextBounds() {
        final Paint.FontMetricsInt fm = getFontMetricsInt();
        final String text = mRenderText;

        mMeasuredLineHeight = fm.descent - fm.ascent + fm.leading;
        mMeasuredTextWidth = (int) Math.ceil(mTextPaint.measureText(text));
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

        setText(text, mAllCaps);

        mText = text.toString();

        updateTextBounds();
        requestLayoutIfNeeded();
        invalidate();
    }

    private void setText(CharSequence text, boolean allCaps) {

        mText = text.toString();
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

        if (mMeasuredTextWidth <= 0 || mMeasuredLineHeight <= 0) {
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
            result = mMeasuredLineHeight + getPaddingTop() + getPaddingBottom();
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
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    private void setTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);

            requestLayout();
            invalidate();
        }
    }

    public void setAllCaps(boolean allCaps) {
        if (mAllCaps == allCaps) {
            return;
        }
        mAllCaps = allCaps;
        setText(mText, mAllCaps);
    }

    public void setTextColor(ColorStateList textColor) {
        if (mTextColorStateList == textColor) {
            return;
        }
        mTextColorStateList = textColor;
        applyCurrentDrawableStateTextColor();
        invalidate();
    }
}
