package com.android.internal.widget;

import com.android.internal.R;

import android.graphics.drawable.*;
import android.content.Context;
import android.widget.*;
import android.util.AttributeSet;
import android.content.res.TypedArray;
import java.lang.StringBuilder;
import android.util.Log;
import android.R.styleable;

public class SoftKeyItem extends FrameLayout {

    public static final int IMAGE = 1;
    public static final int SPACE = 3;
    private static final String TAG = "SoftKeyItem";
    public static final int TEXT = 0;
    public static final int TEXT_AND_IMAGE = 2;
    private static int mSoftkeyItemType = 0;

    private final boolean debug = true;
    private ImageView mImage;
    private Drawable mItemIcon;
    private String mItemText;
    private TextView mText;

    public SoftKeyItem(Context context){
      super(context);
      
      mItemText = "";
      init(context, null);
    }

    public SoftKeyItem(Context context, AttributeSet attrs){
      super(context,attrs);
  
      mItemText = "";
      
      TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.SoftkeyItem);
      mSoftkeyItemType = a.getInt(0,0);
      mItemText = a.getString(1);
      Log.i("SoftKeyItem",new StringBuilder().append("SoftKeyItem: mItemText is ").append(mItemText).toString());

      mItemIcon = a.getDrawable(2);
      init(context,attrs);
      a.recycle();
    }

    private void init(Context context, AttributeSet attrs){
      setFocusable(true);
      setClickable(true);
      setBackgroundResource(R.drawable.button_background);
      switch(mSoftkeyItemType){
	case R.styleable.SoftkeyItem_SoftkeyItemText:
	  mText = new TextView(context,null,R.attr.SoftkeyItemStyle);
	  mText.setText(mItemText);
	  mText.setClickable(false);
	  mText.setDuplicateParentStateEnabled(true);
	  addView(mText);
	case R.styleable.SoftkeyItem_SoftkeyItemImage:
	  mImage = new ImageView(context,null,R.attr.SoftkeyItemStyle);
	  mImage.setImageDrawable(mItemIcon);
	  mImage.setClickable(false);
	  addView(mImage);
	case R.styleable.SoftkeyItem_SoftkeyItemType:
	  setVisibility(4);
	  setHapticFeedbackEnabled(false);
      }
    }

    public static SoftKeyItem makeImage(Context context, int itemIconId){
      return makeImage(context,context.getResources().getDrawable(itemIconId));
    }

    public static SoftKeyItem makeImage(Context context, Drawable icon){
      mSoftkeyItemType = 1;

      SoftKeyItem imageItem = new SoftKeyItem(context);
      imageItem.setSoftkeyItemImage(icon);
      return imageItem;
    }

    public static SoftKeyItem makeSpace(Context context){
      mSoftkeyItemType = 3;
      
      SoftKeyItem spaceItem = new SoftKeyItem(context);
      return spaceItem;
    }

    public static SoftKeyItem makeText(Context context, int itemTextId){
      return makeText(context,context.getResources().getString(itemTextId));
    }

    public static SoftKeyItem makeText(Context context, CharSequence text){
      mSoftkeyItemType = 0;

      SoftKeyItem textItem = new SoftKeyItem(context);
      textItem.setSoftkeyItemText(text);
      return textItem; 
    }

    private void setSoftkeyItemImage(Drawable icon){
      mImage.setImageDrawable(icon);
    }

    private void setSoftkeyItemText(CharSequence text){
      mText.setText(text);
    }
 }