package com.beijing.zzu.normalBroadcastReceiver.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.beijing.zzu.api.receiver.EventReceiver;
import com.beijing.zzu.event_annotation.EventBroadcastReceiver;
import com.beijing.zzu.normalBroadcastReceiver.RxEventBus;
import com.beijing.zzu.normalBroadcastReceiver.event.LoginEvent;
import com.beijing.zzu.normaleventbus.R;


public class MainActivity extends AppCompatActivity {

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_LOGOUT = "logout";
    public static final String LOGIN_NAME = "username";
    public static final String LOGIN_PASS = "password";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventReceiver.bindLifecycle(this);

        initRxEventListener();
    }

    private void initRxEventListener() {
        RxEventBus.getInstance().on(LoginEvent.class).listen(new RxEventBus.EventCallback<LoginEvent>() {
            @Override
            public void onEvent(LoginEvent event) {
                Log.d("jyk","username: "+event.username + (event.loginSuccess ? "登录成功" : "登录失败"));
            }
        },this);
    }

    @EventBroadcastReceiver(actions = {ACTION_LOGIN, ACTION_LOGOUT})
    public void onReceive(Intent intent){
        String username = intent.getStringExtra(LOGIN_NAME);
        String password = intent.getStringExtra(LOGIN_PASS);
        Log.d("jyk","username: "+username +" ,password: "+password);
    }



    public void openSecond(View view) {
        startActivity(new Intent(this,SecondActivity.class));
    }
}
