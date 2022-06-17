# CropView从入门到放弃

本篇我会带你去从零设计一款裁剪旋转的View

# 2022 年有感

本篇是 2018 年写的，现在重新 review 了下，思路是好的，就是代码写的不够优雅，太不优雅了，各种重复代码，各位读者朋友们，不要照抄代码噢，重复代码应该封装，有些可以放到枚举里简化取值操作，并且绘制逻辑里不要重复 new 对象，会影响性能喔，共勉！！！

---

# 你需要准备的

裁剪的View最关键的是裁剪框的绘制和手势的调整，另外还有最核心的裁剪功能就是调用方法createBitmap去裁剪得到目标图片。

```
//图片裁剪的核心功能
Bitmap.createBitmap(originalBitmap,//原图
                 cropX,//图片裁剪横坐标开始位置
                 cropY,//图片裁剪纵坐标开始位置
                 cropWidth,//要裁剪的宽度
                 cropHeight);//要裁剪的高度
```

- 1、裁剪框的绘制无非就是一个透明的方形和灰色透明度的方形之外的区域（如下图红色箭头所指区域）、裁剪框的绘制关键的地方在于裁剪的Rect区域的确定

![示意图](https://upload-images.jianshu.io/upload_images/3515789-63ae083d55d28b8a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


- 2、手势调整裁剪框的边角、四周边。裁剪控件可以拖动的边界为上图的紫色箭头区域，**并且有一个点击所在点区域的周边也可以正常去控制边界的拖动，说明有一个动态的范围**

# 目录

为了能正常的显示图片的缩放比例和旋转、居中等，可以考虑自己控制Bitmap的各项绘制参数。

##### 一、测量
主要是Padding值的处理

##### 二、布局

- 1、Bitmap中心点的确定
- 2、图片比例的确定
- 3、图片Bitmap参数的确定（比如旋转。放大）
- 4、图片显示区域的确定
- 5、裁剪框区域的确定

##### 三、绘制

- 1、画背景
- 2、画Bitmap
- 3、画裁剪框
- 4、画边界线
- 5、画指导线
- 6、画定制四边角的图案


##### 四、添加MotionEvent控制

- 1、按下时计算点击的区域位置
- 2、根据第1步点击的区域去判断需不需要对边界进行Move处理
- 3、抬起时恢复默认的处理状态

##### 五、旋转翻转处理

- 1、左右翻转
- 2、旋转处理

##### 六、裁剪

- 1、对获取的bitmap进行旋转处理
- 2、计算裁剪的原图
- 3、对图片进行翻转处理
- 4、拿到裁剪后的图

##### 七、开放属性Style

##### 八、使用

------
# 额外需要首先考虑的

首先思考我们的控件大概需要什么动态的属性，为了以后方便扩展业务或者功能的精确控制

- 1、裁剪框有一个最小的矩形大小，说明要限制裁剪框的长度宽度，一般为正方形（本例中设计为了正方形，当然解析到该块内容时我会顺便说下如何实现长宽不一样的裁剪框）。此处需要属性为**裁剪框最小边长**
- 2、裁剪框外围除了画Bitmap之外，会存在除了Bitmap之外的空白区域，这里希望能控制他们的颜色。此处需要属性为**窗口背景颜色**
- 3、由于我们要自定义Bitmap区域的绘制，类似ImageView控件，我们需要src属性来引入图片内容。此处需要属性为**src**
- 4、上面讲过，手势点击边界或者角时，点击所在点区域的周边也可以正常去控制边界的拖动，说明有一个动态的范围。这个范围也希望是可以控制的。此处需要属性为**触摸边界的大小范围**
- 5、颜色的比如有：**裁剪内框的颜色**，**裁剪外框的颜色**，**边界线的颜色**，**裁剪框指导线的颜色**，**四周边角自定义点的图案的颜色**
- 6、尺寸的比如：**边框线的宽度**，**指导线的宽度**，**四周边角线宽度**、**长度**（或者圆圈的半径）
- 7、**图片初始化的比例**
- 8、根据默认打开裁剪控件时的状态确定开关类属性：**裁剪框的是否绘制**
- 9、如果说有时候指导线是想要点击才拖动裁剪框才显示，那么还要添加**指导线的绘制开关**
- 10、同理，**四边角自定义图案也需要开关**
- 11、如果你想的够多，那么这些边角图案有时候可能还会加入类似button的点击之后放大图案的需求，那么这个时候你还需要添加一个开关用来控制要不要开启边角动画（逃）

![](https://upload-images.jianshu.io/upload_images/3515789-740e81f79d8e8e9f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

----

![](https://upload-images.jianshu.io/upload_images/3515789-abb7f50c6abc211f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



----

## 一、测量

测量View的大小，主要是处理Padding的值，甚至某些情况要专门对wrap_content属性做专门的处理。对Padding进行处理如下
```
 @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(viewWidth, viewHeight);
        mViewWidth = viewWidth - getPaddingLeft() - getPaddingRight();
        mViewHeight = viewHeight - getPaddingTop() - getPaddingBottom();
    }
```
对左右Padding进行了处理，最终得到的是CropView内容的区域的实际宽高

----

## 二、布局

这一部分只要是对Bitmap中心点、图片比例、裁剪框区域的确定，布局代码如下，为了提高性能，这里只有在图片已设置到View之后```getDrawable() ```有值的时候才进行布局，
```
@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getDrawable() != null) {
            doLayout(mViewWidth, mViewHeight);
        }
    }
```

```
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
        //计算中心点
        PointF pointF = new PointF(getPaddingLeft() + viewWidth * 0.5f, getPaddingTop() + viewHeight * 0.5f);
        //保存中心点
        setCenter(pointF);
        //计算放大比例和保存比例
        setScale(calcScale(viewWidth, viewHeight, mImgAngle));
        //Bitmap的举证变换
        setMatrix();
        RectF rectF = new RectF(0, 0, mImgWidth, mImgHeight);
        //图片Bitmap的实际区域
        mImageRectF = calcImageRect(rectF, mMatrix);
		//计算裁剪框的Rect区域
        mFrameRectF = calculateFrameRect(mImageRectF);
		//标记为初始化完成
        mIsInitialized = true;
        invalidate();
    }
```

##### 1、Bitmap中心点的确定
这个没什么好说的，就是View除了Padding区域外x，y取中点

##### 2、图片比例的确定
这里开始会用到一些变量（或者是属性）

```
// 旋转角度
private float mImgAngle = 0.0f;
 // 图片宽度
private float mImgWidth = 0.0f;
// 图片高度
private float mImgHeight = 0.0f;
// 放大比例
private float mCropScale = 1.0f;

```

计算比例的代码如下，其实就是图片的宽高之比

```
private float calcScale(int viewWidth, int viewHeight, float angle) {
        mImgWidth = getDrawable().getIntrinsicWidth();
        mImgHeight = getDrawable().getIntrinsicHeight();
        if (mImgWidth <= 0)
            mImgWidth = viewWidth;
        if (mImgHeight <= 0)
            mImgHeight = viewHeight;
        float viewRatio = (float) viewWidth / (float) viewHeight;
        //旋转的情况
        float imgRatio = getRotatedWidth(angle) / getRotatedHeight(angle);
        float scale = 1.0f;
        if (imgRatio >= viewRatio) {
            scale = viewWidth / getRotatedWidth(angle);
        } else if (imgRatio < viewRatio) {
            scale = viewHeight / getRotatedHeight(angle);
        }
        return scale;
    }
    
```
首先拿到Drawable的固有宽宽高，接着对图片的宽高mImgWidth、mImgHeight进行边界控制，然后计算出图片的长宽比例，要是有旋转的情况那么viewRatio的值要拿到旋转后的宽高来判断，这里关键就是怎么样拿到旋转后的宽高，```getRotatedWidth```方法如下，传入旋转的角度，如果是旋转了180度，那么原来旋转前的宽也是旋转后的宽。如果旋转了90或者270，那么旋转后的宽是原来的高度。同样```getRotatedHeight```方法原理也是一样的。

```
private float getRotatedWidth(float angle) {
        return getRotatedWidth(angle, mImgWidth, mImgHeight);
    }
    
private float getRotatedWidth(float angle, float width, float height) {
        return angle % 180 == 0 ? width : height;
    }
```

最后会把放大当前的图片比例保存下来
```
	/**
     * 保存比例
     *
     * @param scale
     */
    private void setScale(float scale) {
        mCropScale = scale;
    }
```

##### 3、图片Bitmap参数的确定（比如旋转。放大）
接下来会对图片的平移或者放大比例进行调整

用到属性如下

```
// 图形的矩阵类
private Matrix mMatrix = null;
// 居中中点的矩型区域
private PointF mCenter = new PointF();
```
并且矩阵类我们会在构造方法里进行初始化
```
 mMatrix = new Matrix();
```
一张Bitmap图片加载到View默认情况下一般是这样
![](https://upload-images.jianshu.io/upload_images/3515789-40995be52e7605eb.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
那我们需要做的是将它平移到中点，并且比例放大到我们想要的比例

所以，Matrix变化如下，先对其做平移操作，平移距离是Bitmap显示区域中点到屏幕中点的。放大是以屏幕中心点为放大原点，然后X，Y进行整比例的放大，后面考虑到我们要用到旋转操作，也加了旋转的Matrix操作。
```
 	/**
     * 重设矩阵，平移缩放旋转操作
     */
    private void setMatrix() {
        mMatrix.reset();
        mMatrix.setTranslate(mCenter.x - mImgWidth * 0.5f, mCenter.y - mImgHeight * 0.5f);
        mMatrix.postScale(mCropScale, mCropScale, mCenter.x, mCenter.y);
        mMatrix.postRotate(mImgAngle, mCenter.x, mCenter.y);
        
    }
```
##### 4、图片显示区域的确定
接下来会用到很多关于裁剪框的区域和图片显示区域的Rect变量，我们可以一次性全部定义了它们
![](https://upload-images.jianshu.io/upload_images/3515789-54414ba18edeb6d6.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

```
    // 图形的矩阵类
    private Matrix mMatrix = null;
    // 图片的rectf区域
    private RectF mImageRectF;
    // 裁剪框的RectF
    private RectF mFrameRectF;
```

```
	RectF rectF = new RectF(0, 0, mImgWidth, mImgHeight);
        mImageRectF = calcImageRect(rectF, mMatrix);
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
```
根据Img长宽初始化一个新的Rect区域，然后将变化后的Matrix应用到Rect

##### 5、裁剪框区域的确定

裁剪框的绘制会涉及到多种比例的变化，因此这部分会是我们最复杂的一块，首先我们先定义裁剪的模式，一般情况下，裁剪模式分为充满View四周不能控制型、自由控制型、比例型三种。

定义一个枚举类来标记裁剪模式，这里比例型有7种，其实定义多少都可以，什么比例都可以，最为关键是需要在拿到比例后进行通用的处理

```
	/**
     * 裁剪框的比例模式
     *
     * 
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
```

需要定义变量
```
// 裁剪模式，默认比例是自由模式
private CropModeEnum mCropMode = CropModeEnum.FREE;
```

计算裁剪框的代码如下：

```
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

```

其中计算比例的```getRatioX()```如下，通过Switch当前的裁剪模式，返回比例或者当前的图片宽度，```getRatioY()```原理是也是一样的

```
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
```


接着我们通过分别计算获取裁剪框后的比例和原drawable图的比例，拿到他们的比例为了判断裁剪的框要处于图片框中的什么位置

```
		//获取裁剪模式后计算的比例
		float frameRatio = frameW / frameH;
		//图片原始比例
        float imgRatio = imageRect.width() / imageRect.height();
```

现在我们要做的是，计算出裁剪框的坐标位置，裁剪框的位置可能性有很多，我们可以假设一种来看一下
![](https://upload-images.jianshu.io/upload_images/3515789-fe64da17871cfd53.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

假设我们现在的图为1.5:1的图，如上图的1图。现在裁剪框的设定比例是2:1，那么正常比例下裁剪框是无法充满img的，需要对裁剪框做缩放然后放入img框中，如上图的3图。
根据这种情况，我们要对裁剪框做Rect坐标的定位处理。

```
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
```
首先定义裁剪框的四个边角参数左上角left、top，右下角right、bottom坐标分别为l，t，r，b。
frameRatio >= imgRatio的情况其实就是上面的示意图的情况，左右left和right等同于原img图，top/bottom可以根据计算得到的裁剪框的高度```imageRect.width() / frameRatio```去确认。

	宽度/高度=frameRatio


最后计算得到top和bottom的坐标

```
 			t = hy - hh;
            b = hy + hh;
```

同理，frameRatio < imgRatio的情况原理和这个类似。

最后根据这些参数确认实际的裁剪框的区域

```
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
```

```mInitialFrameScale```这个是初始状态下给裁剪框的默认放大比例参数，0-1，设置为1，则充满img的边界，设置为其他值，比如0.2，那会以图片中心点为基准进行缩小。

----

## 三、绘制

以上全部准备工作做完之后，就可以开始绘制我们最关键的裁剪框了

```
 @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(mBackgroundColor);
		//只有初始化完毕了才绘制
        if (mIsInitialized) {
            setMatrix();//这里一开始绘制裁剪框是没有想到的，后面刷新视图的时候，设置了参数之后需要计算bitmap的情况，需要重新对Bitmap进行矩阵处理
            Bitmap bitmap = getBitmap();
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, mMatrix, mBitmapPaint);
                // 画裁剪框
                drawCropFrame(canvas);
            }
        }
    }
```

##### 1、画背景
画ImageView外围的背景（图片显示之外会存在的空白部分）
```
canvas.drawColor(mBackgroundColor);
```
##### 2、画Bitmap

```
canvas.drawBitmap(bitmap, mMatrix, mBitmapPaint);
```
将bitmap画到已确定位置的mMatrix矩阵中

##### 3、画裁剪框

```
private void drawCropFrame(Canvas canvas) {
        drawOverlay(canvas);
        drawFrame(canvas);
        drawGuidelines(canvas);
        drawHandleLines(canvas);    
   }
```
接下来裁剪框会有覆盖层，透明层，边界线和指导线的绘制

```
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
		
        path.addRect(overlayRectF, Path.Direction.CW);
        path.addRect(mFrameRectF, Path.Direction.CCW);
        canvas.drawPath(path, mTranslucentPaint);
    }
```
第二部分拿到了图片的Rect区域，我们这里画半透明覆盖层只要调用路径来处理，这里我们调用了路径了两种不同的方向来添加，以使最终得到的路径是裁剪框之外。（当然这里也可以用路径合成方式的处理）

```		
		path.addRect(mFrameRectF, Path.Direction.CW);
        path.addRect(overlayRectF, Path.Direction.CCW);
```

##### 4、画边界线

属性变量

```
	// 裁剪外框线框宽度
    private float mFrameStrokeWeight = 2f;
    // 外框的颜色
    private int mFrameColor;
```
直接拿到FrameRect边界来绘制边界线
```
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
```

##### 5、画指导线

这里同理，只需要拿到裁剪框边界，计算索要的点的位置的点的坐标即可绘制自己想要的线条

```
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
```

##### 6、画定制四边角的图案

这里我是在四个角画了线条，由于自由模式有点特殊，自由模式下四条边也可以拉动，所以需要给四个边中间点也绘制图案

变量如下
```
	// 四角线的长度值
    private int mHandleSize;
    // 四角的线宽度
    private int mHandleWidth;
```
主要就是坐标的计算
```
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
```

----

## 四、添加MotionEvent控制

手指触摸区域会有10种情况，除了四个边角和边界之外，还有裁剪框中间和裁剪框外部的情况，这里定义一个枚举类来标记这10种情况

```
	/**
     * 手指点击的区域枚举类
     *
     * @time Created by 2018/8/22 19:00
     */
    public enum TouchAreaEnum {
        CENTER, LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, OUT_OF_BOUNDS, CENTER_LEFT, CENTER_TOP, CENTER_RIGHT, CENTER_BOTTOM
    }
```

定义默认的触摸模式

```
	// 触摸的情况
    private TouchAreaEnum mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS;
```
接下来我们需要在Down事件里记录处理的边界，Move事件里动态判断边界是否需要移动，其他事件里恢复初始的状态

```
 @Override
    public boolean onTouchEvent(MotionEvent event) {
        
      	switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                onActionDown(event);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                onActionMove(event);
            
                if (mTouchArea != TouchAreaEnum.OUT_OF_BOUNDS) {
                    // 阻止父view拦截事件
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
```
##### 1、按下时计算点击的区域位置

```
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
```

这里的关键在于```handleTouchArea()```方法中，情况比较多，但是原理相同，这里只分析一种情况

```
	if (isInLeftTopCorner(x, y)) {
            mTouchArea = TouchAreaEnum.LEFT_TOP;
     
            return;
        }
        
        //判断是否在左上角的边界内
     private boolean isInLeftTopCorner(float x, float y) {
     	float dx = x - mFrameRectF.left;
     	float dy = y - mFrameRectF.top;
     	return isInsideBound(dx, dy);
    }
    
    private boolean isInsideBound(float dx, float dy) {
        float d = (float) (Math.pow(dx, 2) + Math.pow(dy, 2));
        return (Math.pow(mHandleSize + mTouchPadding, 2)) >= d;
    }
```
这里处理的其实就是判断触摸的点是否在边界的有效范围内，比如现在的点是左上角，那么可触摸区域应该是围绕左上角顶点的四周圆形的一块区域。如下图，应该是黄色的一块圆形区域。```isInsideBound（）```做的就是对边界内外的判断。
![](https://upload-images.jianshu.io/upload_images/3515789-c697fb8351b24a72.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



##### 2、根据第1步点击的区域去判断需不需要对边界进行Move处理

边界移动也存在很多种情况。总体上可以分为裁剪框整体移动和裁剪框边界移动。

```
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
```

- A.针对裁剪框移动进行分析，move事件得到的X,Y只需要交给裁剪边框的RectF处理就大吉大利了，```handleMoveBounds（）```是平移的相反的操作，这种策略可避免产生由于Move数值精度不准确产生的边界问题）

```

private void moveFrame(float x, float y) {
        // 1.先平移（这里采取先平移如果条件不满足再后退的策略，避免产生由于Move数值精度不准确产生的边界问题）
        mFrameRectF.left += x;
        mFrameRectF.right += x;
        mFrameRectF.top += y;
        mFrameRectF.bottom += y;
        // 2.判断有没有超出界外，如果超出则后退
        handleMoveBounds();
    }
```

- B、针对边框的边界进行处理，处理边框会有2种情况，一种是自由模式下，长宽都可以分别调整，另外一种是确定比例的模式下，长宽需要一起调整的情况

```
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
                // todo 裁剪框比例控制
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRectF.top -= offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.top -= offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRectF.left -= offsetX;
            }

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
```

- A、对自由模式下的边界拖动进行处理

```
		if (mCropMode == CropModeEnum.FREE) {
			//不管发生什么事，先把过来的数值先加上
            mFrameRectF.left += diffX;
            mFrameRectF.top += diffY;
            //判断变化后的边界有没有比限定的最小边界小，如果小，那么就还原
            if (isWidthTooSmall()) {
                float offsetX = mFrameMinSize - mFrameRectF.width();
                mFrameRectF.left -= offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mFrameMinSize - mFrameRectF.height();
                mFrameRectF.top -= offsetY;
            }
            //处理有没有超出最大边界
            checkScaleBounds();
        }
```

- B、对非自由模式下的边界的缩放就行处理
```
			//这里只需要监测move事件的x或者y的变化，然后通过设定的比例计算出另外的x或者y的坐标，从而限定裁剪框的比例
			float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            
            mFrameRectF.left += dx;
            mFrameRectF.top += dy;
            // 控制缩放边界
            if (isWidthTooSmall()) {
            	//同上自由模式的控制，这里也是要讲超界的值恢复，只是这里x，y要分别恢复
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
```

##### 3、抬起时恢复默认的处理状态

其它点击事件，我们只要恢复默认状态就可

```
 	private void onActionCancel() {
        mTouchArea = TouchAreaEnum.OUT_OF_BOUNDS;
        invalidate();
    }
```

## 旋转翻转处理

##### 1、左右翻转

左右翻转和上下翻转只需要用View默认的ScaleX和ScaleY来控制就可以，比如左右翻转

```
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
```

##### 2、旋转处理

旋转多少度是个问题，这就跟晚饭吃什么是个问题如此。

![](https://upload-images.jianshu.io/upload_images/3515789-df9d635fbb007f41.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这里定义了关于旋转角度的枚举类：

```
/**
     * 旋转角度的枚举类
     *
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
```

还记得一开始使用的Metrix矩阵吗，我们可以通过它来实现对当前Img的旋转，先计算旋转后的比例和角度变化，然后只要将变化值和起始值终点值放入属性动画中进行驱动就会有旋转时候的动画效果。当然，如果不需要动画（比如下面代码的最后），你可以直接设置终值，然后重新layout

```
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
		//先计算旋转后的比例和角度变化
        final float currentAngle = mImgAngle;
        //新的角度
        final float newAngle = (mImgAngle + degrees.getValue());
        final float angleDiff = newAngle - currentAngle;
        final float currentScale = mCropScale;
        //旋转后的比例
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
                	//动画关键参数变化
                    float scale = (float) animation.getAnimatedValue();
                    mImgAngle = currentAngle + angleDiff * scale;
                    mCropScale = currentScale + scaleDiff * scale;
                    //核心旋转缩放处理
                    setMatrix();
                    invalidate();
                }
            });
            mValueAnimator.setDuration(durationMillis);
            mValueAnimator.start();
        } else {
        	//无动画的情况
            mImgAngle = newAngle % 360;
            mCropScale = newScale;
            doLayout(mViewWidth, mViewHeight);
        }
    }
```

----

## 六、裁剪

裁剪功能其实最关键的是获取裁剪后的Bitmap，**注意要在工作者线程处理**
```
	/**
     * 获取裁剪后的图片（<Strong>Notice：不包含翻转变化</Strong>）
     *
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
            cropped = ImgUtils.reverseImg(cropped, -1, 1);
        }
        return cropped;
    }

```

##### 1、对获取的bitmap进行旋转处理

其实就是根据现在的旋转角度对Bitmap就行Matrix的处理
```
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

```
##### 2、计算裁剪框相对原图的区域
计算原图进行旋转处理后相对原图的宽高比例，然后通过变化得到裁剪框的旋转后的比例

```

	private Rect calcCropRect(int originalImageWidth, int originalImageHeight) {
		//旋转之后的宽
        float rotatedWidth = getRotatedWidth(mImgAngle, originalImageWidth, originalImageHeight);
        //旋转之后的高
        float rotatedHeight = getRotatedHeight(mImgAngle, originalImageWidth, originalImageHeight);
        //旋转后的相对旋转前的比例
        float scaleForOriginal = rotatedWidth / mImageRectF.width();
        float offsetX = mImageRectF.left * scaleForOriginal;
        float offsetY = mImageRectF.top * scaleForOriginal;

		//对旋转后的裁剪框的处理，
        int left = Math.round(mFrameRectF.left * scaleForOriginal - offsetX);
        int top = Math.round(mFrameRectF.top * scaleForOriginal - offsetY);
        int right = Math.round(mFrameRectF.right * scaleForOriginal - offsetX);
        int bottom = Math.round(mFrameRectF.bottom * scaleForOriginal - offsetY);

        int imageW = Math.round(rotatedWidth);
        int imageH = Math.round(rotatedHeight);
        return new Rect(Math.max(left, 0), Math.max(top, 0), Math.min(right, imageW), Math.min(bottom, imageH));
    }
```

##### 3、对图片进行翻转处理
```
if (mIsReverseY) {
            // 如果翻转了照片，那么对照片进行翻转处理
            cropped = ImgUtils.reverseImg(cropped, -1, 1);
        }
```

##### 4、拿到裁剪后的图

```
 Bitmap cropped =
            Bitmap.createBitmap(rotated, cropRect.left, cropRect.top, cropRect.width(), cropRect.height(), null, false);
```

## 七、开放属性Style

为了能在xml中自定义我们想要的属性，针对开头所说的额外考虑的部分和实际coding时所遇到的一些属性进行整理

得到属性

```
<declare-styleable name="CropView">
        <attr name="img_src" format="reference" />
        <attr name="crop_mode">
            <enum name="fit_image" value="0" />
            <enum name="ratio_2_3" value="1" />
            <enum name="ratio_3_2" value="2" />
            <enum name="ratio_4_3" value="3" />
            <enum name="ratio_3_4" value="4" />
            <enum name="square" value="5" />
            <enum name="ratio_16_9" value="6" />
            <enum name="ratio_9_16" value="7" />
            <enum name="free" value="8" />

        </attr>
        <attr name="background_color" format="reference|color" />
        <attr name="overlay_color" format="reference|color" />
        <attr name="frame_color" format="reference|color" />
        <attr name="handle_color" format="reference|color" />
        <attr name="handle_width" format="dimension" />
        <attr name="handle_size" format="dimension" />
        <attr name="guide_color" format="reference|color" />
        <attr name="guide_show_mode">
            <enum name="show_always" value="1" />
            <enum name="show_on_touch" value="2" />
            <enum name="not_show" value="3" />
        </attr>
        <attr name="handle_show_mode">
            <enum name="show_always" value="1" />
            <enum name="show_on_touch" value="2" />
            <enum name="not_show" value="3" />
        </attr>
        <attr name="touch_padding" format="dimension" />
        <attr name="min_frame_size" format="dimension" />
        <attr name="frame_stroke_weight" format="dimension" />
        <attr name="guide_stroke_weight" format="dimension" />
        <attr name="crop_enabled" format="boolean" />
        <attr name="initial_frame_scale" format="float" />
    </declare-styleable>
```


加载属性

```
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
```

---

## 八、使用

使用第七节的属性像ImageView引入即可

```
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <com.minminaya.crop.CropView
        android:id="@+id/crop_img"
        android:layout_width="match_parent"
        android:layout_height="583dp"
        android:layout_above="@+id/rv"
        android:layout_alignParentTop="true"
        android:layout_centerHorizontal="true"
        android:padding="10dp"
        app:crop_enabled="true"
        app:crop_mode="free"
        app:background_color="#66FFFFFF"
        app:frame_color="@android:color/white"
        app:frame_stroke_weight="2dp"
        app:guide_color="#66FFFFFF"
        app:guide_show_mode="show_always"
        app:guide_stroke_weight="2dp"
        app:handle_color="@android:color/white"
        app:handle_show_mode="show_always"
        app:handle_size="24dp"
        app:handle_width="3dp"
        app:initial_frame_scale="1"
        app:min_frame_size="100dp"
        app:overlay_color="#AA1C1C1C"
        app:touch_padding="8dp" />

    <android.support.v7.widget.RecyclerView
        android:id="@+id/rv"
        android:layout_width="match_parent"
        android:layout_height="108dp"
        android:layout_alignParentBottom="true"
        android:background="@android:color/white" />

</RelativeLayout>
```

然后Activity中引入

```
public class MainActivity extends AppCompatActivity {

    private CropView mCropView;
    private RecyclerView mRecyclerView;
    private CropRecyclerViewAdapter mRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.rv);
        mCropView = findViewById(R.id.crop_img);
        mRecyclerViewAdapter = new CropRecyclerViewAdapter();

        mCropView.setImageResource(R.mipmap.test_pic);

        if (mRecyclerView != null) {
            mRecyclerViewAdapter = new CropRecyclerViewAdapter();
            // 数据来源
            mRecyclerViewAdapter.setCommonAdapterBean(handleRvAdapterData());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
            mRecyclerViewAdapter.setOnItemClickedListener(new CropRecyclerViewAdapter.OnItemClickedListener() {
                @Override
                public void onClicked(View view, int position) {
                    handleRvItemClicked(view, position);
                }
            });
        }
    }

    private CommonAdapterBean handleRvAdapterData() {
        CommonAdapterBean CommonAdapterBean = new CommonAdapterBean();
        List<Integer> funcPics = CommonAdapterBean.getFuncPics();
        List<String> funcNames = CommonAdapterBean.getFuncNames();
        String[] funcNameArrays =
                new String[] {Constant.CropBean.STR_ROTATION, Constant.CropBean.STR_REVERSION,
                        Constant.CropBean.STR_RATIO_FREE, Constant.CropBean.STR_RATIO_SQUARE,
                        Constant.CropBean.STR_RATIO_2_3, Constant.CropBean.STR_RATIO_3_2,
                        Constant.CropBean.STR_RATIO_3_4, Constant.CropBean.STR_RATIO_4_3,
                        Constant.CropBean.STR_RATIO_9_16, Constant.CropBean.STR_RATIO_16_9};
        for (String funcName : funcNameArrays) {
            funcPics.add(R.mipmap.ic_launcher);
            funcNames.add(funcName);
        }
        return CommonAdapterBean;
    }

    protected void handleRvItemClicked(View view, int position) {
        switch (position) {
            case Constant.CropBean.INDEX_ROTATION: {
                mCropView.rotateImage(CropView.RotateDegreesEnum.ROTATE_M90D);
                break;
            }
            case Constant.CropBean.INDEX_REVERSION: {
                mCropView.reverseY();
                break;
            }
            case Constant.CropBean.INDEX_RATIO_FREE: {
                mCropView.setCropMode(CropView.CropModeEnum.FREE);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_SQUARE: {
                mCropView.setCropMode(CropView.CropModeEnum.SQUARE);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_2_3: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_2_3);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_3_2: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_3_2);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_3_4: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_3_4);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_4_3: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_4_3);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_9_16: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_9_16);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_16_9: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_16_9);
                break;
            }
        }
    }
}
```

效果如下

![](https://upload-images.jianshu.io/upload_images/3515789-ed294a65201cd8cf.gif?imageMogr2/auto-orient/strip)

---

![](https://upload-images.jianshu.io/upload_images/3515789-0aa70fedd860fe33.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


---
##### 源代码

地址：https://github.com/minminaya/CropViewDemo


---

参考

[Android自定义 view之图片裁剪从设计到实现](https://blog.csdn.net/chunqiuwei/article/details/78858192/)
