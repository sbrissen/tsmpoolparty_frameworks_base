package com.android.internal.widget;

/*****************************************************************************************************
/	VERY ALPHA
/  MIUI SlidingPanel Widget:
/    
/  Written By: Scott Brissenden
/
*******************************************************************************************************/

import android.widget.*;
import android.widget.FrameLayout;
import java.lang.Runnable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.content.Context;
import android.util.DisplayMetrics;
import android.content.res.Resources;
import android.view.animation.*;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.*;
import android.graphics.Rect;
import android.content.ContentResolver;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.text.TextUtils;
import java.lang.System;
import android.view.animation.Animation.AnimationListener;
import java.lang.Math;
import java.util.Arrays;
import android.util.Log;
import android.view.Gravity;

import com.android.internal.R;

public class SlidingPanel extends LinearLayout{

    private static final int ANIM_MODE_FLY_DOWN = 1;
    private static final int ANIM_MODE_RESET = 0;
    private static final int BACKGROUND_MODE_BATTERY_CHARGING = 2;
    private static final int BACKGROUND_MODE_BATTERY_FULL = 3;
    private static final int BACKGROUND_MODE_BATTERY_LOW = 1;
    private static final int BACKGROUND_MODE_NORMAL = 0;
    private static final Long DOUBLE_CLICK_THRESHOLD = 0x1f4L;
    private static final String TAG = "SlidingPanel";
    private static final Long SINGLE_CLICK_THRESHOLD = 0x96L;
    private static final int SLIDER_LEFT = 0;
    private static final int SLIDER_MIDDLE = 1;
    private static final int SLIDER_RIGHT = 2;
    private static final Long VIBRATE_LONG = 0x28L;
    private static final Long VIBRATE_SHORT = 0x1eL;

    private final int MOVING_THRESHOLD;
    private SlidingPanelAnimation mAnimation;
    private int mBackgroundMode = 0;
    private ImageView mBatteryAnimationBar;
    private int mBatteryAnimationBarHeight;
    private ImageView mBatteryAnimationLight;
    private Runnable mBatteryAnimationRunnable;
    private FrameLayout mBatteryArea;
    private TextView mBatteryInfo;
    private FrameLayout mButtonRegion;
    private int mButtonRegionHeight;
    private FrameLayout mContentArea;
    private FrameLayout mControlRegion;
    private View mCurrentDragView;
    private int mDisplayHeight;
    private int mDisplayWidth;
    private SlidingPanelAnimation mDownTriggerAnimation;
    private int mDownY = 0;
    private int mFooterRegionHeight;
    private OnPanelTriggerListener mOnTriggerListener;
    private int mGrabbedState = OnPanelTriggerListener.NO_HANDLE;
    private boolean mIsPaused = false;
    private boolean mIsPressing = true;
    private Long mLastDownTime = 0L;
    private Long mLastSetGrabstateTime = 0L;
    private Slider mLeftSlider;
    private boolean mLongVibrate = false;
    private boolean mMoving = true;

    private Slider mRightSlider;
    private Runnable mSingleClick;
    private FrameLayout mTimeRegion;
    private int mTrackingPointerId = -1;
    private boolean mTriggered = false;
    private Vibrator mVibrator;

    public interface OnPanelTriggerListener {

	public static final int ANIMATION_CLICK_HANDLE = 7;
	public static final int DOUBLE_CLICK_HANDLE = 6;
        public static final int NO_HANDLE = 0;
        public static final int LEFT_HANDLE = 1;
        public static final int RIGHT_HANDLE = 2;
	public static final int MIDDLE_HANDLE = 3;
	public static final int POKE_HANDLE = 4;
	public static final int SLIDING_HANDLE = 5;

        void onTrigger(View v, int whichHandle);

        void onGrabbedStateChange(View v, int grabbedState);
    }

    class AnimationSequenceListener extends Object implements Animation.AnimationListener{
      private Animation zNext;
      private View zTarget;
      
      public AnimationSequenceListener(View target, Animation next){
	super();
	zTarget = target;
	zNext = next;
      }

      public void onAnimationEnd(Animation animation){
	zTarget.startAnimation(zNext);
      }

      public void onAnimationRepeat(Animation animation){
      }

      public void onAnimationStart(Animation animation){
      }
    }

    public class SlidingPanelAnimation extends Animation{
	  private int mDeltaY;
	  private int mInitBottom;
	  private int mMode = 0;
	  private SlidingPanel mPanel;
      
	  public SlidingPanelAnimation(SlidingPanel panel, int mode){
	      super();
	      mPanel = panel;
	      mMode = mode;
	  }

	  protected void applyTransformation(float interpolatedTime, Transformation t){
	      mPanel.scrollTo(0,(int)((float)mDeltaY * interpolatedTime) + mInitBottom);
	  }

	  public void reset(){
	      super.reset();
	      mInitBottom = mScrollY;
	      if(mMode != 0){
		if(mMode == 1){
		    mDeltaY = -mDisplayHeight + mInitBottom;
		}
	      }else{
		mDeltaY = -mInitBottom;
	      }
	  }
    }

    class Slider extends FrameLayout{
	private FrameLayout zImageBackground;
	private TextView zText;

	public Slider(Context context){
	  super(context);
	  
	  zImageBackground = new FrameLayout(context);
	  zImageBackground.setVisibility(View.GONE);
	  addView(zImageBackground,new FrameLayout.LayoutParams(-2,-2,0x11));
	  zText = new TextView(mContext);
	  zText.setBackgroundResource(R.drawable.lock_screen_preview_count);
	  zText.setGravity(0x11);
	  zText.setTextColor(-1);
	  zText.setShadowLayer(0,0,-0x4080,-0x6000);
	  zText.setTextSize(2,0x4140);
	  zText.setVisibility(View.GONE);
	  
	  FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2,-2);
	  layoutParams.gravity = 0x35;
	  zImageBackground.addView(zText,layoutParams);
	}

	public int getTextVisibility(){
	  return zText.getVisibility();
	}
	
	public void setImage(int resId){
	  zImageBackground.setBackgroundResource(resId);
	  zImageBackground.setVisibility(View.VISIBLE);
	}

	public void setText(String text){
	  zText.setText(text);
	  if(TextUtils.isEmpty(text)){
	      zText.setVisibility(View.VISIBLE);
	  }else{
	      zText.setVisibility(View.GONE);
	  }
	}
    }
    
    
    public SlidingPanel(Context context){
      this(context,null);
    }

    public SlidingPanel(Context context, AttributeSet attrs){
      super(context,attrs);
      
      mSingleClick = new Runnable(){
	public void run(){
	    removeCallbacks(mSingleClick);
	    if(!mMoving){
	      setGrabbedState(OnPanelTriggerListener.POKE_HANDLE);
	    }else{
	      mLastDownTime = 0L;
	    }
	}
      };

      mDisplayWidth = getResources().getDisplayMetrics().widthPixels;
      mDisplayHeight = getResources().getDisplayMetrics().heightPixels;
      setOrientation(1);
      setGravity(0x50);
      setChildrenDrawnWithCacheEnabled(true);
      setupContentArea();
      setupBatteryArea();
      setBackgroundFor(mBackgroundMode);
      MOVING_THRESHOLD = mButtonRegionHeight / 8;
      
      mAnimation = new SlidingPanelAnimation(this,0);
      mAnimation.setDuration(0x50);

      mAnimation.setAnimationListener(new AnimationListener(){
	public void onAnimationEnd(Animation animation){
	    if(!mIsPressing){
	      mBatteryArea.setVisibility(View.VISIBLE);
	      resetSlidingPanel();
	    }
	}
        public void onAnimationRepeat(Animation animation) {
        }
        public void onAnimationStart(Animation animation) {
        }
      });

      AnimationListener al = new AnimationListener(){
	public void onAnimationEnd(Animation animation){
	  if(!mIsPressing){
	    int handler = 0;
	    if(mCurrentDragView == mLeftSlider){
	      handler = 1;
	    }else if(mCurrentDragView == mRightSlider){
	      handler = 2;
	    }else{
	      handler = 3;
	    }
	    mBatteryArea.setVisibility(View.VISIBLE);
	    dispatchTriggerEvent(handler);
	    resetSlidingPanel();
	  }
	}
	public void onAnimationRepeat(Animation animation) {
	}
	public void onAnimationStart(Animation animation) {
	}
      };

      mDownTriggerAnimation = new SlidingPanelAnimation(this,1);
      mDownTriggerAnimation.setDuration(0x12cL);
      mDownTriggerAnimation.setAnimationListener(al);
    }

    private void dispatchTriggerEvent(int whichHandle){
      if(mOnTriggerListener != null){
		  Log.d("SlidingPanel","dispatchTriggerEvent");
	mOnTriggerListener.onTrigger(this,whichHandle);
      }
    }

    private Slider getSlider(int slider){
      Slider result = null;
      switch(slider){
	case SLIDER_LEFT:
	  Log.d("SlidingPanel","LeftSlider");
	  result = mLeftSlider;
	  break;
	case SLIDER_MIDDLE:
	  Log.d("SlidingPanel","MiddleSlider");
	  break;
	case SLIDER_RIGHT:
	  Log.d("SlidingPanel","RightSlider");
	  result = mRightSlider;
	  break;
      }
      return result;
    }

    private boolean hitDownThreshold(){
	if(-mScrollY < (mFooterRegionHeight * 2)/3){
	  return false;
	}else{
	  return true;
	}
    }

    private boolean hitVibrateThreshold(){
	if(-mScrollY < mFooterRegionHeight){
	  return false;
	}else{
	  return true;
	}
    }

    private void movePanel(float x, float y){
	Log.d("SlidingPanel","movePanel()");
	int deltaY = (int)y - mDownY;
	if(Math.min(deltaY,mFooterRegionHeight) > 0){
	  scrollTo(0,-mDownY);
	  Log.d("SlidingPanel","PanelShouldBeMoving");
	}
    }

    private void resetSlidingPanel(){
Log.d("SlidingPanel","resetSlidingPanel()");
	setBackgroundFor(mBackgroundMode);
	if(!hitDownThreshold()){
	  setGrabbedState(OnPanelTriggerListener.NO_HANDLE);
	}
    }

    private void setBatteryAnimations(){
	Log.d("SlidingPanel","setBatteryAnimations()");
	int duration1 = 1000;
	int duration2 = 1000;
	int duration3 = 1000;
	int delayDuration = 2500;
	int deltaY = mContext.getResources().getDimensionPixelOffset(R.dimen.lock_screen_battery_animation_height);
	FrameLayout.LayoutParams barLayout = (FrameLayout.LayoutParams) mBatteryAnimationBar.getLayoutParams();
	barLayout.bottomMargin = -mBatteryAnimationBarHeight;
	TranslateAnimation barTranslate1 = new TranslateAnimation(0.0f,0.0f,0.0f,(float)deltaY);
	AlphaAnimation barAlpha1 = new AlphaAnimation(0x3e4ccccd,0x3f80);
	final AnimationSet barSet1 = new AnimationSet(true);
	barSet1.addAnimation(barTranslate1);
	barSet1.addAnimation(barAlpha1);
	barSet1.setDuration((long)duration1);
	barSet1.setInterpolator(new AccelerateInterpolator());
	TranslateAnimation barTranslate2 = new TranslateAnimation(0.0f,0.0f,(float)deltaY,(float)deltaY);
	AlphaAnimation barAlpha2 = new AlphaAnimation(0x3f80,0.0f);
	barAlpha2.setFillAfter(true);
	AnimationSet barSet2 = new AnimationSet(false);
	barSet2.addAnimation(barTranslate2);
	barSet2.addAnimation(barAlpha2);
	barSet2.setDuration(duration2);
	AlphaAnimation barAlpha3 = new AlphaAnimation(0.0f,0.0f);
	barAlpha3.setFillAfter(true);
	barAlpha3.setDuration((long)duration3);
	barSet1.setAnimationListener(new AnimationSequenceListener(mBatteryAnimationBar,barSet2));
	barSet2.setAnimationListener(new AnimationSequenceListener(mBatteryAnimationBar,barAlpha3));
	mBatteryAnimationBar.startAnimation(barSet1);
	final AlphaAnimation lightAlpha1 = new AlphaAnimation(0x3f00,0x3f00);
	lightAlpha1.setFillAfter(true);
	lightAlpha1.setDuration((long)duration1);
	AlphaAnimation lightAlpha2 = new AlphaAnimation(0x3f00,0x3f80);
	lightAlpha2.setFillAfter(true);
	lightAlpha2.setDuration((long)duration2);
	AlphaAnimation lightAlpha3 = new AlphaAnimation(0x3f80,0x3f00);
	lightAlpha3.setFillAfter(true);
	lightAlpha3.setDuration((long)duration3);
	lightAlpha1.setAnimationListener(new AnimationSequenceListener(mBatteryAnimationBar,lightAlpha2));
	lightAlpha2.setAnimationListener(new AnimationSequenceListener(mBatteryAnimationBar,lightAlpha3));
	mBatteryAnimationLight.startAnimation(lightAlpha1);
	mBatteryAnimationRunnable = new Runnable(){
	  public void run(){
	      Log.d("SlidingPanel","setBatteryAnimations()-run");
	      removeCallbacks(mBatteryAnimationRunnable);
	      mBatteryAnimationBar.startAnimation(barSet1);
	      mBatteryAnimationLight.startAnimation(lightAlpha1);
	      postDelayed(mBatteryAnimationRunnable,0x1194);
	  }
	};
	      mBatteryAnimationBar.startAnimation(barSet1);
	      mBatteryAnimationBar.setSelected(true);
	      mBatteryAnimationLight.startAnimation(lightAlpha1);
	      mBatteryAnimationLight.setSelected(true);

	this.postDelayed(mBatteryAnimationRunnable,0x1194);
    }

    private void setGrabbedState(int newState){
	if(newState == 4){
	    if(mOnTriggerListener != null){
	      mOnTriggerListener.onGrabbedStateChange(this,newState);
	    }
	}else if(newState != mGrabbedState){
	    mGrabbedState = newState;
	    if(mOnTriggerListener != null){
	      mOnTriggerListener.onGrabbedStateChange(this,newState);
	    }
	}
    }

    private void setupBatteryArea(){
	Log.d("SlidingPanel","setupBatteryArea()");
	mBatteryArea = new FrameLayout(mContext);
	mBatteryArea.setVisibility(View.INVISIBLE);
	mBatteryAnimationBar = new ImageView(mContext);
	mBatteryAnimationBar.setVisibility(View.GONE);
	mBatteryArea.addView(mBatteryAnimationBar, new FrameLayout.LayoutParams(-2,-2,0x51));
	mBatteryAnimationLight = new ImageView(mContext);
	mBatteryAnimationLight.setVisibility(View.GONE);
	mBatteryArea.addView(mBatteryAnimationLight,new FrameLayout.LayoutParams(-1,-2,0x51));
	mBatteryInfo = new TextView(mContext);
	mBatteryInfo.setTextColor(-1);
	mBatteryInfo.setGravity(0x11);
	mBatteryArea.addView(mBatteryInfo,new FrameLayout.LayoutParams(-2,-1,0x51));
	mContentArea.addView(mBatteryArea,new FrameLayout.LayoutParams(-2,-2,0x51));
    }

    private void setupContentArea(){
	Log.d("SlidingPanel","setupContentArea()");
	mControlRegion = new FrameLayout(mContext);
	mControlRegion.setVisibility(View.GONE);
	addView(mControlRegion,new LinearLayout.LayoutParams(-1,-1,0x3f80));
	mContentArea = new FrameLayout(mContext);
	addView(mContentArea,new FrameLayout.LayoutParams(-1,-1,0x50));
	mButtonRegion = new FrameLayout(mContext);
	mButtonRegion.setDrawingCacheEnabled(true);
	mButtonRegion.setBackgroundResource(R.drawable.lock_screen_button_bg);
	mContentArea.addView(mButtonRegion,new FrameLayout.LayoutParams(-1,-1,0x50));
	mLeftSlider = new Slider(mContext);
	mLeftSlider.setBackgroundResource(R.drawable.lock_screen_bar_call_bg_mask);
	mLeftSlider.setImage(R.drawable.lock_screen_bar_call_bg);
	mButtonRegion.addView(mLeftSlider,new FrameLayout.LayoutParams(-1,-1,0x11));
	mTimeRegion = new FrameLayout(mContext);
	mTimeRegion.setBackgroundResource(R.drawable.lock_screen_bar_time_bg_mask);
	mButtonRegion.addView(mTimeRegion,new FrameLayout.LayoutParams(-1,-1,0x11));
	mRightSlider = new Slider(mContext);
	mRightSlider.setBackgroundResource(R.drawable.lock_screen_bar_sms_bg_mask);
	mRightSlider.setImage(R.drawable.lock_screen_bar_sms_bg);
	mButtonRegion.addView(mRightSlider,new FrameLayout.LayoutParams(-1,-1,0x11));
	Drawable callBgDrawable = mLeftSlider.getBackground();
	mButtonRegionHeight = callBgDrawable.getIntrinsicHeight();
	Drawable buttonRegionBg = mButtonRegion.getBackground();
	Rect rect = new Rect();
	buttonRegionBg.getPadding(rect);
	mFooterRegionHeight = rect.bottom;
	ImageView contentAreaMask = new ImageView(mContext);
	contentAreaMask.setImageResource(R.drawable.lock_screen_button_mask);
	mContentArea.addView(contentAreaMask, new FrameLayout.LayoutParams(-1,-1,0x50));
    }

    private synchronized void vibrate(long duration) {
        if (mVibrator == null) {
            mVibrator = (android.os.Vibrator)
                    getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        mVibrator.vibrate(duration);
    }	

    private boolean withinView(float x, float y, View view){
	int[] tmpLocation;
	tmpLocation = new int[2];
	view.getLocationOnScreen(tmpLocation);
	int startX = view.getPaddingLeft() + tmpLocation[0];
	int endX = (view.getWidth() + tmpLocation[0]) - view.getPaddingRight();
	int startY = view.getPaddingTop() + tmpLocation[1];
	int endY = (view.getHeight() + tmpLocation[1]) - view.getPaddingBottom();
	
	if(Float.compare((float)startX,x) > 0){
	  return false;
	}else if(Float.compare(x,(float)endX) > 0){
	  return false;
	}else if(Float.compare((float)startY,y) > 0){
	  return false;
	}else if(Float.compare(y,(float)endY) > 0){
	  return false;
	}else{
	  return true;
	}
    }

    public void clearBatteryAnimations(){
	Log.d("SLidingPanel","ClearBatteryAnimations()");
	removeCallbacks(mBatteryAnimationRunnable);
	Animation animation = mBatteryAnimationBar.getAnimation();
	if(animation != null){
	  animation.setAnimationListener(null);
	  mBatteryAnimationBar.clearAnimation();
	}else if(mBatteryAnimationLight.getAnimation() != null){
	  //animation.setAnimationListener(null);
	  mBatteryAnimationLight.clearAnimation();
	}
    }

    public int getBottomHeight(){
	return mButtonRegionHeight + mFooterRegionHeight;
    }

    public FrameLayout getControlView(){
	return mControlRegion;
    }

    public int getSliderTextVisibility(int slider){
	return getSlider(slider).getTextVisibility();
    }

    public boolean onInterceptTouchEvent(MotionEvent event){
	int action = event.getAction();
	float x = event.getRawX();
	float y = event.getRawY();
	boolean leftHit = this.withinView(x,y,mLeftSlider);
	boolean rightHit = this.withinView(x,y,mRightSlider);
	boolean timeViewHit = this.withinView(x,y,mTimeRegion);
	dumpevent2(event);
	
	if(mTrackingPointerId > 0 || leftHit || rightHit || timeViewHit){
	  switch(action){
	    case MotionEvent.ACTION_DOWN:
	      mTrackingPointerId = event.getPointerId(0);
	      mMoving = false;
	      mTriggered = false;
	      mDownY = (int)y;
	      vibrate(VIBRATE_SHORT);
	      if(timeViewHit){ 
		mCurrentDragView = mTimeRegion;
		Long mLongTime = System.currentTimeMillis() - mLastDownTime;
		if(mLongTime.compareTo(0x1f4L) >= 0){    
		    mLastDownTime = System.currentTimeMillis();
		    setGrabbedState(OnPanelTriggerListener.NO_HANDLE);
		    postDelayed(mSingleClick,0x96L);
		}else{
		    mLastDownTime = 0L;
		    setGrabbedState(OnPanelTriggerListener.DOUBLE_CLICK_HANDLE);
		}
	      }else if(leftHit){
		mCurrentDragView = mLeftSlider;
		setGrabbedState(OnPanelTriggerListener.LEFT_HANDLE);
	      }else if(rightHit){
		mCurrentDragView = mRightSlider;
		setGrabbedState(OnPanelTriggerListener.RIGHT_HANDLE);
	      }else{
		setGrabbedState(OnPanelTriggerListener.POKE_HANDLE);
	      }	
	      mLastSetGrabstateTime = SystemClock.elapsedRealtime();
	      if(mCurrentDragView != null){
		mIsPressing = true;
		mCurrentDragView.setPressed(true);
		setBackgroundFor(mBackgroundMode);
	      }
	      return true;
	    }
	  }
      return false;
    }
    

    protected void onLayout(boolean changed,int l, int t, int r, int b){
	super.onLayout(changed,l,t,r,b);
	if(changed && mBatteryAnimationBar != null && mBatteryAnimationBar.getAnimation() != null){
	    clearBatteryAnimations();
	    
	}
	setBatteryAnimations();
    }

    public void onPause(){
	mIsPaused = true;
	mBatteryArea.setVisibility(View.GONE);
	clearBatteryAnimations();
    }

    public void onResume(){
	mIsPaused = false;
	scrollTo(0,0);
	setBatteryAnimations();
	setBackgroundFor(mBackgroundMode);
    }

    public boolean onTouchEvent(MotionEvent event){
	if(mTrackingPointerId >= 0){
	  if(mTrackingPointerId == event.getPointerId(0)){
	    	      
	    int action = event.getAction();
	    float x = event.getRawX();
	    float y = event.getRawY();
	    dumpEvent(event);
	    switch(action){
	    case MotionEvent.ACTION_DOWN:
Log.d("SlidingPanel", "onTouchEvent ACTION_DOWN");
	      mIsPressing = false;
	      removeCallbacks(mSingleClick);

		//mTriggered = true;
		//mMoving = true;
	      if(mTriggered){
		Log.d("SlidingPanel", "onTouchEvent ACTION_DOWN 1");
		mTrackingPointerId = -1;
		if(mMoving){ 	
		Log.d("SlidingPanel", "onTouchEvent ACTION_DOWN 2");
		  if(hitDownThreshold()){
		    Log.d("SlidingPanel", "onTouchEvent ACTION_DOWN 3");
		    setGrabbedState(OnPanelTriggerListener.ANIMATION_CLICK_HANDLE);
		  }else{
		    Log.d("SlidingPanel", "onTouchEvent ACTION_DOWN 4");
		    startAnimation(mDownTriggerAnimation);
		  }
		}else{
		  Log.d("SlidingPanel", "onTouchEvent ACTION_DOWN 5");
		  resetSlidingPanel();		  
		}
	      }
	      break;

	      case MotionEvent.ACTION_MOVE:
Log.d("SlidingPanel", "onTouchEvent ACTION_MOVE");
		//if(mMoving || Float.compare(y - Math.abs((float)mDownY),(float)MOVING_THRESHOLD) >= 0){ 
		    if(mMoving){
			Long mLongLastGrab = SystemClock.elapsedRealtime() - mLastSetGrabstateTime;
			if(mLongLastGrab.compareTo(0xfa0L) >= 0){
			  setGrabbedState(OnPanelTriggerListener.POKE_HANDLE);
			  mLastSetGrabstateTime = SystemClock.elapsedRealtime();
			  Log.d("SlidingPanel", "onTouchEvent MOVE  1");
			}else{
			  Log.d("SlidingPanel", "onTouchEvent MOVE  2");
			  movePanel(x,y);
			  if(mTriggered || !hitDownThreshold()){
			    Log.d("SlidingPanel", new StringBuilder().append("onTouchEvent MOVE  2: ").append(mTriggered).toString());
			      if(hitDownThreshold()){
				if(mLongVibrate || !hitVibrateThreshold()){
				    Log.d("SlidingPanel", "onTouchEvent MOVE  3");
				    if(!hitVibrateThreshold()){
					mLongVibrate = false;
					Log.d("SlidingPanel", "onTouchEvent MOVE  4: long vibrate false");
				    }
				}else{
				  mLongVibrate = true;
Log.d("SlidingPanel", "onTouchEvent MOVE  4: long vibrate true");
				}
			      }else{
				mTriggered = false;
Log.d("SlidingPanel", "onTouchEvent MOVE  4: triggered false");
			      }
			  }else{
			      mTriggered = true;
Log.d("SlidingPanel", "onTouchEvent MOVE  4: triggered true");
			  }
			}
		    }else{
			Log.d("SlidingPanel", "onTouchEvent MOVE  moving");
			setGrabbedState(OnPanelTriggerListener.SLIDING_HANDLE);
			mBatteryInfo.setVisibility(View.GONE);
			mBatteryAnimationBar.setVisibility(View.GONE);
			mMoving = true;
		    }
		//}
	    break;

             //  case MotionEvent.ACTION_CANCEL:
	    case MotionEvent.ACTION_UP:
	      Log.d("SlidingPanel", "onTouchEvent ACTION_UP");
	      mIsPressing = false;
	      removeCallbacks(mSingleClick);
	      mTrackingPointerId = -1;
	      mTriggered = false;
	      if(mCurrentDragView != null){   
		  mCurrentDragView.setPressed(false);
		  mCurrentDragView = null;
	      }else{
		mDownY = 0;
		if(mMoving){
	      Log.d("SlidingPanel", "onTouchEvent ACTION_UP startanimation");
		  startAnimation(mAnimation);
		}else{
	      Log.d("SlidingPanel", "onTouchEvent ACTION_UP resetSlidingPanel");
		   resetSlidingPanel();
		}
	      }
	      break;
	    }
	  }else{
	    event.setAction(3);
	  }
	}else if(mTrackingPointerId >= 0){
	  return true;
	}else if(!super.onTouchEvent(event)){
	  return false;
	}
      return true;
    }

    public void setBackgroundFor(int mode){
	int resId = 0;
	int batteryAnimationBarId = R.drawable.lock_screen_battery_charging_bar;
	int batteryAnimationLightId = 0;
	boolean batteryInfoVisible = false;
	mBackgroundMode = mode;
	
	if(!mIsPaused){
	  switch(mode){
	    case BACKGROUND_MODE_BATTERY_FULL:
	      Log.d("SlidingPnl","battery mode full");
	      batteryAnimationBarId = R.drawable.lock_screen_battery_charging_bar;
	      break;
	    case BACKGROUND_MODE_BATTERY_LOW:
	      Log.d("SlidingPnl","battery mode low");
	      resId = R.drawable.lock_screen_battery_low_bg;
	      batteryAnimationBarId = R.drawable.lock_screen_battery_low_bar;
	      batteryAnimationLightId = R.drawable.lock_screen_battery_low_light;
	      mBatteryInfo.setShadowLayer(0x4040f,0.0f,0.0f,-0x4cb70100);
	      break;
	    case BACKGROUND_MODE_NORMAL:
	      Log.d("SlidingPnl","battery mode normal");
	      if(mIsPressing){
		resId = R.drawable.lock_screen_button_bg_pressed;
		batteryAnimationLightId = R.drawable.lock_screen_button_mask;
	      }else{
		resId = R.drawable.lock_screen_button_bg;
		
	      }
	      batteryAnimationBarId = R.drawable.lock_screen_battery_low_bar;
	      break;
	    case BACKGROUND_MODE_BATTERY_CHARGING:
	      Log.d("SlidingPnl","battery mode charging");
	      batteryAnimationLightId = R.drawable.lock_screen_battery_charging_light;
	      batteryAnimationBarId = R.drawable.lock_screen_battery_charging_bar;
	      resId = R.drawable.lock_screen_battery_charging_bg;
	      mBatteryInfo.setShadowLayer(0x4040f,0.0f,0.0f,-0x4cb70100);
	      break;
	  }
	  mButtonRegion.setBackgroundResource(resId);
	  clearBatteryAnimations();
	  if(batteryAnimationLightId != 0){                    
	      mBatteryAnimationLight.setImageResource(batteryAnimationLightId);
	      mBatteryAnimationLight.setVisibility(View.VISIBLE);
	      Log.d("SlidingPnl","battery background 1");
	  }else{
	      mBatteryAnimationLight.setVisibility(View.GONE);
	      Log.d("SlidingPnl","battery background 2");
	  }
	  if(batteryAnimationBarId != 0 && !mIsPressing)
	      mBatteryAnimationBar.setImageResource(batteryAnimationBarId);
	      mBatteryAnimationBar.setVisibility(View.VISIBLE);
	      //mBatteryAnimationBarHeight = mBatteryAnimationBar.getDrawable().getIntrinsicHeight();
	      setBatteryAnimations();
	      Log.d("SlidingPnl","battery background 3");
	  }else{
	      Log.d("SlidingPnl","battery background 4");
	      mBatteryAnimationBar.setVisibility(View.GONE);
	  }
	  mBatteryArea.setVisibility(mBatteryAnimationLight.getVisibility());
	  if(mBackgroundMode != 0 && !mIsPressing){
	     batteryInfoVisible = true;
	  }else{
	     batteryInfoVisible = false;
	  }
	  if(batteryInfoVisible){
	    mBatteryInfo.setVisibility(View.VISIBLE);
	  }else{
	    mBatteryInfo.setVisibility(View.GONE);
	  }
    }			
  
    public void setBatteryInfo(String info){
	mBatteryInfo.setText(info);
    }

    public void setOnTriggerListener(OnPanelTriggerListener listener){
      mOnTriggerListener = listener;
    }

    public void setSliderText(int slider, String text){
      getSlider(slider).setText(text);
    }

    public void setTimeView(View view, LayoutParams params){
	mTimeRegion.removeAllViews();

	if(params == null){
	  mTimeRegion.addView(view,new FrameLayout.LayoutParams(-1,-1,0x11));
	}else{	
	  mTimeRegion.addView(view,params);
	}
	mTimeRegion.setVisibility(View.VISIBLE);
    }

private void dumpEvent(MotionEvent event) {
   String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
      "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
   StringBuilder sb = new StringBuilder();
   int action = event.getAction();
   int actionCode = action & MotionEvent.ACTION_MASK;
   sb.append("event ACTION_" ).append(names[actionCode]);
   if (actionCode == MotionEvent.ACTION_POINTER_DOWN
         || actionCode == MotionEvent.ACTION_POINTER_UP) {
      sb.append("(pid " ).append(
      action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
      sb.append(")" );
   }
   sb.append("[" );
   for (int i = 0; i < event.getPointerCount(); i++) {
      sb.append("#" ).append(i);
      sb.append("(pid " ).append(event.getPointerId(i));
      sb.append(")=" ).append((int) event.getX(i));
      sb.append("," ).append((int) event.getY(i));
      if (i + 1 < event.getPointerCount())
         sb.append(";" );
   }
   sb.append("]" );
   Log.d(TAG, sb.toString());
}
private void dumpevent2(MotionEvent event) {
   String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
      "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
   StringBuilder sb = new StringBuilder();
   int action = event.getAction();
   int actionCode = action & MotionEvent.ACTION_MASK;
   sb.append("event ACTION_" ).append(names[actionCode]);
   if (actionCode == MotionEvent.ACTION_POINTER_DOWN
         || actionCode == MotionEvent.ACTION_POINTER_UP) {
      sb.append("(pid " ).append(
      action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
      sb.append(")" );
   }
   sb.append("[" );
   for (int i = 0; i < event.getPointerCount(); i++) {
      sb.append("#" ).append(i);
      sb.append("(pid " ).append(event.getPointerId(i));
      sb.append(")=" ).append((int) event.getX(i));
      sb.append("," ).append((int) event.getY(i));
      if (i + 1 < event.getPointerCount())
         sb.append(";" );
   }
   sb.append("]" );
   Log.d("SLidingPanel2", sb.toString());
}
}
	
	  
	

      
      