# ZoomImageView
<br>
自定义ImageView控件，以实现对相册图片的手势缩放、双击缩放，放大后平移查看<br>
<br>
技术要点：<br>
1.自由的使用手势，用两根手指缩放动作产生的放大与缩小<br>
2.双击图片后产生的放大与缩小<br>
3.放大以后，可以对图片进行自由的移动查看细节<br>
4.处理与ViewPager之间的拦截事件冲突<br>
<br>
------------------------------------<br>
需要用到的知识点<br>
1.Matrix<br>
图片的缩放是通过矩阵变换来实现的，原理是：<br>
xScale   xSkew    xTrans<br>
ySkew    yScale   yTrans<br>
0        0        0<br>
它的数据结构是一个一维数组，程序中用全局变量mScaleMatrix来保存。<br>
mScaleMatrix会调用postScale()、postTranslate()等方法来设置矩阵变换。<br>
最后再用setImageMatrix(mScaleMatrix);来把矩阵变换更新到图片控件上。<br>
<br>
2.ScaleGestureDetector<br>
ScaleGestureDetector是一个用于多点触控判断的类，它的实例对象主要在<br>
onTouch()回调方法中调用onTouchEvent()方法，来触发多点触控的事件。<br>
<br>
3.GestureDetector + postDelay + Runnable<br>
<br>
4.事件分发机制<br>
---------------------------------------------------------------------------<br>
由于ImageView不具有我们需要的功能，所以需要复写ImageView，实现自定义的控件<br>
自定义控件 ZoomImageView extends ImageView<br>
实现了3个接口：OnGlobalLayoutListener, OnScaleGestureListener , OnTouchListener<br>
1.其构造方法需要这样写：<br>
public ZoomImageView(Context context) {<br>
	this(context , null);<br>
}<br>
<br>
public ZoomImageView(Context context, AttributeSet attrs) {<br>
	this(context, attrs , 0);<br>
}<br>
<br>
public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {<br>
	super(context, attrs, defStyle);<br>
}<br>
<br>
2.OnGlobalLayoutListener接口相关的：<br>
获取ImageView加载完成的图片后，因为图片有的大，有的小，所以需要经过调整<br>
使得正好大小适配到屏幕上，并居中显示。全局的布局完成以后，会调用这个方法:<br>
onGlobalLayout()<br>
在这个方法里面把图片调整为合适的大小显示在屏幕的正中心。<br>
<br>
onAttachedToWindow()里面需要：<br>
getViewTreeObserver().addOnGlobalLayoutListener(this);<br>
<br>
onDetachedFromWindow里面需要：<br>
getViewTreeObserver().removeGlobalOnLayoutListener(this);<br>
<br>
3.OnScaleGestureListener接口相关的：<br>
onScale()方法，非常重要，在这里面进行缩放的具体实现<br>
onScaleBegin()方法，什么也不写，最后一定要return true;<br>
onScaleEnd()方法，什么也不写<br>
<br>
4.OnTouchListener接口相关的：<br>
onTouch()方法，通过switch判断条件event.getAction()，通过各类选项：<br>
MotionEvent.ACTION_DOWN<br>
MotionEvent.ACTION_MOVE<br>
MotionEvent.ACTION_UP<br>
MotionEvent.ACTION_CANCEL<br>
来进行图片平移的设置。<br>
<br>
5.最后实现双击放大与缩小图片，需要在构造器中利用GestureDetector类<br>
的对象，实现一个匿名的内部监听器：GestureDetector.SimpleOnGestureListener<br>
然后重写onDoubleTap()方法，在这里面实现双击缩放的逻辑。<br>
<br>
6.给双击缩放添加动画效果，需要用postDelayed()这个方法，配合Runnable，具体是<br>
class AutoScaleRunnable implements Runnable<br>
在该内部类中的run()方法中postDelayed(this , 时间毫秒);来不断的调用run()方法自己<br>
达到缩放目标后，就设置矩阵变换。<br>
<br>
--------------------------------------------------------------------------------<br>
最终，该自定义控件是在ViewPager中使用，是为了体现滑动切换图片的效果。<br>
但是图片放大以后，和ViewPager的左右滑动发生冲突。<br>
冲突发生的原因：ViewPager屏蔽了子View的左右移动事件<br>
处理：在ACTION_DOWN和ACTION_MOVE里面，如果宽或高大于屏幕宽度或者高度，请求不被屏蔽<br>
getParent().requestDisallowInterceptTouchEvent(true);<br>
这样图片只有恢复到小于等于屏幕大小时，才能切换下一张图片。<br>
<br>




