package dev.petshopsoftware.utilities.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionUtil {
    public static List<Method> getMethodsAnnotatedWith(Class<?> clazz, Class<? extends Annotation> annotationClass){
        List<Method> methods = new LinkedList<>();
        while (clazz != null && clazz != Object.class) {
            methods.addAll(
                    Stream.of(clazz.getDeclaredMethods())
                            .filter(method -> method.isAnnotationPresent(annotationClass))
                            .collect(Collectors.toList())
            );
            for (Class<?> interfaceClass : clazz.getInterfaces())
                methods.addAll(getMethodsAnnotatedWith(interfaceClass, annotationClass));
            clazz = clazz.getSuperclass();
        }
        return methods;
    }
}
