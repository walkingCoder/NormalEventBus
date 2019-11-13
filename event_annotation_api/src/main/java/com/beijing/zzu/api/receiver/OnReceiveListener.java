package com.beijing.zzu.api.receiver;

import android.content.Context;
import android.content.Intent;

/**
 * @author jiayk
 * @date 2019/11/10
 */
public interface OnReceiveListener {

    void onReceive(Context context, Intent intent);
}
