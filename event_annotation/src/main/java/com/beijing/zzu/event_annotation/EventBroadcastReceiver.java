package com.beijing.zzu.event_annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jiayk
 * @date 2019/11/10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventBroadcastReceiver {

    /**
     * 广播需要监听的Actions
     *
     * @return
     */
    String[] actions() default {};

    /**
     * 是否在生命周期活跃状态，默认false
     *
     * @return true在生命活跃状态，false 整个生命周期
     */
    boolean isActive() default false;

}
