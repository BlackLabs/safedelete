package com.uach.safedelete;

//import com.google.code.morphia.annotations.Entity;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import org.reflections.Reflections;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

/**
 * Agrega un atributo {@code Set<Class<?>>} llamado
 * <i>referencedBy</i> a las clases modelos de Morphia (anotadas con
 * {@link Entity}), con las clases que hacen referencia a esta clase. Además,
 * agrega un método para acceder al Set creado
 * 
*/
public class SafeDeleteEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {

        final String ATTRIBUTE_NAME = "referencedBy";
        final String METHOD_NAME = "getReferencedBy";
        final String PROJECT_PREFIX = "models";

        final CtClass ctClass = makeClass(applicationClass);
        final CtClass modelClass = classPool.getCtClass("play.modules.morphia.Model");
        final String setClassFullyQualifiedName = "java.util.Set";
        final String hashSetClassFullyQualifiedName = "java.util.HashSet";
        final String arraysClassFullyQualifiedName = "java.util.Arrays";
        final CtClass hashSetClass = classPool.getCtClass("java.util.HashSet");
        final Class currentClass = ctClass.toClass();

        if (!ctClass.subclassOf(modelClass)) {
            return;
        }
        
//        if (ctClass.getAnnotation(EN.class) == null) {
//            return;
//        }

        try {
//            CtField ctField = ctClass.getDeclaredField(ATTRIBUTE_NAME);
            CtMethod ctMethod = ctClass.getDeclaredMethod(ATTRIBUTE_NAME);
            if (METHOD_NAME.equals(ctMethod.getName()) && ctMethod.getDeclaringClass().equals(ctClass)) {
                throw new Exception(String.format("Error al realizar enhance de SafeDelete sobre la clase %s, el método %s ya se encuentra previamente definido.", ctClass.getName(), METHOD_NAME));
            }
        } catch (NotFoundException noReferencedByMethod) {
        }
        
        Reflections reflections = new Reflections(PROJECT_PREFIX);

        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(SafeDelete.class);

        annotatedClasses = annotatedClasses.stream()
                .filter(
                        (clazz) -> {
                            return Arrays.asList(clazz.getDeclaredFields()).stream()
                            .anyMatch(
                                    (field) -> {
                                        return field.getType().isAssignableFrom(currentClass);
                                    }
                            );
                        }
                )
                .collect(Collectors.toSet());

//            CtField referencedByField = new CtField(hashSetClass, ATTRIBUTE_NAME, ctClass);
//            referencedByField.setModifiers(Modifier.PUBLIC);
//
//            ctClass.addField(referencedByField);
//            String referencedByGetter = String.format(
//                    "public %s<Class<?>> getReferencedBy(){ return (%s) referencedBy; }",
//                    setClassFullyQualifiedName,
//                    hashSetClassFullyQualifiedName
//            );
        String referencedByGetter = String.format(
                "public static %s<Class<?>> %s(){"
                + "    return new %s<Class<?>>(%s.asList(new Class<?>[]{%s}));"
                + "}",
                setClassFullyQualifiedName,
                METHOD_NAME,
                hashSetClassFullyQualifiedName,
                arraysClassFullyQualifiedName,
                concatenateClassesFQN(annotatedClasses)
        );

        CtMethod getReferencedByMethod = CtMethod.make(referencedByGetter, ctClass);

        ctClass.addMethod(getReferencedByMethod);
        
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

    private String concatenateClassesFQN(Set<Class<?>> classesToConcatenate) {
        String result = "";
        if (classesToConcatenate != null && !classesToConcatenate.isEmpty()) {
            result = classesToConcatenate.stream()
                    .map((clazz) -> {
                        return clazz.getName();
                    })
                    .collect(Collectors.joining(", "));
        }
        return result;
    }
}
