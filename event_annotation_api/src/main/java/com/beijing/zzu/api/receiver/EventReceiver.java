package com.beijing.zzu.api.receiver;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Pair;

import com.beijing.zzu.api.ApiModuleApplication;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jiayk
 * @date 2019/11/10
 */
public class EventReceiver implements ICommonReceiver, LifecycleObserver {

    private CommonBroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private IReceiverLifecycleController controller;
    private boolean needActive;
    private boolean isRegistered;

    public EventReceiver(CommonBroadcastReceiver receiver, IntentFilter intentFilter, boolean needActive) {
        this.receiver = receiver;
        this.intentFilter = intentFilter;
        this.needActive = needActive;
        isRegistered = false;
    }

    public EventReceiver(CommonBroadcastReceiver receiver, IntentFilter intentFilter, IReceiverLifecycleController controller) {
        this.receiver = receiver;
        this.intentFilter = intentFilter;
        this.controller = controller;
        isRegistered = false;
    }

    public static ICommonReceiver bindLifecycle(LifecycleOwner lifecycleOwner) {
        EventReceiver eventReceiver = getReceiverFromProcessor(lifecycleOwner);
        if (eventReceiver != null) {
            eventReceiver.attachToLifecycle(lifecycleOwner.getLifecycle());
        }
        return eventReceiver;
    }

    private static EventReceiver getReceiverFromProcessor(@NonNull Object obj) {
        EventReceiver eventReceiver = null;

        try {
            Pair<Class<?>, Class<?>> pair = findObjectValidClass(obj.getClass());
            if (pair == null) {
                return null;
            }
            Class<?> clazz = pair.first;
            if (clazz == null) {
                return null;
            }
            Constructor<?> constructor = clazz.getConstructor();
            Object instance = constructor.newInstance();
            Method method = clazz.getMethod("buildReceiver", pair.second);
            Object object = method.invoke(instance, obj);
            eventReceiver = (EventReceiver) object;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return eventReceiver;
    }

    private static Pair<Class<?>, Class<?>> findObjectValidClass(Class<?> clazz) {
        if (clazz == null){
            return null;
        }

        String name = clazz.getCanonicalName();
        try {
            return Pair.<Class<?>, Class<?>>create(Class.forName(name + "_Receiver"), clazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return findObjectValidClass(clazz.getSuperclass());
        }
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    @Override
    public void attachToLifecycle(Lifecycle lifecycle) {
        if (lifecycle == null) {
            return;
        }
        lifecycle.addObserver(this);
    }

    public IntentFilter getIntentFilter() {
        return intentFilter == null ? new IntentFilter() : intentFilter;
    }

    @Override
    public void register(Context context) {
        context = checkNoNullContext(context);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter);
        isRegistered = true;
    }

    @Override
    public void unRegister(Context context) {
        context = checkNoNullContext(context);
        isRegistered = false;
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
        if (receiver != null) {
            receiver.releaseReceive();
        }
    }

    @Override
    public boolean isRegistered() {
        return isRegistered;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        if (!needActive) {
            register(checkNoNullContext(null));
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (needActive) {
            register(checkNoNullContext(null));
        }

        if (controller != null) {
            controller.changeLifecycleState(true);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (needActive) {
            unRegister(checkNoNullContext(null));
        }
        if (controller != null) {
            controller.changeLifecycleState(false);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        if (!needActive) {
            unRegister(checkNoNullContext(null));
        }
        releaseReceiver();
    }

    private void releaseReceiver() {
        if (receiver != null) {
            receiver.releaseReceive();
            receiver = null;
        }
        if (intentFilter != null) {
            intentFilter = null;
        }
    }

    private Context checkNoNullContext(Context context) {
        if (context == null) {
            context = ApiModuleApplication.getGlobalContext();
        }
        return context;
    }

    public static class Builder {
        private OnReceiveListener listener;
        private IntentFilter intentFilter;
        private List<String> actionList;

        /**
         * 只在onResume 和 onPause中接受事件，绑定生命周期后有效
         */
        private boolean needActive;

        public Builder setOnReceiveListener(OnReceiveListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder setIntentFilter(IntentFilter intentFilter) {
            this.intentFilter = intentFilter;
            return this;
        }

        public Builder addFilterAction(String action) {
            if (TextUtils.isEmpty(action)) {
                return this;
            }
            if (actionList == null) {
                actionList = new ArrayList<>();
            }
            actionList.add(action);
            return this;
        }

        public Builder needActive(boolean needActive) {
            this.needActive = needActive;
            return this;
        }

        public EventReceiver build() {
            if (intentFilter == null) {
                intentFilter = new IntentFilter();
            }
            if (actionList != null && !actionList.isEmpty()) {
                for (String action : actionList) {
                    intentFilter.addAction(action);
                }
            }
            CommonBroadcastReceiver commonBroadcastReceiver = CommonBroadcastReceiver.getBroadcastReceiver(listener);
            return new EventReceiver(commonBroadcastReceiver, intentFilter, needActive);
        }
    }
}
