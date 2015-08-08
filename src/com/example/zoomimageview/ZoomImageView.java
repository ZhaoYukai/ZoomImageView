package com.example.zoomimageview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class ZoomImageView extends ImageView implements OnGlobalLayoutListener, OnScaleGestureListener , OnTouchListener {
	
	//--手指控制缩放的成员变量---------------------------------------------------
	private boolean mOnce = false;
	//初始化时缩放的值
	private float mInitScale;
	//双击放大时到达的值
	private float mMidScale;
	//放大的极限
	private float mMaxScale;
	
	private Matrix mScaleMatrix = null;
	
	//用于多点触控的类对象
	private ScaleGestureDetector mScaleGestureDetector = null;
	
	//--自由移动相关的成员变量---------------------------------------------------
	
	//记录上一次多点触控的数量
	private int mLastPointerCount;
	
	//最后一次的位置
	private float mLastX;
	private float mLastY;
	
	private int mTouchSlop;
	
	private boolean isCanDrag;
	
	private boolean isCheckLeftAndRight;
	private boolean isCheckTopAndBottom;
	
	//--双击放大缩小的成员变量---------------------------------------------------
	private GestureDetector mGestureDetector = null;
	private boolean isAutoScale;
	
	
	
	public ZoomImageView(Context context) {
		this(context , null);
	}
	
	public ZoomImageView(Context context, AttributeSet attrs) {
		this(context, attrs , 0);
	}

	public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		mScaleMatrix = new Matrix();
		setScaleType(ScaleType.MATRIX);
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		setOnTouchListener(this);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mGestureDetector = new GestureDetector(
				context, 
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onDoubleTap(MotionEvent e) {
						
						if(isAutoScale == true) {
							return true;
						}
						
						float x = e.getX();
						float y = e.getY();
						
						//如果当前缩放大小<2倍，就放大到2倍
						if(getCurrentScale() < mMidScale) {
							postDelayed(new AutoScaleRunnable(mMidScale, x, y) , 16);
							isAutoScale = true;
						}
						else { //如果当前缩放大小>2倍，就缩小到1倍
							postDelayed(new AutoScaleRunnable(mInitScale, x, y) , 16);
							isAutoScale = true;
						}
						
						return true;
					}
				});
	}

	
	/**
	 * 获取ImageView加载完成的图片
	 * 因为图片有的大，有的小，所以需要经过调整使得正好大小适配到屏幕上，并居中显示
	 */
	@Override
	public void onGlobalLayout() {
		//全局的布局完成以后，会调用这个方法
		if(mOnce == false) {
			//控件的宽和高
			int width = getWidth();
			int height = getHeight();
			
			//得到我们的图片，以及宽和高
			Drawable drawable = getDrawable();
			if(drawable == null) {
				return;
			}
			int drawableWidth = drawable.getIntrinsicWidth();
			int drawableHeight = drawable.getIntrinsicHeight();
			
			//下面进行缩放
			float scale = 1.0f;
			if(drawableWidth > width && drawableHeight < height) { //如果图片很宽，但高度低
				scale = width * 1.0f / drawableWidth;
			}
			if(drawableWidth < width && drawableHeight > height) { //如果图片很窄，但是高度很高
				scale = height * 1.0f / drawableHeight;
			}
			if(drawableWidth > width && drawableHeight > height) { //如果图片的宽和高都很大
				scale = Math.min(width * 1.0f / drawableWidth , height * 1.0f / drawableHeight);
			}
			if(drawableWidth < width && drawableHeight < height) { //如果图片的宽和高都很小
				scale = Math.min(width * 1.0f / drawableWidth , height * 1.0f / drawableHeight);
			}
			
			//得到了初始化时缩放的比例
			mInitScale = scale;
			mMidScale = mInitScale * 2;
			mMaxScale = mInitScale * 4;
			
			//将图片移动到控件的中心
			int dx = width / 2 - drawableWidth / 2;
			int dy = height / 2 - drawableHeight / 2;
			
			//进行平移
			mScaleMatrix.postTranslate(dx, dy);
			//进行缩放
			mScaleMatrix.postScale(mInitScale , mInitScale , width / 2 , height / 2);
			//设置这个矩阵
			setImageMatrix(mScaleMatrix);
			
			mOnce = true;
		}
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}
	
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
		getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}
	
	
	
	/**
	 * 得到当前的缩放比例
	 */
	public float getCurrentScale() {
		float[] values = new float[9];
		mScaleMatrix.getValues(values);
		return values[Matrix.MSCALE_X];
	}
	
	
	

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float scale = getCurrentScale();
		//拿到手指触控后得到的缩放的值，可能是1点几，也可能是0点几
		float scaleFactor = detector.getScaleFactor();
		
		if(getDrawable() == null) {
			return true;
		}
		
		//前面是“想放大”，后面是“想缩小”，这里进行的是缩放范围的控制
		if( (scale < mMaxScale && scaleFactor > 1.0f) || (scale > mInitScale && scaleFactor < 1.0f) ) {
			if(scale * scaleFactor < mInitScale) {
				scaleFactor = mInitScale / scale; //也就是让scale*scaleFactor=mInitScale
			}
			if(scale * scaleFactor > mMaxScale) {
				scaleFactor = mMaxScale / scale; //也就是让scale*scaleFactor=mMaxScale
			}
		}
		
		//进行缩放detector.getFocusX()和detector.getFocusY()是以手指触控的中心点
		mScaleMatrix.postScale(scaleFactor , scaleFactor , detector.getFocusX() , detector.getFocusY());
		
		checkBorderAndCenterWhenScale();
		
		setImageMatrix(mScaleMatrix);
		
		return true;
	}
	
	
	/**
	 * 获得图片放大或缩小以后的宽和高，以及left、right、top、bottom
	 */
	private RectF getMatrixRectF() {
		Matrix matrix = mScaleMatrix;
		RectF rectF = new RectF();
		Drawable drawable = getDrawable();
		if(drawable != null) {
			rectF.set(0 , 0 , drawable.getIntrinsicWidth() , drawable.getIntrinsicHeight());
			matrix.mapRect(rectF);
		}
		return rectF;
	}
	
	

	/**
	 * 在缩放的时候，进行边界的控制，以及我们的位置的控制
	 */
	private void checkBorderAndCenterWhenScale() {
		
		RectF rect = getMatrixRectF();
		
		//差值
		float deltaX = 0.0f;
		float deltaY = 0.0f;
		
		//控件的宽度和高度
		int width = getWidth();
		int height = getHeight();
		
		//有白边出现就用平移补白边
		if(rect.width() >= width) {
			if(rect.left > 0) { //如果左边有空隙，接下来就要弥补
				deltaX = -rect.left; //负值，表示应该向左移动
			}
			if(rect.right < width) { //如果右边有空隙，接下来就要弥补
				deltaX = width - rect.right; //正值，表示应该向右移动
			}
		}
		if(rect.height() >= height) {
			if(rect.top > 0) {
				deltaY = -rect.top;
			}
			if(rect.bottom < height) {
				deltaY = height - rect.bottom;
			}
		}
		
		//如果宽度或高度小于控件的宽度或高度，就居中
		if(rect.width() < width) {
			deltaX = width / 2f - rect.right + rect.width() / 2f;
		}
		if(rect.height() < height) {
			deltaY = height / 2f - rect.bottom + rect.height() / 2f;
		}
		
		//把之前得到的平移数据更新到mScaleMatrix中
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		
		//在这里一定要改为返回true
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		if(mGestureDetector.onTouchEvent(event)) {
			return true;
		}
		
		mScaleGestureDetector.onTouchEvent(event);
		
		//记录中心点的位置
		float x = 0;
		float y = 0;
		
		//拿到多点触控的数量
		int pointerCount = event.getPointerCount();
		
		for(int i = 0; i < pointerCount ; i++) {
			//这里之所以要累加是为了下面计算平均值
			x += event.getX(i);
			y += event.getY(i);
		}
		
		//最终中心点的位置是通过计算平均值约等于出来的
		x /= pointerCount;
		y /= pointerCount;
		
		if(mLastPointerCount != pointerCount) {
			isCanDrag = false;
			mLastX = x;
			mLastY = y;
		}
		mLastPointerCount = pointerCount;
		RectF rectF = getMatrixRectF();
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if(rectF.width() > getWidth() + 0.01 || rectF.height() > getHeight() + 0.01) {
				if(getParent() instanceof ViewPager) {
					//请求父控件不被允许拦截当前的控件
					getParent().requestDisallowInterceptTouchEvent(true);
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if(rectF.width() > getWidth() + 0.01 || rectF.height() > getHeight() + 0.01) {
				if(getParent() instanceof ViewPager) {
					//请求父控件不被允许拦截当前的控件
					getParent().requestDisallowInterceptTouchEvent(true);
				}
			}
			float deltaX = x - mLastX;
			float deltaY = y - mLastY;
			if(isCanDrag == false) {
				isCanDrag = isMoveAction(deltaX , deltaY);
			}
			if(isCanDrag == true) {
				//在这里面完成图片的移动
				if(getDrawable() != null) {
					isCheckLeftAndRight = true;
					isCheckTopAndBottom = true;
					if(rectF.width() < getWidth()) { //如果图片的宽度<控件的宽度
						isCheckLeftAndRight = false;
						deltaX = 0; //就不允许横向移动
					}
					if(rectF.height() < getHeight()) { //如果图片的高度<控件的高度
						isCheckTopAndBottom = false;
						deltaY = 0; //就不允许竖向移动
					}
					//之所以要这样安排，是因为移动的目的是为了显示出没有在控件内显示的东西
					mScaleMatrix.postTranslate(deltaX, deltaY);
					//控制移动的边界
					checkBorderWhenTranslate();
					setImageMatrix(mScaleMatrix);
				}
			}
			mLastX = x;
			mLastY = y;
			break;
		case MotionEvent.ACTION_UP:
			mLastPointerCount = 0;
			break;
		case MotionEvent.ACTION_CANCEL:
			mLastPointerCount = 0;
			break;
		}
		
		return true;
	}

	
	
	/**
	 * 控制平移图片的边界
	 */
	private void checkBorderWhenTranslate() {
		
		RectF rectF = getMatrixRectF();
		
		float deltaX = 0;
		float deltaY = 0;
		
		//控件的宽和高
		int width = getWidth();
		int height = getHeight();
		
		if(rectF.top > 0 && isCheckTopAndBottom) {
			deltaY = -rectF.top;
		}
		if(rectF.bottom < height && isCheckTopAndBottom) {
			deltaY = height - rectF.bottom;
		}
		if(rectF.left > 0 && isCheckLeftAndRight) {
			deltaX = -rectF.left;
		}
		if(rectF.right < width && isCheckLeftAndRight) {
			deltaX = width - rectF.right;
		}
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}
	
	

	/**
	 * 判断是否移动了
	 */
	private boolean isMoveAction(float deltaX, float deltaY) {
		
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY) > mTouchSlop;
		
	}
	
	
	/**
	 * 自动放大与缩小
	 */
	private class AutoScaleRunnable implements Runnable {
		
		//缩放的目标值
		private float mTargetScale;
		//缩放的中心点
		private float x;
		private float y;
		
		private final float BIGGER = 1.07f;
		private final float SMALL = 0.93f;
		
		//临时的变量
		private float tmpScale;
		
		

		public AutoScaleRunnable(float mTargetScale, float x, float y) {
			this.mTargetScale = mTargetScale;
			this.x = x;
			this.y = y;
			
			if(getCurrentScale() < mTargetScale) {
				tmpScale = BIGGER; //目标是想放大
			}
			else if(getCurrentScale() > mTargetScale) {
				tmpScale = SMALL; //目标是想缩小
			}
		}



		@Override
		public void run() {
			//进行缩放
			mScaleMatrix.postScale(tmpScale , tmpScale , x , y);
			checkBorderAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);
			
			float currentScale = getCurrentScale();
			//if中的作用是，如果没有达到目标值，就一直通过postDelayed()执行run()方法，直到进入else为止
			if( (tmpScale > 1.0f && currentScale < mTargetScale) || (tmpScale < 1.0f && currentScale > mTargetScale) ) {
				postDelayed(this , 16); //传this就是传自己
			}
			else {
				//就设置为我们的目标值
				float scale = mTargetScale / currentScale;
				mScaleMatrix.postScale(scale, scale, x, y);
				checkBorderAndCenterWhenScale();
				setImageMatrix(mScaleMatrix);
				isAutoScale = false;
			}
		}
		
	}

}
