package com.hiddenspot.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatSpinner;

import com.hiddenspot.app.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LimitedHeightSpinner extends AppCompatSpinner {

    private int maxPopupHeightPx = 0;

    public LimitedHeightSpinner(Context context) {
        super(context);
    }

    public LimitedHeightSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LimitedHeightSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs == null) return;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LimitedHeightSpinner);
        maxPopupHeightPx = a.getDimensionPixelSize(R.styleable.LimitedHeightSpinner_maxPopupHeight, 0);
        a.recycle();
    }

    @Override
    public boolean performClick() {
        applyPopupHeight();
        return super.performClick();
    }

    private void applyPopupHeight() {
        if (maxPopupHeightPx <= 0) return;

        try {
            Field popupField = AppCompatSpinner.class.getDeclaredField("mPopup");
            popupField.setAccessible(true);
            Object popup = popupField.get(this);
            if (popup == null) return;

            Method setHeightMethod = popup.getClass().getMethod("setHeight", int.class);
            setHeightMethod.invoke(popup, maxPopupHeightPx);
        } catch (Exception ignored) {
        }
    }
}
