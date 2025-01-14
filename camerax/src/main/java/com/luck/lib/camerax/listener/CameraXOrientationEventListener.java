package com.luck.lib.camerax.listener;

import android.content.Context;
import android.view.OrientationEventListener;
import android.view.Surface;

/**
 * @author：luck
 * @date：2022/6/4 3:28 下午
 * @describe：CameraXOrientationEventListener
 */
public class CameraXOrientationEventListener extends OrientationEventListener {
    private int mRotation = Surface.ROTATION_0;

    public CameraXOrientationEventListener(Context context, OnOrientationChangedListener listener) {
        super(context);
        this.changedListener = listener;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return;
        }
        int currentRotation;
        if (orientation >= 45 && orientation < 135) {
            currentRotation = Surface.ROTATION_270;
        } else if (orientation >= 135 && orientation < 225) {
            currentRotation = Surface.ROTATION_180;
        } else if (orientation >= 225 && orientation < 315) {
            currentRotation = Surface.ROTATION_90;
        } else {
            currentRotation = Surface.ROTATION_0;
        }
        if (mRotation != currentRotation) {
            mRotation = currentRotation;
            if (changedListener != null) {
                changedListener.onOrientationChanged(mRotation);
            }
        }
    }

    private OnOrientationChangedListener changedListener;

    public interface OnOrientationChangedListener {
        void onOrientationChanged(int orientation);
    }

    public void star() {
        enable();
    }

    public void stop() {
        disable();
    }
}
