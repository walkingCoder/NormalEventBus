package com.beijing.zzu.bind_lib;

import android.app.Activity;

import java.lang.reflect.Field;

/**
 * @author jiayk
 * @date 2019/11/14
 */
public class ButterKnifeProcess {
    /**
     * 绑定Activity
     */
    public static void bind(final Activity activity) throws IllegalAccessException, IllegalArgumentException {
        //得到Activity对应的Class
        Class clazz = activity.getClass();
        //得到该Activity的所有字段
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            //判断字段是否标注InjectView
            if (field.isAnnotationPresent(BindView.class)) {
                //如果标注了，就获得它的id
                BindView inject = field.getAnnotation(BindView.class);
                int id = inject.value();
                if (id > 0) {
                    //反射访问私有成员，必须加上这句
                    field.setAccessible(true);
                    //然后对这个属性复制
                    field.set(activity, activity.findViewById(id));
                }
            }

        }
    }
}