package com.beijing.zzu.complier;

import com.squareup.javapoet.TypeName;

/**
 * @author jiayk
 * @date 2019/11/10
 */
public class ReceiverMethod {

    /**
     * 方法所在包的名字
     */
    private String packageName;

    /**
     * 方法所在类的名字
     */
    private String className;

    /**
     * 方法接受的action
     */
    private String[] actions;

    /**
     * 方法广播是否处于活跃状态
     */
    private boolean isActive;

    /**
     * 方法名字
     */
    private String methodName;

    /**
     * 参数个数
     */
    private int paramsCount;

    /**
     * 方法所在的类型
     */
    private TypeName typeName;


    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String[] getActions() {
        return actions;
    }

    public void setActions(String[] actions) {
        this.actions = actions;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getParamsCount() {
        return paramsCount;
    }

    public void setParamsCount(int paramsCount) {
        this.paramsCount = paramsCount;
    }

    public TypeName getTypeName() {
        return typeName;
    }

    public void setTypeName(TypeName typeName) {
        this.typeName = typeName;
    }
}
