package com.beijing.zzu.api;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * @author jiayk
 * @date 2019/11/10
 */
public class ApiModuleApplication {

    private static Context context;

    public static final boolean isApp = false;

    public static void init(@NonNull Application application) {
        context = application.getApplicationContext();
    }

    public static Context getGlobalContext() {
        if (context == null) {
            throw new NullPointerException("ApiModuleApplication must be init(application)");
        }
        return context;
    }
}
