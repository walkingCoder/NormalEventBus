package com.beijing.zzu.complier;

import com.beijing.zzu.event_annotation.EventBroadcastReceiver;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * 编译时处理的注解
 * @author jiayk
 * @date 2019/11/10
 */
@AutoService(Processor.class)
public class EventProcessor extends AbstractProcessor {

    private static final String PKG_CONTENT = "android.content";
    private static final String PKG_RECEIVER = "com.beijing.zzu.api.receiver";
    private static final String RECEIVER_SUFFIX = "_Receiver";

    private static final String TYPE_INTENT = "android.content.Intent";
    private static final String TYPE_CONTEXT = "android.content.Context";

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private Types types;

    private Map<String, List<ReceiverMethod>> receiverMap;

    private Set<TypeElement> erasedTargetNames = new LinkedHashSet<>();
    private Map<String, TypeElement> typeElementMap;

    /**
     * 每个Annotation Processor必须有一个空的构造函数。
     * 编译期间，init()会自动被注解处理工具调用，并传入ProcessingEnvironment参数，
     * 通过该参数可以获取到很多有用的工具类（Element，Filer，Messager等）
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
        elementUtils = processingEnv.getElementUtils();
        types = processingEnvironment.getTypeUtils();
        receiverMap = new HashMap<>(16);
        typeElementMap = new HashMap<>(16);
    }
    /**
     * Annotation Processor扫描出的结果会存储进roundEnvironment中，可以在这里获取到注解内容，编写你的操作逻辑。
     * 注意:process()函数中不能直接进行异常抛出,否则程序会异常崩溃
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //生成广播
        genEventReceiver(roundEnvironment);
        return true;

    }

    /**
     * 用于指定自定义注解处理器(Annotation Processor)是注册给哪些注解的(Annotation),
     * 注解(Annotation)指定必须是完整的包名+类名
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotationTypes = new LinkedHashSet<>();
        supportedAnnotationTypes.add(EventBroadcastReceiver.class.getCanonicalName());
        return supportedAnnotationTypes;
    }

    /**
     * 用于指定你的java版本，一般返回：SourceVersion.latestSupported()
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 生成广播
     * @param env
     */
    private void genEventReceiver(RoundEnvironment env) {
        if (env == null) {
            return;
        }
        erasedTargetNames.clear();
        typeElementMap.clear();
        //遍历所有注解为EventBroadcastReceiver的Element
        for (Element element : env.getElementsAnnotatedWith(EventBroadcastReceiver.class)) {
            if (element.getKind() != ElementKind.METHOD) {
                return;
            }
            String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
            messager.printMessage(Diagnostic.Kind.WARNING, "packageName:" + packageName);
            //TypeElement 类或接口
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();
            //得到类名字
            String className = typeElement.getSimpleName().toString();
            //ExecutableElement 方法
            ExecutableElement executableElement = (ExecutableElement) element;
            //对修饰符进行判断
            Set<Modifier> modifierSet = executableElement.getModifiers();
            String methodName = executableElement.getSimpleName().toString();
            if (modifierSet != null && !modifierSet.contains(Modifier.PUBLIC)) {
                throw new IllegalArgumentException(String.format("%s.%s method %s's modifier must be public!", packageName, className, methodName));
            }
            EventBroadcastReceiver annotation = executableElement.getAnnotation(EventBroadcastReceiver.class);
            String[] actions = annotation.actions();
            boolean isActive = annotation.isActive();
            //没有任何的事件也是无意义的
            if (actions.length == 0) {
                throw new IllegalArgumentException(String.format("%s.%s method %s's actions.length must be >0!", packageName, className, methodName));
            }
            List<? extends VariableElement> params = executableElement.getParameters();
            int paramsCount = checkReceiverParams(params);
            //检查参数合法性
            if (paramsCount < 0) {
                throw new IllegalArgumentException(String.format("%s.%s method %s's params must be %s(),%s(Intent) or %s(Context, Intent)",
                        packageName, className, methodName, methodName, methodName, methodName));
            }
            String key = packageName + className;
            List<ReceiverMethod> methods;
            if (receiverMap.containsKey(key)) {
                methods = receiverMap.get(key);
            } else {
                methods = new ArrayList<>();
                receiverMap.put(key, methods);
            }
            erasedTargetNames.add(typeElement);
            typeElementMap.put(key, typeElement);
            ReceiverMethod method = new ReceiverMethod();
            method.setActions(actions);
            method.setPackageName(packageName);
            method.setClassName(className);
            method.setActive(isActive);
            method.setParamsCount(paramsCount);
            method.setMethodName(methodName);
            method.setTypeName(ParameterizedTypeName.get(typeElement.asType()));

            methods.add(method);

        }
        genReceiverFile();

    }

    /**
     * 生成receiver文件
     */
    private void genReceiverFile() {
        if (receiverMap.size() == 0) {
            return;
        }
        for (Map.Entry<String, List<ReceiverMethod>> entry : receiverMap.entrySet()) {
            List<ReceiverMethod> methods = entry.getValue();
            if (methods == null || methods.isEmpty()) {
                continue;
            }
            boolean mutiMethod = methods.size() > 1;
            ReceiverMethod m = methods.get(0);
            TypeName typeName = m.getTypeName();
            String className = m.getClassName();
            String packageName = m.getPackageName();
            ParameterSpec parameterSpec = ParameterSpec.builder(typeName, "object").build();
            ClassName returnClassName = ClassName.get(PKG_RECEIVER, "EventReceiver");
            ClassName intentFilterClassName = ClassName.get(PKG_CONTENT, "IntentFilter");
            ClassName receiverClassName = ClassName.get(PKG_RECEIVER, "CommonBroadcastReceiver");

            //构造类对象
            //weakBuilder =  private SoftReference<MainActivity> softReference;
            FieldSpec.Builder weakBuilder = FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(SoftReference.class), typeName), "softReference", Modifier.PRIVATE);
            //public class MainActivity_Receiver
            TypeSpec.Builder typeSpec = TypeSpec.classBuilder(className + RECEIVER_SUFFIX)
                    .addModifiers(Modifier.PUBLIC)
                    .addField(weakBuilder.build());
            boolean hasParent = false;
            TypeElement typeElement = typeElementMap.get(entry.getKey());
            if (typeElement != null) {
                TypeElement parentType = findParentType(typeElement, erasedTargetNames);
                if (parentType != null) {
                    hasParent = true;
                    String pClassName = parentType.getSimpleName().toString();
                    String pPackageName = elementUtils.getPackageOf(parentType).getQualifiedName().toString();
                    // extends
                    typeSpec.superclass(ClassName.get(pPackageName, pClassName + RECEIVER_SUFFIX));
                }
            }

            // implements
            if (!hasParent) {
                typeSpec.addSuperinterface(ClassName.get(PKG_RECEIVER, "OnReceiveListener"));
                typeSpec.addSuperinterface(ClassName.get(PKG_RECEIVER, "IReceiverLifecycleController"));
            }

            /**
             *  @Override
             *  public void onReceive(Context context, Intent intent)
             */
            MethodSpec.Builder listenerMethod = MethodSpec.methodBuilder("onReceive")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.get(PKG_CONTENT, "Context"), "context")
                    .addParameter(ClassName.get(PKG_CONTENT, "Intent"), "intent")
                    .returns(void.class);

            if (hasParent) {
                listenerMethod.addStatement("super.$L( $L, $L)", "onReceive", "context", "intent");
            }

            //判断Intent为null的话直接返回
            /**
             * if(intent == null) {
             *       return;
             *     }
             */
            listenerMethod.beginControlFlow("if(intent == null)");
            listenerMethod.addStatement("return");
            listenerMethod.endControlFlow();
            /**
             * if(softReference == null || softReference.get() == null) {
             *       return;
             *     }
             */
            listenerMethod.beginControlFlow("if($L == null || $L.get() == null)", "softReference", "softReference");
            listenerMethod.addStatement("return");
            listenerMethod.endControlFlow();
            List<ReceiverMethod> activeList = new ArrayList<>();
            List<ReceiverMethod> nomalList = new ArrayList<>();
            for (ReceiverMethod method : methods) {
                if (method.isActive()) {
                    activeList.add(method);
                } else {
                    nomalList.add(method);
                }
            }

            if (activeList.size() == 0) {
                for (ReceiverMethod method : nomalList) {
                    int paramsCount = method.getParamsCount();
                    String methodName = method.getMethodName();
                    addReceiveStatement(paramsCount, methodName, listenerMethod, mutiMethod);
                }
            } else {
                listenerMethod.beginControlFlow("if(isActive)");
                for (ReceiverMethod method : activeList) {
                    int paramsCount = method.getParamsCount();
                    String methodName = method.getMethodName();
                    addReceiveStatement(paramsCount, methodName, listenerMethod, mutiMethod);
                }
                listenerMethod.endControlFlow();
                listenerMethod.beginControlFlow("else");
                for (ReceiverMethod method : nomalList) {
                    int paramsCount = method.getParamsCount();
                    String methodName = method.getMethodName();
                    addReceiveStatement(paramsCount, methodName, listenerMethod, mutiMethod);
                }
                listenerMethod.endControlFlow();
            }

            /**
             * public EventReceiver buildReceiver(MainActivity object) {
             *     softReference = new SoftReference<>(object);
             */
            MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("buildReceiver")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(returnClassName)
                    .addParameter(parameterSpec)
                    .addStatement("$L = new SoftReference<>($L)", "softReference", "object");


            Set<String> mergeActions = new HashSet<>();
            for (ReceiverMethod method : methods) {
                String[] mActions = method.getActions();
                Set<String> methodActions = new HashSet<>(Arrays.asList(mActions));
                mergeActions.addAll(methodActions);
                if (mutiMethod) {
                    String methodName = method.getMethodName();
                    FieldSpec.Builder setBuilder = FieldSpec.builder(ParameterizedTypeName.get(ClassName.get("java.util", "HashSet"), ClassName.get(String.class)), methodName + "Set", Modifier.PRIVATE);
                    typeSpec.addField(setBuilder.build());
                    methodSpec.addStatement("$LSet= new HashSet<String>()", methodName);
                    for (String action : methodActions) {
                        if (action == null || action.isEmpty()) {
                            continue;
                        }
                        methodSpec.addStatement("$LSet.add($S)", methodName, action);
                    }
                }
            }

            if (hasParent) {
                methodSpec.addStatement("$T $L = super.$L($L)", returnClassName, "eventReceiver", "buildReceiver", "object");
                methodSpec.addStatement("$T $L = $L.getIntentFilter()", intentFilterClassName, "intentFilter", "eventReceiver");
            } else {
                //IntentFilter intentFilter = new IntentFilter();
                methodSpec.addStatement("$T intentFilter = new IntentFilter()", intentFilterClassName);
            }

            for (String action : mergeActions) {
                if (action == null || action.isEmpty()) {
                    continue;
                }
//                intentFilter.addAction("logout");
                methodSpec.addStatement("intentFilter.addAction($S)", action);
            }
            if (hasParent) {
                methodSpec.addStatement("return $L", "eventReceiver");
            } else {
                /**
                 *  CommonBroadcastReceiver commonBroadcastReceiver = CommonBroadcastReceiver.getBroadcastReceiver(this);
                 *     return new EventReceiver(commonBroadcastReceiver, intentFilter, this);
                 */
                methodSpec.addStatement("$T commonBroadcastReceiver = CommonBroadcastReceiver.getBroadcastReceiver(this)", receiverClassName);
                methodSpec.addStatement("return new EventReceiver($L, $L, $L)", "commonBroadcastReceiver", "intentFilter", "this");
            }

            /**
             * @Override
             *   public void changeLifecycleState(boolean isActive) {
             *     this.isActive = isActive;
             *   }
             */
            FieldSpec.Builder isActiveFiled = FieldSpec.builder(boolean.class, "isActive", Modifier.PRIVATE)
                    .initializer("false");
            MethodSpec.Builder lifecycleBuilder = MethodSpec.methodBuilder("changeLifecycleState")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addAnnotation(Override.class)
                    .addParameter(boolean.class, "isActive")
                    .addStatement("this.isActive = isActive");
            if (hasParent) {
                lifecycleBuilder.addStatement("super.$L($L)", "changeLifecycleState", "isActive");
            }

            //添加 变量 方法
            typeSpec.addField(isActiveFiled.build())
                    .addMethod(methodSpec.build())
                    .addMethod(listenerMethod.build())
                    .addMethod(lifecycleBuilder.build());

            //生成文件
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec.build())
                    .build();

            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
                messager.printMessage(Diagnostic.Kind.NOTE, "err:" + e.getMessage());
            }
        }

        receiverMap.clear();
        erasedTargetNames.clear();
        typeElementMap.clear();
    }

    private void addReceiveStatement(int paramsCount, String methodName, MethodSpec.Builder listenerMethod, boolean mutiMethod) {
        if (mutiMethod) {
            listenerMethod.beginControlFlow("if($LSet.contains(intent.getAction()))", methodName);
            if (paramsCount == 0) {
                listenerMethod.addStatement("$L.get().$L()", "softReference", methodName);
            } else if (paramsCount == 1) {
                listenerMethod.addStatement("$L.get().$L($L)", "softReference", methodName, "intent");
            } else if (paramsCount == 2) {
                listenerMethod.addStatement("$L.get().$L($L,$L)", "softReference", methodName, "context", "intent");
            }
            listenerMethod.endControlFlow();
        } else {
            /**
             * softReference.get().onReceive(intent);
             */
            if (paramsCount == 0) {
                listenerMethod.addStatement("$L.get().$L()", "softReference", methodName);
            } else if (paramsCount == 1) {
                listenerMethod.addStatement("$L.get().$L($L)", "softReference", methodName, "intent");
            } else if (paramsCount == 2) {
                listenerMethod.addStatement("$L.get().$L($L,$L)", "softReference", methodName, "context", "intent");
            }
        }
    }

    private TypeElement findParentType(TypeElement typeElement, Set<TypeElement> parents) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            if (parents.contains(typeElement)) {
                return typeElement;
            }
        }
    }

    /**
     * 检查receiver的方法的参数
     *
     * @param params 方法参数集合
     * @return -1，无效参数，0，1，2
     */
    private int checkReceiverParams(List<? extends VariableElement> params) {
        if (params == null || params.size() == 0) {
            //无参
            return 0;
        } else if (params.size() == 1) {
            //只有一个参数，那么必须是Intent
            VariableElement variableElement = params.get(0);
            if (!TYPE_INTENT.equals(variableElement.asType().toString())) {
                return -1;
            }
            return 1;
        } else if (params.size() == 2) {
            VariableElement variableElement = params.get(0);
            VariableElement variableElement1 = params.get(1);
            if (!TYPE_CONTEXT.equals(variableElement.asType().toString()) ||
                    !TYPE_INTENT.equals(variableElement1.asType().toString())) {
                return -1;
            }
            return 2;
        }
        //超过两个参数
        return -1;
    }

}
