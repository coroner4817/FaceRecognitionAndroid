package com.clarifai.clarapplai.yingnan;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.clarifai.clarapplai.R;

/**
 * Created by YingnanWang on 4/14/17.
 */

/**
 * Reference my previous project: https://github.com/coroner4817/HangridStatusChecker/blob/master/app/src/main/java/com/yingnanwang/statuschecker/widget/BreathButton.java
 * A breath ripple effect view, on the top of the base imageview to show the face finding result
 */

public class TagDrawView extends View{

    private final String TAG = getClass().getSimpleName();

    private static final int ANIMATION_TIME = 1500;
    private static final int RIPPLE_WIDTH_DIP_1 = 20;
    private static final int RIPPLE_WIDTH_DIP_2 = 20;
    private static final int SHADOW_ALPHA = 75;
    private static final int RIPPLE_STROKE_WIDTH = 10;
    private static final int RIPPLE_COLOR = Color.parseColor("#418BCA");

    private boolean isAniming = false;
    private boolean isClearCanvas = true;

    private Paint ripplePaint;
    private Paint shadowPaint;
    private Paint erasePaint;
    private ObjectAnimator mAnimator;
    private float animationProgress;

    private int imageviewW;
    private int imageviewH;
    private int bitmapRight;
    private int bitmapBottom;
    private int bitmapOriginX;
    private int bitmapOriginY;
    private int centerX;
    private int centerY;
    private int rippleWidth1;
    private int rippleWidth2;
    private int rippleRadius1;
    private int rippleRadius2;

    public TagDrawView(Context context) {
        super(context);
        init(context, null);
    }

    public TagDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TagDrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.setFocusable(true);
        this.setClickable(false);
        // turn off HWA to enable erasing
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mAnimator = ObjectAnimator.ofFloat(this, "animationProgress", 0f, 0f);
        mAnimator.setDuration(ANIMATION_TIME);

        ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setColor(RIPPLE_COLOR);
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(RIPPLE_STROKE_WIDTH);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(SHADOW_ALPHA);
        shadowPaint.setStyle(Paint.Style.FILL);

        erasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));


        rippleWidth1 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RIPPLE_WIDTH_DIP_1,
                getResources().getDisplayMetrics());
        rippleWidth2 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RIPPLE_WIDTH_DIP_2,
                getResources().getDisplayMetrics());

        if(attrs != null){
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TagDrawView);
            rippleWidth1 = (int) a.getDimension(R.styleable.TagDrawView_rb_ripple_width_1, rippleWidth1);
            rippleWidth2 = (int) a.getDimension(R.styleable.TagDrawView_rb_ripple_width_2, rippleWidth2);
            a.recycle();
        }
    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    public void setAnimationProgress(float animationProgress) {
        this.animationProgress = animationProgress;
        this.invalidate();
    }

    public boolean getIsAniming(){
        return isAniming;
    }

    public void startAnim(int x, int y, int minr, Matrix tf, int bmW, int bmH){
        isAniming = true;
        isClearCanvas = false;

        float[] pts = { x, y };
        tf.mapPoints(pts);
        centerX = (int)pts[0];
        centerY = (int)pts[1];

        pts[0]=0f;pts[1]=0f;
        tf.mapPoints(pts);
        bitmapOriginX = (int)pts[0];
        bitmapOriginY = (int)pts[1];

        RectF bitmapRect = new RectF();
        bitmapRect.right = bmW;
        bitmapRect.bottom = bmH;
        tf.mapRect(bitmapRect);
        bitmapRight = (int)bitmapRect.width() + bitmapOriginX;
        bitmapBottom = (int)bitmapRect.height() + bitmapOriginY;

        // init radius
        rippleRadius1 = minr + rippleWidth1 / 2;
        rippleRadius2 = rippleRadius1 + rippleWidth2 / 2;

        if(mAnimator.isStarted()){
            mAnimator.end();
        }

        mAnimator.setFloatValues(0f, 10f);
        mAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mAnimator.setRepeatMode(ObjectAnimator.RESTART);
        mAnimator.start();
    }

    public void stopAnim(){
        isAniming = false;

        if(mAnimator.isStarted()){
            mAnimator.end();
        }
        this.invalidate();
    }

    private float animationRipple1(float prog){
        if(prog < 5){
            return rippleWidth1 / 2f / 5f * prog;
        }else{
            return  - rippleWidth1 / 2f / 5f * prog + 2f * rippleWidth1 / 2f;
        }
    }

    private float animationRipple2(float prog){
        if(prog < 2){
            return 0;
        } else if(2 <= prog && prog < 6){
            return (rippleWidth1 / 2f + rippleWidth2 / 2f) / 4f * prog - (rippleWidth1 / 2f + rippleWidth2 / 2f) / 2f;
        }else if(6 <= prog && prog <= 10){
            return - (rippleWidth1 / 2f + rippleWidth2 / 2f) / 4f * prog + 5f / 2f * (rippleWidth1 / 2f + rippleWidth2 / 2f);
        }else{
            return 0;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: " + w + ", " + h);
        imageviewW = w;
        imageviewH = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(TAG, "onDraw: ");
        super.onDraw(canvas);

        if(!isClearCanvas){
            canvas.drawRect(bitmapOriginX, bitmapOriginY, bitmapRight, bitmapBottom, shadowPaint);
            canvas.drawCircle(centerX, centerY, rippleRadius1, erasePaint);
            if(!isAniming){
                canvas.drawCircle(centerX, centerY, rippleRadius1, ripplePaint);
            }else{
                canvas.drawCircle(centerX, centerY, rippleRadius2 + animationRipple2(animationProgress), ripplePaint);
                canvas.drawCircle(centerX, centerY, rippleRadius1 + animationRipple1(animationProgress), ripplePaint);
            }
        }else{
//            Log.d(TAG, "onClear: ");
            canvas.drawRect(bitmapOriginX, bitmapOriginY, bitmapRight, bitmapBottom, erasePaint);
        }
    }

    public void plotTag(String tag){
        //TODO
    }

    public void clearCanvas(){
        Log.d(TAG, "onClear: ");
        isClearCanvas = true;
        invalidate();
    }
}
