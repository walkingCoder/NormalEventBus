package com.beijing.zzu.normalBroadcastReceiver.ui;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.beijing.zzu.normalBroadcastReceiver.RxEventBus;
import com.beijing.zzu.normalBroadcastReceiver.event.LoginEvent;
import com.beijing.zzu.normaleventbus.R;


public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }

    public void sendBroad(View view) {

        Intent intent = new Intent(MainActivity.ACTION_LOGIN);
        intent.putExtra(MainActivity.LOGIN_NAME,"ddd");
        intent.putExtra(MainActivity.LOGIN_PASS,"123456");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void sendEvent(View view) {
        RxEventBus.getInstance().fire(new LoginEvent("张三", true));
    }
}
