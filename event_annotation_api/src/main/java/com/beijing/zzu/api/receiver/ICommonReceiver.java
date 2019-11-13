package com.beijing.zzu.api.receiver;

import android.arch.lifecycle.Lifecycle;
import android.content.Context;

/**
 * @author jiayk
 * @date 2019/11/10
 */
public interface ICommonReceiver {

    void attachToLifecycle(Lifecycle lifecycle);

    void register(Context context);

    void unRegister(Context context);

    boolean isRegistered();
}
