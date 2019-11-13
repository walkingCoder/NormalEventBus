package com.beijing.zzu.normalBroadcastReceiver.event;

/**
 * @author jiayk
 * @date 2019/11/11
 */
public class LoginEvent {

    public String username;

    public boolean loginSuccess;

    public LoginEvent(String username, boolean loginSuccess) {
        this.username = username;
        this.loginSuccess = loginSuccess;
    }
}
