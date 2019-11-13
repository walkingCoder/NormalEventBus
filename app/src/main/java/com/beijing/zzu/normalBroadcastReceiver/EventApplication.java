package com.beijing.zzu.normalBroadcastReceiver;

import android.app.Application;

import com.beijing.zzu.api.ApiModuleApplication;

/**
 * @author jiayk
 * @date 2019/11/10
 */
public class EventApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ApiModuleApplication.init(this);
    }
}
