package com.imdacode.bharatfreetv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class VideoViewport extends FrameLayout {
    private String aspectMode = AppPreferences.ASPECT_AUTO;

    public VideoViewport(@NonNull Context context) {
        super(context);
    }

    public VideoViewport(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAspectMode(String aspectMode) {
        this.aspectMode = aspectMode;
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int availableWidth = right - left;
        int availableHeight = bottom - top;
        float requestedRatio = getRequestedRatio();
        int childLeft = 0;
        int childTop = 0;
        int childRight = availableWidth;
        int childBottom = availableHeight;

        if (requestedRatio > 0f && availableWidth > 0 && availableHeight > 0) {
            float availableRatio = (float) availableWidth / availableHeight;
            if (availableRatio > requestedRatio) {
                int width = Math.round(availableHeight * requestedRatio);
                childLeft = (availableWidth - width) / 2;
                childRight = childLeft + width;
            } else {
                int height = Math.round(availableWidth / requestedRatio);
                childTop = (availableHeight - height) / 2;
                childBottom = childTop + height;
            }
        }

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child.getVisibility() != GONE) {
                child.layout(childLeft, childTop, childRight, childBottom);
            }
        }
    }

    private float getRequestedRatio() {
        if (AppPreferences.ASPECT_16_9.equals(aspectMode)) {
            return 16f / 9f;
        }
        if (AppPreferences.ASPECT_4_3.equals(aspectMode)) {
            return 4f / 3f;
        }
        return 0f;
    }
}
