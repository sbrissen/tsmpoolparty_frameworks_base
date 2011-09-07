package com.android.internal.widget;

import com.android.internal.R;

import android.widget.*;
import android.content.Context;
import android.util.AttributeSet;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.content.res.Configuration;
import android.view.*;
import android.util.Log;

public class SoftKeyView extends LinearLayout {

    private static final float DEFAULT_SOFTKEYITEM_PADDING_BOTTOM = 2.66f;
    private static final float DEFAULT_SOFTKEYITEM_PADDING_SIDE = 3.33f;
    private static final float DEFAULT_SOFTKEYITEM_PADDING_TOP = 6.66f;
    private static final float DEFAULT_SOFTKEYVIEW_HEIGHT = 60.0f;
    private static final float MAX_SOFTKEY_ITEM_COUNT = 0x4;
    private static final String TAG = "SoftKeyView";

    private final boolean debug = true;
    private LayoutParams mChildLp;
    private float mScale;
    float mSideMarginOneButton;

    public SoftKeyView(Context context){
      super(context);
      
      mChildLp = null;
      mScale = 0x0;
      mSideMarginOneButton = 0x0;
      init(context);
    }

    public SoftKeyView(Context context, AttributeSet attrs){
      super(context,attrs);

      mChildLp = null;
      mScale = 0x0;
      mSideMarginOneButton = 0x0;
      init(context);
    }

    private void init(Context context){
      setHapticFeedbackEnabled(false);
      setSoundEffectsEnabled(false);
      mScale = getContext().getResources().getDisplayMetrics().density;
      if(getResources().getConfiguration().orientation != 0x1){
	if(getResources().getConfiguration().orientation != 0x2){
	}else{
	  mSideMarginOneButton = 0x4299570a;
	}
      }else{
	mSideMarginOneButton = 0x4222ae14;
      }

      setBackgroundResource(R.drawable.softkeyview_bg);
      int backgroundHeight = (int) ((mScale * 0x4270) + 0x3f00);
      int FixedSidePadding = (int) ((mScale * 0x40551eb8) + 0x3f00);
      int FixedTopPadding = (int) ((mScale * 0x40d51eb8) + 0x3f00);
      int FixedBottomPadding = (int) ((mScale * 0x402a3d71) + 0x3f00);
      setLayoutParams(new LinearLayout.LayoutParams(-0x1,backgroundHeight));
      setOrientation(0x0);
      setPadding(FixedSidePadding,FixedTopPadding,FixedSidePadding,FixedBottomPadding);
      setGravity(0x11);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
      Log.i("SoftKeyView", "onMeasure: W E L C O M E");
      int i = 0x0;

      if(getResources().getConfiguration().orientation != 0x1){
	if(getResources().getConfiguration().orientation != 0x2){
	}else{
	  mSideMarginOneButton = 0x4299570a;
	}
      }else{
	mSideMarginOneButton = 0x4222ae14;
      }
      
      if(mChildLp != null){  
	if(getChildCount() != 0x1){ 
	  i = 0x0;
	}else{
	  int FixedSideMargin = (int) ((mSideMarginOneButton * mScale) + 0x3f00);
	  mChildLp.setMargins(FixedSideMargin,0x0,FixedSideMargin,0x0);
	}
      }else{
	mChildLp = new LinearLayout.LayoutParams(-0x1, -0x1);
	mChildLp.weight = 0x3f80;
	mChildLp.gravity = 0x11;
      }
	
      if(i >= getChildCount() ){
	super.onMeasure(widthMeasureSpec,heightMeasureSpec);
      }else{
	getChildAt(i).setLayoutParams(mChildLp);
	i++;
      }
    }

    public void setEnabledSoftkeyItem(int index, boolean bEnable){
      if(index < 0){
	return;
      }else if(index <= getChildCount()){
	if(getChildAt(index) == null){
	  return;
	}else{
	  getChildAt(index).setEnabled(bEnable);
	}
      }
    }
  }
    