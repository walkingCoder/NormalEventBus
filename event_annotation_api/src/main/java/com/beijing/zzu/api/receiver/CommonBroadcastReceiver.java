package com.beijing.zzu.api.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author jiayk
 * @date 2019/11/10
 */
public class CommonBroadcastReceiver extends BroadcastReceiver {

    private OnReceiveListener listener;

    public static CommonBroadcastReceiver getBroadcastReceiver(OnReceiveListener listener){
        return new CommonBroadcastReceiver(listener);
    }

    public CommonBroadcastReceiver(OnReceiveListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (listener != null){
            listener.onReceive(context, intent);
        }
    }

    public void releaseReceive(){
        if (listener != null){
            listener = null;
        }
    }
}
