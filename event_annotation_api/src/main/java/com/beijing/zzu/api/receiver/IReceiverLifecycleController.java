package com.beijing.zzu.api.receiver;

/**
 * @author jiayk
 * @date 2019/11/10
 */
public interface IReceiverLifecycleController {

    /**
     * 改变生命周期状态
     *
     * @param isActive  true 是活跃状态， false 非活跃状态
     */
    void changeLifecycleState(boolean isActive);
}
