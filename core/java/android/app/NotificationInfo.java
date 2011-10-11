package android.app;

import android.os.Parcelable;
import android.os.Parcel;
import android.app.PendingIntent;
import android.text.TextUtils;

public class NotificationInfo extends Object{

  public static final Parcelable.Creator<NotificationInfo> CREATOR = new Parcelable.Creator<NotificationInfo>(){
      public NotificationInfo createFromParcel(Parcel parcel)
        {
            return new NotificationInfo(parcel);
        }

        public NotificationInfo[] newArray(int size)
        {
            return new NotificationInfo[size];
        }
    };

  public CharSequence contactCharSeq;
  public PendingIntent contentIntent;
  public int missedCount;

  public NotificationInfo(){
    super();
  }

  public NotificationInfo(int missedCount, CharSequence contactCharSeq, PendingIntent contentIntent){
    super();
    this.missedCount = missedCount;
    this.contactCharSeq = contactCharSeq;
    this.contentIntent = contentIntent;
  }

  public NotificationInfo(Parcel parcel){
    super();
    
    int version = parcel.readInt();
    missedCount = version;

    if(version > 0){
      contactCharSeq = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
      contentIntent = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
    }
  }

  public NotificationInfo clone(){
    //NotificationInfo that = new NotificationInfo();
 
    return new NotificationInfo(missedCount,contactCharSeq,contentIntent);
  }
/*
  public bridge Object clone(){
    return clone();
  }*/

  public int describeContents(){
    return 0;
  }

  public void writeToParcel(Parcel parcel, int flags){
    parcel.writeInt(1);
    parcel.writeInt(missedCount);
    if(contactCharSeq != null){
      parcel.writeInt(1);
      TextUtils.writeToParcel(contactCharSeq,parcel,flags);
    }else{
      parcel.writeInt(0);
    }
    if(contentIntent != null){
      parcel.writeInt(1);
      contentIntent.writeToParcel(parcel,0);
    }else{
      parcel.writeInt(0);
    }
  }
}
    
