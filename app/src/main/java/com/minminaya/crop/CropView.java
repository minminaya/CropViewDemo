package com.minminaya.crop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.UiThread;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;


/**
 * 旋转裁剪的View类
 *
 * @author minminaya
 * @time Created by 2018/8/20 17:00
 */
public class CropView extends AppCompatImageView {

    private final static String TAG = CropView.class.getSimpleName();

    private final static int HANDLE_SIZE = 24;
    private final static int HANDLE_WIDTH = 2;
    private final static int FRAME_MIN_SIZE = 50;
    private final static int FRAME_STROKE_WEIGHT = 1;
    private final static int GUIDE_STROKE_WEIGHT = 1;
    private final static float DEFAULT_INITIAL_SCALE = 1f;
    // 颜色值，包含透明度
    private static final int TRANSPARENT = 0x00000000;
    private static final int TRANSLUCENT_WHITE = 0xBBFFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int TRANSLUCENT_BLACK = 0xBB000000;
    // 默认的动画时长
    private static final int DEFAULT_ANIMATION_DURATION_MILLIS = 100;

    // view的宽高
    private int mViewWidth = 0;
    private int mViewHeight = 0;
    // 放大比例
    private float mCropScale = 1.0f;
    // 旋转角度
    private float mImgAngle = 0.0f;
    // 图片宽度
    private float mImgWidth = 0.0f;
    // 图片高度
    private float mImgHeight = 0.0f;
    // 四角线的长度值
    private int mHandleSize;
    // 四角的线宽度
    private int mHandleWidth;
    // 触摸可处理的宽度值
    private int mTouchPadding = 0;
    // 裁剪框最小值
    private int mFrameMinSize;
    // 裁剪外框线框宽度
    private float mFrameStrokeWeight = 2f;
    // 裁剪指导线宽度
    private float mGuideStrokeWeight = 2f;
    // 外框画笔
    private Paint mFramePaint;
    // 半透明区域画笔
    private Paint mTranslucentPaint;
    // bitmap画笔
    private Paint mBitmapPaint;

    // 背景的颜色
    private int mBackgroundColor;
    // 半透明框的颜色
    private int mOverlayColor;
    // 外框的颜色
    private int mFrameColor;
    // 四边角点的颜色
    private int mHandleColor;
    // 指导线颜色
    private int mGuideColor;
    // 裁剪模式
    private CropModeEnum mCropMode = CropModeEnum.RATIO_2_3;
    // 指导线的显示模式
    private ShowModeEnum mGuideShowMode = ShowModeEnum.SHOW_ALWAYS;
    // 处理四个角的显示模式
    private ShowModeEnum mHandleShowMode = ShowModeEnum.SHOW_ALWAYS;
    // 触摸的情况
    private TouchAreaEnum mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS;
    // 居中中点的矩型区域
    private PointF mCenter = new PointF();
    // 图形的矩阵类
    private Matrix mMatrix = null;
    // 图片的rectf区域
    private RectF mImageRectF;
    // 裁剪框的RectF
    private RectF mFrameRectF;
    // 初始化的比例大小0.01到1.0，默认情况为0.75
    private float mInitialFrameScale;
    // 是否初始化
    private boolean mIsInitialized;
    // 是否开始裁剪
    private boolean mIsCropEnabled = true;
    // 是否显示指导线
    private boolean mShowGuide = true;
    // 是否显示处理线
    private boolean mShowHandle = true;

    private float mLastX, mLastY;
    // 旋转的动画
    private ValueAnimator mValueAnimator = null;
    // 裁剪框放大的动画
    private ValueAnimator mFrameValueAnimator = null;
    private boolean mIsAnimationEnabled = true;
    private int mAnimationDurationMillis = DEFAULT_ANIMATION_DURATION_MILLIS;

    private boolean mIsRotating = false;
    private boolean mIsAnimating = false;

    private OnCropListener mOnCropListener;

    // 是否已经翻转了
    private boolean mIsReverseY = false;
    // 是否有旋转操作
    private boolean mIsRotated = false;
    // 旋转操作的开关
    private boolean mRotateSwitch = true;

    public CropView(Context context) {
        this(context, null);
    }

    public CropView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHandleSize = SizeUtils.dp2px(HANDLE_SIZE);
        mFrameMinSize = SizeUtils.dp2px(FRAME_MIN_SIZE);
        mFrameStrokeWeight = SizeUtils.dp2px(FRAME_STROKE_WEIGHT);
        mGuideStrokeWeight = SizeUtils.dp2px(GUIDE_STROKE_WEIGHT);

        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTranslucentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // 抗锯齿处理
        mBitmapPaint.setFilterBitmap(true);

        mMatrix = new Matrix();
        mCropScale = 1.0f;

        mBackgroundColor = TRANSPARENT;
        mFrameColor = WHITE;
        mOverlayColor = TRANSLUCENT_BLACK;
        mHandleColor = WHITE;
        mGuideColor = TRANSLUCENT_WHITE;
        loadStyleable(context, attrs, defStyleAttr);

        mValueAnimator = ValueAnimator.ofFloat(0, 1f);
        mValueAnimator.setInterpolator(new DecelerateInterpolator());
        mValueAnimator.setDuration(mAnimationDurationMillis);

        mFrameValueAnimator = ValueAnimator.ofFloat(0, 1f);
        mFrameValueAnimator.setInterpolator(new DecelerateInterpolator());
        mFrameValueAnimator.setDuration(mAnimationDurationMillis);
    }

    public void setBgColor(@ColorInt int backgroundColor) {
        mBackgroundColor = backgroundColor;
        invalidate();
    }

    /**
     * 加载Style自定义属性数据
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    private void loadStyleable(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CropView, defStyleAttr, 0);
        Drawable drawable;
        mCropMode = CropModeEnum.SQUARE;
        try {
            drawable = ta.getDrawable(R.styleable.CropView_img_src);
            if (drawable != null)
                setImageDrawable(drawable);
            for (CropModeEnum mode : CropModeEnum.values()) {
                if (ta.getInt(R.styleable.CropView_crop_mode, 3) == mode.getID()) {
                    mCropMode = mode;
                    break;
                }
            }
            mBackgroundColor = ta.getColor(R.styleable.CropView_background_color, TRANSPARENT);
            mOverlayColor = ta.getColor(R.styleable.CropView_overlay_color, TRANSLUCENT_BLACK);
            mFrameColor = ta.getColor(R.styleable.CropView_frame_color, WHITE);
            mHandleColor = ta.getColor(R.styleable.CropView_handle_color, WHITE);
            mGuideColor = ta.getColor(R.styleable.CropView_guide_color, TRANSLUCENT_WHITE);
            for (ShowModeEnum mode : ShowModeEnum.values()) {
                if (ta.getInt(R.styleable.CropView_guide_show_mode, 1) == mode.getId()) {
                    mGuideShowMode = mode;
                    break;
                }
            }

            for (ShowModeEnum mode : ShowModeEnum.values()) {
                if (ta.getInt(R.styleable.CropView_handle_show_mode, 1) == mode.getId()) {
                    mHandleShowMode = mode;
                    break;
                }
            }
            setGuideShowMode(mGuideShowMode);
            setHandleShowMode(mHandleShowMode);
            mHandleSize = ta.getDimensionPixelSize(R.styleable.CropView_handle_size, SizeUtils.dp2px(HANDLE_SIZE));
            mHandleWidth = ta.getDimensionPixelSize(R.styleable.CropView_handle_width, SizeUtils.dp2px(HANDLE_WIDTH));
            mTouchPadding = ta.getDimensionPixelSize(R.styleable.CropView_touch_padding, 0);
            mFrameMinSize =
                    ta.getDimensionPixelSize(R.styleable.CropView_min_frame_size, SizeUtils.dp2px(FRAME_MIN_SIZE));
            mFrameStrokeWeight =
                    ta.getDimensionPixelSize(R.styleable.CropView_frame_stroke_weight, SizeUtils.dp2px(FRAME_STROKE_WEIGHT));
            mGuideStrokeWeight =
                    ta.getDimensionPixelSize(R.styleable.CropView_guide_stroke_weight, SizeUtils.dp2px(GUIDE_STROKE_WEIGHT));
            mIsCropEnabled = ta.getBoolean(R.styleable.CropView_crop_enabled, true);
            mInitialFrameScale =
                    constrain(ta.getFloat(R.styleable.CropView_initial_frame_scale, DEFAULT_INITIAL_SCALE), 0.01f, 1.0f,
                            DEFAULT_INITIAL_SCALE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ta.recycle();
        }
    }

    /**
     * 设置指导线的显示模式
     *
     * @param guideShowMode
     */
    public void setGuideShowMode(ShowModeEnum guideShowMode) {
        mGuideShowMode = guideShowMode;
        switch (guideShowMode) {
            case SHOW_ALWAYS:
                mShowGuide = true;
                break;
            case NOT_SHOW:
            case SHOW_ON_TOUCH:
                mShowGuide = false;
                break;
        }
        invalidate();
    }

    /**
     * 设置处理线的显示模式
     *
     * @param mode
     */
    public void setHandleShowMode(ShowModeEnum mode) {
        mHandleShowMode = mode;
        switch (mode) {
            case SHOW_ALWAYS:
                mShowHandle = true;
                break;
            case NOT_SHOW:
            case SHOW_ON_TOUCH:
                mShowHandle = false;
                break;
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(viewWidth, viewHeight);
        mViewWidth = viewWidth - getPaddingLeft() - getPaddingRight();
        mViewHeight = viewHeight - getPaddingTop() - getPaddingBottom();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getDrawable() != null) {
            doLayout(mViewWidth, mViewHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(mBackgroundColor);

        if (mIsInitialized) {
            setMatrix();
            Bitmap bitmap = getBitmap();
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, mMatrix, mBitmapPaint);
                // 画裁剪框
                drawCropFrame(canvas);
            }
        }
    }

    /**
     * 画裁剪框
     *
     * @param canvas
     */
    private void drawCropFrame(Canvas canvas) {
        if (!mIsCropEnabled) {
            // 如果不是裁剪模式，直接就不画裁剪框
            return;
        }
        if (mIsRotating) {
            // 如果正在旋转，不画裁剪框
            return;
        }
        drawOverlay(canvas);
        drawFrame(canvas);
        if (mShowGuide) {
            drawGuidelines(canvas);
        }
        if (mShowHandle) {
            drawHandleLines(canvas);
        }
    }

    /**
     * 画四角的线
     *
     * @param canvas
     */
    private void drawHandleLines(Canvas canvas) {
        mFramePaint.setColor(mHandleColor);
        mFramePaint.setStyle(Paint.Style.FILL);
        // 指导线最边界（最左/最右/最下/最上）的x和y
        float handleLineLeftX = mFrameRectF.left - mHandleWidth;
        float handleLineRightX = mFrameRectF.right + mHandleWidth;
        float handleLineTopY = mFrameRectF.top - mHandleWidth;
        float handleLineBottomY = mFrameRectF.bottom + mHandleWidth;
        // 左上竖向
        RectF ltRectFVertical =
                new RectF(handleLineLeftX, handleLineTopY, mFrameRectF.left, handleLineTopY + mHandleSize);
        // 左上横向
        RectF ltRectFHorizontal =
                new RectF(handleLineLeftX, handleLineTopY, handleLineLeftX + mHandleSize, mFrameRectF.top);

        RectF rtRectFHorizontal =
                new RectF(handleLineRightX - mHandleSize, handleLineTopY, handleLineRightX, mFrameRectF.top);
        RectF rtRectFVertical =
                new RectF(mFrameRectF.right, handleLineTopY, handleLineRightX, handleLineTopY + mHandleSize);

        RectF lbRectFVertical =
                new RectF(handleLineLeftX, handleLineBottomY - mHandleSize, mFrameRectF.left, mFrameRectF.bottom);
        RectF lbRectFHorizontal =
                new RectF(handleLineLeftX, mFrameRectF.bottom, handleLineLeftX + mHandleSize, handleLineBottomY);

        RectF rbRectFVertical =
                new RectF(mFrameRectF.right, handleLineBottomY - mHandleSize, handleLineRightX, handleLineBottomY);
        RectF rbRectFHorizontal =
                new RectF(handleLineRightX - mHandleSize, mFrameRectF.bottom, handleLineRightX, handleLineBottomY);

        canvas.drawRect(ltRectFVertical, mFramePaint);
        canvas.drawRect(ltRectFHorizontal, mFramePaint);

        canvas.drawRect(rtRectFVertical, mFramePaint);
        canvas.drawRect(rtRectFHorizontal, mFramePaint);

        canvas.drawRect(lbRectFVertical, mFramePaint);
        canvas.drawRect(lbRectFHorizontal, mFramePaint);

        canvas.drawRect(rbRectFVertical, mFramePaint);
        canvas.drawRect(rbRectFHorizontal, mFramePaint);

        if (mCropMode == CropModeEnum.FREE) {
            // 如果当前是自由模式
            mFramePaint.setStrokeCap(Paint.Cap.ROUND);
            mFramePaint.setStrokeWidth(mHandleWidth + SizeUtils.dp2px(2));

            float centerX = mFrameRectF.left + mFrameRectF.width() / 2;
            float centerY = mFrameRectF.top + mFrameRectF.height() / 2;

            canvas.drawLine(centerX - mHandleSize, mFrameRectF.top, (centerX + mHandleSize), mFrameRectF.top,
                    mFramePaint);
            canvas.drawLine(centerX - mHandleSize, mFrameRectF.bottom, centerX + mHandleSize, mFrameRectF.bottom,
                    mFramePaint);
            canvas.drawLine(mFrameRectF.left, (centerY - mHandleSize), mFrameRectF.left, centerY + mHandleSize,
                    mFramePaint);
            canvas.drawLine(mFrameRectF.right, centerY - mHandleSize, mFrameRectF.right, centerY + mHandleSize,
                    mFramePaint);
        }
    }

    /**
     * 画指导线
     *
     * @param canvas
     */
    private void drawGuidelines(Canvas canvas) {
        mFramePaint.setColor(mGuideColor);
        mFramePaint.setStrokeWidth(mGuideStrokeWeight);
        // 从左往右第一个竖线的横坐标
        float x1 = mFrameRectF.left + (mFrameRectF.right - mFrameRectF.left) / 3.0f;
        float x2 = mFrameRectF.right - (mFrameRectF.right - mFrameRectF.left) / 3.0f;
        // 从上往下第一个横的y坐标
        float y1 = mFrameRectF.top + (mFrameRectF.bottom - mFrameRectF.top) / 3.0f;
        float y2 = mFrameRectF.bottom - (mFrameRectF.bottom - mFrameRectF.top) / 3.0f;
        // 画竖线
        canvas.drawLine(x1, mFrameRectF.top, x1, mFrameRectF.bottom, mFramePaint);
        canvas.drawLine(x2, mFrameRectF.top, x2, mFrameRectF.bottom, mFramePaint);
        // 画横线
        canvas.drawLine(mFrameRectF.left, y1, mFrameRectF.right, y1, mFramePaint);
        canvas.drawLine(mFrameRectF.left, y2, mFrameRectF.right, y2, mFramePaint);
    }

    /**
     * 画裁剪框边界线
     *
     * @param canvas
     */
    private void drawFrame(Canvas canvas) {
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setFilterBitmap(true);
        mFramePaint.setColor(mFrameColor);
        mFramePaint.setStrokeWidth(mFrameStrokeWeight);
        canvas.drawRect(mFrameRectF, mFramePaint);
    }

    /**
     * 画裁剪框的半透明覆盖层
     *
     * @param canvas
     */
    private void drawOverlay(Canvas canvas) {
        mTranslucentPaint.setStyle(Paint.Style.FILL);
        mTranslucentPaint.setFilterBitmap(true);
        mTranslucentPaint.setColor(mOverlayColor);
        Path path = new Path();
        RectF overlayRectF = new RectF();
        overlayRectF.set(mImageRectF);

        path.addRect(mFrameRectF, Path.Direction.CW);
        path.addRect(overlayRectF, Path.Direction.CCW);
        canvas.drawPath(path, mTranslucentPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 如果还没初始化，直接跳过触摸事件
        if (!mIsInitialized)
            return false;
        // 如果没有打开裁剪功能，那么也直接跳过触摸事件
        if (!mIsCropEnabled)
            return false;
        // 如果是在旋转，跳过触摸事件
        if (mIsRotating)
            return false;
        // 如果是在切换裁剪模式，跳过触摸事件
        if (mIsAnimating)
            return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                onActionDown(event);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                onActionMove(event);
                if (mTouchArea != TouchAreaEnum.OUT_OF_BOUNDS) {
                    // 阻止父view拦截点击事件
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                getParent().requestDisallowInterceptTouchEvent(true);
                onActionCancel();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                getParent().requestDisallowInterceptTouchEvent(true);
                onActionUp();
                return true;
            }
            default: {
                break;
            }
        }
        return false;
    }

    private void onActionUp() {
        if (mGuideShowMode == ShowModeEnum.SHOW_ON_TOUCH)
            mShowGuide = false;
        if (mHandleShowMode == ShowModeEnum.SHOW_ON_TOUCH)
            mShowHandle = false;
        mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS;
        invalidate();
    }

    private void onActionCancel() {
        mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS;
        invalidate();
    }

    private void onActionMove(MotionEvent event) {
        float diffX = event.getX() - mLastX;
        float diffY = event.getY() - mLastY;
        // 区分点击的区域进行移动
        switch (mTouchArea) {
            case CENTER: {
                moveFrame(diffX, diffY);
                break;
            }
            case LEFT_TOP: {
                moveHandleLeftTop(diffX, diffY);
                break;
            }
            case RIGHT_TOP: {
                moveHandleRightTop(diffX, diffY);
                break;
            }
            case LEFT_BOTTOM: {
                moveHandleLeftBottom(diffX, diffY);
                break;
            }
            case RIGHT_BOTTOM: {
                moveHandleRightBottom(diffX, diffY);
                break;
            }

            case CENTER_LEFT: {
                moveHandleCenterLeft(diffX);
                break;
            }
            case CENTER_TOP: {
                moveHandleCenterTop(diffY);
                break;
            }
            case CENTER_RIGHT: {
                moveHandleCenterRight(diffX);
                break;
            }
            case CENTER_BOTTOM: {
                moveHandleCenterBottom(diffY);
                break;
            }
            case OUT_OF_BOUNDS: {
                break;
            }
        }
        invalidate();
        mLastX = event.getX();
        mLastY = event.getY();
    }

    private void moveHandleCenterBottom(float diffY) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF.bottom += diffY;
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.bottom += offsetY;
            }
            checkScaleBounds();
        }
    }

    private void moveHandleCenterRight(float diffX) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF.right += diffX;
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.right += offsetX;
            }
            checkScaleBounds();
        }
    }

    private void moveHandleCenterTop(float diffY) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF.top += diffY;
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.top -= offsetY;
            }
            checkScaleBounds();
        }
    }

    private void moveHandleCenterLeft(float diffX) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF.left += diffX;
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.left -= offsetX;
            }
            checkScaleBounds();
        }
    }

    private void moveHandleRightTop(float diffX, float diffY) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF.right += diffX;
            mFrameRectF.top += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.right += offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.top -= offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRectF.right += dx;
            mFrameRectF.top -= dy;
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.right += offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRectF.top -= offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.top -= offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRectF.right += offsetX;
            }
            float ox, oy;
            if (!isInsideX(mFrameRectF.right)) {
                ox = mFrameRectF.right - mImageRectF.right;
                mFrameRectF.right -= ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRectF.top += oy;
            }
            if (!isInsideY(mFrameRectF.top)) {
                oy = mImageRectF.top - mFrameRectF.top;
                mFrameRectF.top += oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRectF.right -= ox;
            }
        }
    }

    private void moveHandleLeftBottom(float diffX, float diffY) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF.left += diffX;
            mFrameRectF.bottom += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.left -= offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.bottom += offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRectF.left += dx;
            mFrameRectF.bottom -= dy;
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.left -= offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRectF.bottom += offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.bottom += offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRectF.left -= offsetX;
            }
            float ox, oy;
            if (!isInsideX(mFrameRectF.left)) {
                ox = mImageRectF.left - mFrameRectF.left;
                mFrameRectF.left += ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRectF.bottom -= oy;
            }
            if (!isInsideY(mFrameRectF.bottom)) {
                oy = mFrameRectF.bottom - mImageRectF.bottom;
                mFrameRectF.bottom -= oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRectF.left += ox;
            }
        }
    }

    private void moveHandleRightBottom(float diffX, float diffY) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF.right += diffX;
            mFrameRectF.bottom += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.right += offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.bottom += offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRectF.right += dx;
            mFrameRectF.bottom += dy;
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.right += offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRectF.bottom += offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.bottom += offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRectF.right += offsetX;
            }
            float ox, oy;
            if (!isInsideX(mFrameRectF.right)) {
                ox = mFrameRectF.right - mImageRectF.right;
                mFrameRectF.right -= ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRectF.bottom -= oy;
            }
            if (!isInsideY(mFrameRectF.bottom)) {
                oy = mFrameRectF.bottom - mImageRectF.bottom;
                mFrameRectF.bottom -= oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRectF.right -= ox;
            }
        }
    }

    private void moveHandleLeftTop(float diffX, float diffY) {
        if (mCropMode == CropModeEnum.FREE) {
            mFrameRectF.left += diffX;
            mFrameRectF.top += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.left -= offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.top -= offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRectF.left += dx;
            mFrameRectF.top += dy;
            // 控制缩放边界
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.left -= offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRectF.top -= offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.top -= offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRectF.left -= offsetX;
            }

            //限制不能让裁剪框超出图片边界
            float ox, oy;
            if (!isInsideX(mFrameRectF.left)) {
                ox = mImageRectF.left - mFrameRectF.left;
                mFrameRectF.left += ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRectF.top += oy;
            }
            if (!isInsideY(mFrameRectF.top)) {
                oy = mImageRectF.top - mFrameRectF.top;
                mFrameRectF.top += oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRectF.left += ox;
            }
        }
    }

    /**
     * 检查缩放边界
     */
    private void checkScaleBounds() {
        float lDiff = mFrameRectF.left - mImageRectF.left;
        float rDiff = mFrameRectF.right - mImageRectF.right;
        float tDiff = mFrameRectF.top - mImageRectF.top;
        float bDiff = mFrameRectF.bottom - mImageRectF.bottom;

        if (lDiff < 0) {
            mFrameRectF.left -= lDiff;
        }
        if (rDiff > 0) {
            mFrameRectF.right -= rDiff;
        }
        if (tDiff < 0) {
            mFrameRectF.top -= tDiff;
        }
        if (bDiff > 0) {
            mFrameRectF.bottom -= bDiff;
        }
    }

    /**
     * x 是否在图片内
     *
     * @param x
     * @return
     */
    private boolean isInsideX(float x) {
        return mImageRectF.left <= x && mImageRectF.right >= x;
    }

    private boolean isInsideY(float y) {
        return mImageRectF.top <= y && mImageRectF.bottom >= y;
    }

    private boolean isHeightTooSmall() {
        return mFrameRectF.height() < mFrameMinSize;
    }

    private boolean isWidthTooSmall() {
        return mFrameRectF.width() < mFrameMinSize;
    }

    private void moveFrame(float x, float y) {
        // 1.先平移
        mFrameRectF.left += x;
        mFrameRectF.right += x;
        mFrameRectF.top += y;
        mFrameRectF.bottom += y;
        // 2.判断有没有超出界外，如果超出则后退
        handleMoveBounds();
    }

    /**
     * 控制不超出界外
     */
    private void handleMoveBounds() {
        float diff = mFrameRectF.left - mImageRectF.left;
        if (diff < 0) {
            mFrameRectF.left -= diff;
            mFrameRectF.right -= diff;
        }
        diff = mFrameRectF.right - mImageRectF.right;
        if (diff > 0) {
            mFrameRectF.left -= diff;
            mFrameRectF.right -= diff;
        }
        diff = mFrameRectF.top - mImageRectF.top;
        if (diff < 0) {
            mFrameRectF.top -= diff;
            mFrameRectF.bottom -= diff;
        }
        diff = mFrameRectF.bottom - mImageRectF.bottom;
        if (diff > 0) {
            mFrameRectF.top -= diff;
            mFrameRectF.bottom -= diff;
        }
    }

    private void onActionDown(MotionEvent event) {
        invalidate();
        mLastX = event.getX();
        mLastY = event.getY();
        handleTouchArea(mLastX, mLastY);
    }

    /**
     * <Strong>控制指导线或者边框线的显示</Strong>
     * <p></p>
     * 处理触摸的边界来控制指导线或者边框线的显示，共5种，四个角和里面的中心部分
     *
     * @param x
     * @param y
     */
    private void handleTouchArea(float x, float y) {
        if (isInLeftTopCorner(x, y)) {
            mTouchArea = TouchAreaEnum.LEFT_TOP;
            handleGuideAndHandleMode();
            return;
        }
        if (isInRightTopCorner(x, y)) {
            mTouchArea = TouchAreaEnum.RIGHT_TOP;
            handleGuideAndHandleMode();
            return;
        }
        if (isInLeftBottomCorner(x, y)) {
            mTouchArea = TouchAreaEnum.LEFT_BOTTOM;
            handleGuideAndHandleMode();
            return;
        }
        if (isInRightBottomCorner(x, y)) {
            mTouchArea = TouchAreaEnum.RIGHT_BOTTOM;
            handleGuideAndHandleMode();
            return;
        }

        if (isInCenterLeftCorner(x, y)) {
            mTouchArea = TouchAreaEnum.CENTER_LEFT;
            handleGuideAndHandleMode();
            return;
        }
        if (isInCenterTopCorner(x, y)) {
            mTouchArea = TouchAreaEnum.CENTER_TOP;
            handleGuideAndHandleMode();
            return;
        }
        if (isInCenterRightCorner(x, y)) {
            mTouchArea = TouchAreaEnum.CENTER_RIGHT;
            handleGuideAndHandleMode();
            return;
        }
        if (isInCenterBottomCorner(x, y)) {
            mTouchArea = TouchAreaEnum.CENTER_BOTTOM;
            handleGuideAndHandleMode();
            return;
        }

        if (isInFrameCenter(x, y)) {
            if (mGuideShowMode == ShowModeEnum.SHOW_ON_TOUCH)
                mShowGuide = true;
            mTouchArea = TouchAreaEnum.CENTER;
            return;
        }
        // 默认情况
        mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS;
    }

    private boolean isInCenterBottomCorner(float x, float y) {
        float dx = x - (mFrameRectF.left + mFrameRectF.width() / 2);
        float dy = y - mFrameRectF.bottom;
        return isInsideBound(dx, dy);

    }

    private boolean isInCenterRightCorner(float x, float y) {
        float dx = x - mFrameRectF.right;
        float dy = y - (mFrameRectF.top + mFrameRectF.height() / 2);
        return isInsideBound(dx, dy);

    }

    private boolean isInCenterTopCorner(float x, float y) {
        float dx = x - (mFrameRectF.left + mFrameRectF.width() / 2);
        float dy = y - mFrameRectF.top;
        return isInsideBound(dx, dy);
    }

    private boolean isInCenterLeftCorner(float x, float y) {
        float dx = x - mFrameRectF.left;
        float dy = y - (mFrameRectF.top + mFrameRectF.height() / 2);
        return isInsideBound(dx, dy);
    }

    /**
     * 处理指导线和四角线的显示模式
     */
    private void handleGuideAndHandleMode() {
        if (mHandleShowMode == ShowModeEnum.SHOW_ON_TOUCH) {
            mShowHandle = true;
        }
        if (mGuideShowMode == ShowModeEnum.SHOW_ON_TOUCH) {
            mShowGuide = true;
        }
    }

    private boolean isInLeftTopCorner(float x, float y) {
        float dx = x - mFrameRectF.left;
        float dy = y - mFrameRectF.top;
        return isInsideBound(dx, dy);
    }

    private boolean isInRightTopCorner(float x, float y) {
        float dx = x - mFrameRectF.right;
        float dy = y - mFrameRectF.top;
        return isInsideBound(dx, dy);
    }

    private boolean isInLeftBottomCorner(float x, float y) {
        float dx = x - mFrameRectF.left;
        float dy = y - mFrameRectF.bottom;
        return isInsideBound(dx, dy);

    }

    private boolean isInRightBottomCorner(float x, float y) {
        float dx = x - mFrameRectF.right;
        float dy = y - mFrameRectF.bottom;
        return isInsideBound(dx, dy);
    }

    private boolean isInsideBound(float dx, float dy) {
        float d = (float) (Math.pow(dx, 2) + Math.pow(dy, 2));
        return (Math.pow(mHandleSize + mTouchPadding, 2)) >= d;
    }

    private boolean isInFrameCenter(float x, float y) {
        if (mFrameRectF.left <= x && mFrameRectF.right >= x) {
            if (mFrameRectF.top <= y && mFrameRectF.bottom >= y) {
                mTouchArea = TouchAreaEnum.CENTER;
                return true;
            }
        }
        return false;
    }

    /**
     * 限制范围
     *
     * @param val
     * @param min
     * @param max
     * @param defaultVal
     * @return
     */
    private float constrain(float val, float min, float max, float defaultVal) {
        if (val < min || val > max)
            return defaultVal;
        return val;
    }

    /**
     * 获取当前View显示的的bitmap
     *
     * @return
     */
    private Bitmap getBitmap() {
        Bitmap bm = null;
        Drawable d = getDrawable();
        if (d != null && d instanceof BitmapDrawable)
            bm = ((BitmapDrawable) d).getBitmap();
        return bm;
    }

    /**
     * 对图片进行布局
     *
     * @param viewWidth
     * @param viewHeight
     */
    private void doLayout(int viewWidth, int viewHeight) {
        if (viewHeight == 0 || viewWidth == 0) {
            return;
        }
        PointF pointF = new PointF(getPaddingLeft() + viewWidth * 0.5f, getPaddingTop() + viewHeight * 0.5f);
        setCenter(pointF);
        setScale(calcScale(viewWidth, viewHeight, mImgAngle));
        setMatrix();
        RectF rectF = new RectF(0, 0, mImgWidth, mImgHeight);
        mImageRectF = calcImageRect(rectF, mMatrix);

        mFrameRectF = calculateFrameRect(mImageRectF);

        mIsInitialized = true;
        invalidate();
    }

    /**
     * 重设矩阵，平移缩放旋转操作
     */
    private void setMatrix() {
        mMatrix.reset();
        mMatrix.setTranslate(mCenter.x - mImgWidth * 0.5f, mCenter.y - mImgHeight * 0.5f);
        mMatrix.postScale(mCropScale, mCropScale, mCenter.x, mCenter.y);
        if (mRotateSwitch) {
            mMatrix.postRotate(mImgAngle, mCenter.x, mCenter.y);
        }
    }

    private RectF calculateFrameRect(RectF imageRect) {
        float frameW = getRatioX(imageRect.width());
        float frameH = getRatioY(imageRect.height());
        float frameRatio = frameW / frameH;
        float imgRatio = imageRect.width() / imageRect.height();

        float l = imageRect.left, t = imageRect.top, r = imageRect.right, b = imageRect.bottom;
        if (frameRatio >= imgRatio) {
            //宽比长比例大于img图宽高比的情况
            l = imageRect.left;
            r = imageRect.right;
            //图的中点
            float hy = (imageRect.top + imageRect.bottom) * 0.5f;
            //中点到上下顶点坐标的距离
            float hh = (imageRect.width() / frameRatio) * 0.5f;
            t = hy - hh;
            b = hy + hh;
        } else if (frameRatio < imgRatio) {
            //宽比长比例大于img图宽高比的情况
            t = imageRect.top;
            b = imageRect.bottom;
            float hx = (imageRect.left + imageRect.right) * 0.5f;
            float hw = imageRect.height() * frameRatio * 0.5f;
            l = hx - hw;
            r = hx + hw;
        }
        //裁剪框宽度
        float w = r - l;
        //高度
        float h = b - t;
        //中心点
        float cx = l + w / 2;
        float cy = t + h / 2;
        //放大后的裁剪框的宽高
        float sw = w * mInitialFrameScale;
        float sh = h * mInitialFrameScale;
        return new RectF(cx - sw / 2, cy - sh / 2, cx + sw / 2, cy + sh / 2);
    }

    private float getRatioX(float w) {
        switch (mCropMode) {
            case FIT_IMAGE:
                return mImageRectF.width();
            case FREE:
                return w;
            case RATIO_2_3:
                return 2;
            case RATIO_3_2:
                return 3;
            case RATIO_4_3:
                return 4;
            case RATIO_3_4:
                return 3;
            case RATIO_16_9:
                return 16;
            case RATIO_9_16:
                return 9;
            case SQUARE:
                return 1;
            default:
                return w;
        }
    }

    private float getRatioY(float h) {
        switch (mCropMode) {
            case FIT_IMAGE:
                return mImageRectF.height();
            case FREE:
                return h;
            case RATIO_2_3:
                return 3;
            case RATIO_3_2:
                return 2;
            case RATIO_4_3:
                return 3;
            case RATIO_3_4:
                return 4;
            case RATIO_16_9:
                return 9;
            case RATIO_9_16:
                return 16;
            case SQUARE:
                return 1;
            default:
                return h;
        }
    }

    private float getRatioX() {
        switch (mCropMode) {
            case FIT_IMAGE:
                return mImageRectF.width();
            case RATIO_2_3:
                return 2;
            case RATIO_3_2:
                return 3;
            case RATIO_4_3:
                return 4;
            case RATIO_3_4:
                return 3;
            case RATIO_16_9:
                return 16;
            case RATIO_9_16:
                return 9;
            case SQUARE:
                return 1;
            default:
                return 1;
        }
    }

    private float getRatioY() {
        switch (mCropMode) {
            case FIT_IMAGE:
                return mImageRectF.height();
            case RATIO_2_3:
                return 3;
            case RATIO_3_2:
                return 2;
            case RATIO_4_3:
                return 3;
            case RATIO_3_4:
                return 4;
            case RATIO_16_9:
                return 9;
            case RATIO_9_16:
                return 16;
            case SQUARE:
                return 1;
            default:
                return 1;
        }
    }

    /**
     * 将Matrix映射到rect
     *
     * @param rect
     * @param matrix
     * @return
     */
    private RectF calcImageRect(RectF rect, Matrix matrix) {
        RectF applied = new RectF();
        matrix.mapRect(applied, rect);
        return applied;
    }

    private float calcScale(int viewWidth, int viewHeight, float angle) {
        mImgWidth = getDrawable().getIntrinsicWidth();
        mImgHeight = getDrawable().getIntrinsicHeight();
        if (mImgWidth <= 0)
            mImgWidth = viewWidth;
        if (mImgHeight <= 0)
            mImgHeight = viewHeight;
        float viewRatio = (float) viewWidth / (float) viewHeight;
        float imgRatio = getRotatedWidth(angle) / getRotatedHeight(angle);
        float scale = 1.0f;
        if (imgRatio >= viewRatio) {
            scale = viewWidth / getRotatedWidth(angle);
        } else if (imgRatio < viewRatio) {
            scale = viewHeight / getRotatedHeight(angle);
        }
        return scale;
    }

    private float getRotatedWidth(float angle) {
        return getRotatedWidth(angle, mImgWidth, mImgHeight);
    }

    private float getRotatedWidth(float angle, float width, float height) {
        return angle % 180 == 0 ? width : height;
    }

    private float getRotatedHeight(float angle) {
        return getRotatedHeight(angle, mImgWidth, mImgHeight);
    }

    private float getRotatedHeight(float angle, float width, float height) {
        return angle % 180 == 0 ? height : width;
    }

    /**
     * 保存中心矩阵
     *
     * @param center
     */
    private void setCenter(PointF center) {
        mCenter = center;
    }

    /**
     * 保存比例
     *
     * @param scale
     */
    private void setScale(float scale) {
        mCropScale = scale;
    }

    /**
     * 旋转图片
     *
     * @param degrees
     */
    public void rotateImage(RotateDegreesEnum degrees) {
        // 标记有旋转操作
        mIsRotated = true;
        if (mIsReverseY) {
            degrees = reverseDegrees(degrees);
        }
        rotateImage(degrees, mAnimationDurationMillis);
    }

    /**
     * 将翻转方向取反
     *
     * @param degrees
     * @return
     */
    private RotateDegreesEnum reverseDegrees(RotateDegreesEnum degrees) {
        // 处理翻转后的旋转方向改变问题
        switch (degrees) {
            case ROTATE_90D: {
                degrees = RotateDegreesEnum.ROTATE_M90D;
                break;
            }
            case ROTATE_M90D: {
                degrees = RotateDegreesEnum.ROTATE_90D;
                break;
            }
            case ROTATE_180D: {
                degrees = RotateDegreesEnum.ROTATE_M180D;
                break;
            }
            case ROTATE_M180D: {
                degrees = RotateDegreesEnum.ROTATE_180D;
                break;
            }
            case ROTATE_270D: {
                degrees = RotateDegreesEnum.ROTATE_M270D;
                break;
            }
            case ROTATE_M270D: {
                degrees = RotateDegreesEnum.ROTATE_270D;
                break;
            }
        }
        return degrees;
    }

    /**
     * 旋转图片
     *
     * @param degrees
     * @param durationMillis 需要调整的动画时间
     */
    private void rotateImage(RotateDegreesEnum degrees, int durationMillis) {
        if (mIsRotating) {
            mValueAnimator.cancel();
        }

        final float currentAngle = mImgAngle;
        final float newAngle = (mImgAngle + degrees.getValue());
        final float angleDiff = newAngle - currentAngle;
        final float currentScale = mCropScale;
        final float newScale = calcScale(mViewWidth, mViewHeight, newAngle);

        if (mIsAnimationEnabled) {
            final float scaleDiff = newScale - currentScale;

            mValueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mImgAngle = newAngle % 360;
                    mCropScale = newScale;
                    doLayout(mViewWidth, mViewHeight);
                    mIsRotating = false;
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    mIsRotating = true;
                }
            });
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float scale = (float) animation.getAnimatedValue();
                    mImgAngle = currentAngle + angleDiff * scale;
                    mCropScale = currentScale + scaleDiff * scale;
                    setMatrix();
                    invalidate();
                }
            });
            mValueAnimator.setDuration(durationMillis);
            mValueAnimator.start();
        } else {
            mImgAngle = newAngle % 360;
            mCropScale = newScale;
            doLayout(mViewWidth, mViewHeight);
        }
    }

    /**
     * 左右翻转
     */
    public void reverseY() {
        if (!mIsReverseY) {
            mIsReverseY = true;
        } else {
            mIsReverseY = false;
        }
        super.setScaleX(getScaleX() * -1f);
    }

    /**
     * 裁剪图像返回裁剪后的bitmap
     * <p>
     * <Strong>NOTICE:工作在异步线程</Strong>
     *
     * @return
     */
    public void cropImage(OnCropListener onCropListener) {
        addOnCropListener(onCropListener);

        ThreadPoolManagerUtils.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                Bitmap cropped = getCroppedBitmap();
                if (mOnCropListener != null) {
                    mOnCropListener.onCropFinished(cropped);
                }
            }
        });
    }

    /**
     * @return
     */
    public Bitmap getCroppedBitmap() {
        Bitmap source = getBitmap();
        if (source == null)
            return null;
        Bitmap rotated = getRotatedBitmap(source);
        Rect cropRect = calcCropRect(source.getWidth(), source.getHeight());
        Bitmap cropped =
                Bitmap.createBitmap(rotated, cropRect.left, cropRect.top, cropRect.width(), cropRect.height(), null, false);
        if (rotated != cropped && rotated != source) {
            rotated.recycle();
        }
        if (mIsReverseY) {
            // 如果翻转了照片，那么对照片进行翻转处理
            cropped = reverseImg(cropped, -1, 1);
        }
        return cropped;
    }

    private Rect calcCropRect(int originalImageWidth, int originalImageHeight) {
        float rotatedWidth = getRotatedWidth(mImgAngle, originalImageWidth, originalImageHeight);
        float rotatedHeight = getRotatedHeight(mImgAngle, originalImageWidth, originalImageHeight);
        float scaleForOriginal = rotatedWidth / mImageRectF.width();
        float offsetX = mImageRectF.left * scaleForOriginal;
        float offsetY = mImageRectF.top * scaleForOriginal;

        int left = Math.round(mFrameRectF.left * scaleForOriginal - offsetX);
        int top = Math.round(mFrameRectF.top * scaleForOriginal - offsetY);
        int right = Math.round(mFrameRectF.right * scaleForOriginal - offsetX);
        int bottom = Math.round(mFrameRectF.bottom * scaleForOriginal - offsetY);

        int imageW = Math.round(rotatedWidth);
        int imageH = Math.round(rotatedHeight);
        return new Rect(Math.max(left, 0), Math.max(top, 0), Math.min(right, imageW), Math.min(bottom, imageH));
    }

    /**
     * 得到旋转后的Bitmap
     *
     * @param bitmap
     * @return
     */
    private Bitmap getRotatedBitmap(Bitmap bitmap) {
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.postRotate(mImgAngle, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, true);
    }

    private void addOnCropListener(OnCropListener onCropListener) {
        mOnCropListener = onCropListener;
    }

    @UiThread
    public void setCropMode(CropModeEnum mode) {
        setCropMode(mode, mAnimationDurationMillis);
    }

    /**
     * 设置裁剪层的开关
     *
     * @param enabled
     */
    @UiThread
    public void setCropEnabled(boolean enabled) {
        mIsCropEnabled = enabled;
        invalidate();
    }

    private void setCropMode(CropModeEnum mode, int durationMillis) {
        this.mCropMode = mode;
        recalculateFrameRect(durationMillis);
    }

    private void recalculateFrameRect(int durationMillis) {
        if (mImageRectF == null)
            return;
        if (mIsAnimating) {
            mFrameValueAnimator.cancel();
        }
        final RectF currentRect = new RectF(mFrameRectF);
        final RectF newRect = calculateFrameRect(mImageRectF);
        final float diffL = newRect.left - currentRect.left;
        final float diffT = newRect.top - currentRect.top;
        final float diffR = newRect.right - currentRect.right;
        final float diffB = newRect.bottom - currentRect.bottom;
        if (mIsAnimationEnabled) {
            mFrameValueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mFrameRectF = newRect;
                    invalidate();
                    mIsAnimating = false;
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    mIsAnimating = true;
                }
            });
            mFrameValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float scale = (float) animation.getAnimatedValue();
                    // 裁剪框放大的动画RectF
                    mFrameRectF =
                            new RectF(currentRect.left + diffL * scale, currentRect.top + diffT * scale, currentRect.right
                                    + diffR * scale, currentRect.bottom + diffB * scale);
                    invalidate();
                }
            });
            mFrameValueAnimator.setDuration(durationMillis);
            mFrameValueAnimator.start();
        } else {
            mFrameRectF = calculateFrameRect(mImageRectF);
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
        if (mFrameValueAnimator != null) {
            mFrameValueAnimator.cancel();
            mFrameValueAnimator = null;
        }
        if (mOnCropListener != null) {
            mOnCropListener = null;
        }
    }

    /**
     * 用于确认后显示预览图
     *
     * @param bitmap
     */
    public void showBitmap(Bitmap bitmap) {
        if (mIsRotated) {
            mRotateSwitch = false;
        }
        this.setImageBitmap(bitmap);
        // 排除翻转的左右翻转问题
        if (mIsReverseY) {
            // 恢复CropView的ScaleX值
            this.setScaleX(this.getScaleX() * -1f);
        }
    }

    /**
     * 翻转照片到相应的尺寸
     *
     * @param bm
     * @param x  -1时为水平翻转
     * @param y  -1时为垂直翻转
     * @return
     */
    public Bitmap reverseImg(Bitmap bm, int x, int y) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(x, 1);
        matrix.postScale(1, y);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }

    /**
     * 手指点击的区域枚举类
     *
     * @author LiGuangMin
     * @email lgm@meitu.com
     * @time Created by 2018/8/22 19:00
     */
    public enum TouchAreaEnum {
        CENTER, LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, OUT_OF_BOUNDS, CENTER_LEFT, CENTER_TOP, CENTER_RIGHT, CENTER_BOTTOM
    }

    /**
     * 裁剪框的比例模式
     *
     * @author LiGuangMin
     * @email lgm@meitu.com
     * @time Created by 2018/8/22 19:01
     */
    public enum CropModeEnum {
        FIT_IMAGE(0), RATIO_2_3(1), RATIO_3_2(2), RATIO_4_3(3), RATIO_3_4(4), SQUARE(5), RATIO_16_9(6), RATIO_9_16(7), FREE(
                8);
        private final int ID;

        CropModeEnum(int id) {
            ID = id;
        }

        public int getID() {
            return ID;
        }
    }

    /**
     * 旋转角度的枚举类
     *
     * @author LiGuangMin
     * @email lgm@meitu.com
     * @time Created by 2018/8/22 19:07
     */
    public enum RotateDegreesEnum {
        ROTATE_90D(90), ROTATE_180D(180), ROTATE_270D(270), ROTATE_M90D(-90), ROTATE_M180D(-180), ROTATE_M270D(-270), ROTATE_0D(
                0);

        private final int VALUE;

        RotateDegreesEnum(final int value) {
            this.VALUE = value;
        }

        public int getValue() {
            return VALUE;
        }
    }

    /**
     * 展示模式，控制裁剪框显示的时机
     *
     * @author LiGuangMin
     * @email lgm@meitu.com
     * @time Created by 2018/8/22 20:44
     */
    public enum ShowModeEnum {
        SHOW_ALWAYS(1), SHOW_ON_TOUCH(2), NOT_SHOW(3);
        private final int ID;

        ShowModeEnum(final int id) {
            this.ID = id;
        }

        public int getId() {
            return ID;
        }
    }

    public interface OnCropListener {
        void onCropFinished(Bitmap bitmap);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        SavedState ss = (SavedState) state;
        this.mCropMode = ss.mCropMode;
        this.mImgAngle = ss.mImgAngle;
        this.mImgWidth = ss.mImgWidth;
        this.mImgHeight = ss.mImgHeight;
        this.mHandleSize = ss.mHandleSize;
        this.mHandleWidth = ss.mHandleWidth;
        this.mTouchPadding = ss.mTouchPadding;
        this.mFrameMinSize = ss.mFrameMinSize;
        this.mFrameStrokeWeight = ss.mFrameStrokeWeight;
        this.mGuideStrokeWeight = ss.mGuideStrokeWeight;
        this.mBackgroundColor = ss.mBackgroundColor;
        this.mOverlayColor = ss.mOverlayColor;
        this.mFrameColor = ss.mFrameColor;
        this.mHandleColor = ss.mHandleColor;
        this.mGuideColor = ss.mGuideColor;
        this.mCropMode = ss.mCropMode;
        this.mGuideShowMode = ss.mGuideShowMode;
        this.mHandleShowMode = ss.mHandleShowMode;
        this.mInitialFrameScale = ss.mInitialFrameScale;
        this.mIsInitialized = ss.mIsInitialized;
        this.mIsCropEnabled = ss.mIsCropEnabled;
        this.mShowGuide = ss.mShowGuide;
        this.mShowHandle = ss.mShowHandle;
        this.mIsRotating = ss.mIsRotating;
        this.mIsAnimating = ss.mIsAnimating;
        this.mIsReverseY = ss.mIsReverseY;
        this.mIsRotated = ss.mIsRotated;
        this.mRotateSwitch = ss.mRotateSwitch;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superParcelable = super.onSaveInstanceState();
        SavedState ss = new SavedState(superParcelable);
        ss.mCropMode = this.mCropMode;
        ss.mImgAngle = this.mImgAngle;
        ss.mImgWidth = this.mImgWidth;
        ss.mImgHeight = this.mImgHeight;
        ss.mHandleSize = this.mHandleSize;
        ss.mHandleWidth = this.mHandleWidth;
        ss.mTouchPadding = this.mTouchPadding;
        ss.mFrameMinSize = this.mFrameMinSize;
        ss.mFrameStrokeWeight = this.mFrameStrokeWeight;
        ss.mGuideStrokeWeight = this.mGuideStrokeWeight;
        ss.mBackgroundColor = this.mBackgroundColor;
        ss.mOverlayColor = this.mOverlayColor;
        ss.mFrameColor = this.mFrameColor;
        ss.mHandleColor = this.mHandleColor;
        ss.mGuideColor = this.mGuideColor;
        ss.mCropMode = this.mCropMode;
        ss.mGuideShowMode = this.mGuideShowMode;
        ss.mHandleShowMode = this.mHandleShowMode;
        ss.mInitialFrameScale = this.mInitialFrameScale;
        ss.mIsInitialized = this.mIsInitialized;
        ss.mIsCropEnabled = this.mIsCropEnabled;
        ss.mShowGuide = this.mShowGuide;
        ss.mShowHandle = this.mShowHandle;
        ss.mIsRotating = this.mIsRotating;
        ss.mIsAnimating = this.mIsAnimating;
        ss.mIsReverseY = this.mIsReverseY;
        ss.mIsRotated = this.mIsRotated;
        ss.mRotateSwitch = this.mRotateSwitch;
        return ss;
    }

    public static class SavedState extends BaseSavedState {
        // 放大比例
        private float mCropScale = 1.0f;
        // 旋转角度
        private float mImgAngle = 0.0f;
        // 图片宽度
        private float mImgWidth = 0.0f;
        // 图片高度
        private float mImgHeight = 0.0f;
        // 四角线的长度值
        private int mHandleSize;
        // 四角的线宽度
        private int mHandleWidth;
        // 触摸可处理的宽度值
        private int mTouchPadding = 0;
        // 裁剪框最小值
        private int mFrameMinSize;
        // 裁剪外框线框宽度
        private float mFrameStrokeWeight = 2f;
        // 裁剪指导线宽度
        private float mGuideStrokeWeight = 2f;

        // 背景的颜色
        private int mBackgroundColor;
        // 半透明框的颜色
        private int mOverlayColor;
        // 外框的颜色
        private int mFrameColor;
        // 四边角点的颜色
        private int mHandleColor;
        // 指导线颜色
        private int mGuideColor;
        // 裁剪模式
        private CropModeEnum mCropMode;
        // 指导线的显示模式
        private ShowModeEnum mGuideShowMode;
        // 处理四个角的显示模式
        private ShowModeEnum mHandleShowMode;
        // 初始化的比例大小0.01到1.0，默认情况为0.75
        private float mInitialFrameScale;
        // 是否初始化
        private boolean mIsInitialized;
        // 是否开始裁剪
        private boolean mIsCropEnabled = true;
        // 是否显示指导线
        private boolean mShowGuide = true;
        // 是否显示处理线
        private boolean mShowHandle = true;

        private boolean mIsRotating = false;
        private boolean mIsAnimating = false;

        // 是否已经翻转了
        private boolean mIsReverseY = false;
        // 是否有旋转操作
        private boolean mIsRotated = false;
        // 旋转操作的开关
        private boolean mRotateSwitch = true;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel source) {
            super(source);
            mGuideColor = source.readInt();
            mHandleColor = source.readInt();
            mFrameColor = source.readInt();
            mOverlayColor = source.readInt();
            mBackgroundColor = source.readInt();
            mFrameMinSize = source.readInt();
            mTouchPadding = source.readInt();
            mHandleWidth = source.readInt();
            mHandleSize = source.readInt();
            mImgHeight = source.readFloat();
            mImgWidth = source.readFloat();
            mImgAngle = source.readFloat();
            mCropScale = source.readFloat();
            mFrameStrokeWeight = source.readFloat();
            mGuideStrokeWeight = source.readFloat();
            mHandleShowMode = (ShowModeEnum) source.readSerializable();
            mGuideShowMode = (ShowModeEnum) source.readSerializable();
            mCropMode = (CropModeEnum) source.readSerializable();
            mInitialFrameScale = source.readFloat();
            mRotateSwitch = (source.readInt() != 0);
            mIsRotated = (source.readInt() != 0);
            mIsReverseY = (source.readInt() != 0);
            mIsAnimating = (source.readInt() != 0);
            mIsRotating = (source.readInt() != 0);
            mShowHandle = (source.readInt() != 0);
            mShowGuide = (source.readInt() != 0);
            mIsCropEnabled = (source.readInt() != 0);
            mIsInitialized = (source.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mGuideColor);
            out.writeInt(mHandleColor);
            out.writeInt(mFrameColor);
            out.writeInt(mOverlayColor);
            out.writeInt(mBackgroundColor);
            out.writeInt(mFrameMinSize);
            out.writeInt(mTouchPadding);
            out.writeInt(mHandleWidth);
            out.writeInt(mHandleSize);
            out.writeFloat(mImgHeight);
            out.writeFloat(mImgWidth);
            out.writeFloat(mImgAngle);
            out.writeFloat(mCropScale);
            out.writeFloat(mFrameStrokeWeight);
            out.writeFloat(mGuideStrokeWeight);
            out.writeSerializable(mHandleShowMode);
            out.writeSerializable(mGuideShowMode);
            out.writeSerializable(mCropMode);
            out.writeFloat(mInitialFrameScale);
            out.writeInt(mRotateSwitch ? 1 : 0);
            out.writeInt(mIsRotated ? 1 : 0);
            out.writeInt(mIsReverseY ? 1 : 0);
            out.writeInt(mIsAnimating ? 1 : 0);
            out.writeInt(mIsRotating ? 1 : 0);
            out.writeInt(mShowHandle ? 1 : 0);
            out.writeInt(mShowGuide ? 1 : 0);
            out.writeInt(mIsCropEnabled ? 1 : 0);
            out.writeInt(mIsInitialized ? 1 : 0);
        }

        public static final Creator CREATOR = new Creator() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
    }
}
