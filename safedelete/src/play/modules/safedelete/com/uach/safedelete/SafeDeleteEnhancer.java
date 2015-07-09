package com.uach.safedelete;

//import com.google.code.morphia.annotations.Entity;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import org.reflections.Reflections;
import play.Logger;
import play.Play;
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

        final CtClass modelClass = classPool.getCtClass("play.modules.morphia.Model");
//        classPool.getClassLoader().

        final CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.hasAnnotation(SafeDelete.class)) {
            return;
        } else {
            System.out.println("... si tiene la anotacion safedelete! :D <3");
        }

//        if (!applicationClass.javaClass.isAssignableFrom(modelClass.getClass())) {
//            return;
//        }
//        if (!applicationClass.javaClass.isAnnotationPresent(SafeDelete.class)) {
//            return;
//        }
        final String FIELD_NAME = "referencedBy";
        final String METHOD_NAME = "getReferencedBy";
        final String PROJECT_PREFIX = "models";
        final String PROJECT_PREFIX2 = "";

        final String setClassFullyQualifiedName = "java.util.Set";
        final String hashSetClassFullyQualifiedName = "java.util.HashSet";
        final String arraysClassFullyQualifiedName = "java.util.Arrays";
        final CtClass hashSetClass = classPool.getCtClass("java.util.HashSet");
        final CtClass objectClass = classPool.getCtClass("java.lang.Object");
//        final Class currentClass = ctClass.toClass();
//        System.out.println("currentClass = " + currentClass);
//        final Class currentClass = Class.forName(ctClass.getName());

//        if (ctClass.getAnnotation(EN.class) == null) {
//            return;
//        }
        try {
//            CtField ctField = ctClass.getDeclaredField(ATTRIBUTE_NAME);
            CtMethod ctMethod = ctClass.getDeclaredMethod(METHOD_NAME);
            if (METHOD_NAME.equals(ctMethod.getName()) && ctMethod.getDeclaringClass().equals(ctClass)) {
                throw new Exception(String.format("Error al realizar enhance de SafeDelete sobre la clase %s, el método %s ya se encuentra previamente definido.", ctClass.getName(), METHOD_NAME));
            }
        } catch (NotFoundException noReferencedByMethod) {
        }

//        ctClass.defrost();
//        ctClass.stopPruning(true);

//        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(SafeDelete.class);
        Set<Class<?>> annotatedClasses = new HashSet<>();

//        Play.classloader.
        List<ApplicationClass> allClasses = Play.classes.all();
        System.out.println("allClasses.size() = " + allClasses.size());
        
        System.out.println("Play.classes.all().size() = " + Play.classes.all().size());

        List<ApplicationClass> candidateClasses = allClasses.stream()
                .filter(
                        (appClass) -> {
//                            return clazz.(SafeDelete.class);
                            try {
                                return makeClass(appClass).hasAnnotation(SafeDelete.class);
                            } catch (IOException e) {
                                System.out.println("IOException, returning false :C");
                                System.out.println(e.getCause());
                                return false;
                            }
                        }
                )
                .collect(Collectors.toList());
        System.out.println("candidateClasses.size() = " + candidateClasses.size());
        candidateClasses = candidateClasses.stream()
                .filter(
                        (appClass) -> {
                            try {
                                return Arrays.asList(makeClass(appClass).getDeclaredFields()).stream()
                                .anyMatch(
                                        (field) -> {
                                            try {
                                                return field.getType().getName().equals(ctClass.getName());
                                            } catch (NotFoundException notFoundException) {
                                                System.out.println("NotFoundException, returning false :C");
                                                System.out.println(notFoundException.getCause());
                                                return false;
                                            }
                                        }
                                );
                                
                            } catch (IOException  e) {
                                System.out.println("IOException, returning false :C");
                                System.out.println(e.getCause());
                                return false;
                            }
                        }
                )
                .collect(Collectors.toList());

        System.out.println("candidateClasses.size() = " + candidateClasses.size());
        System.out.println("allClasses.size() = " + allClasses.size());

//        Reflections reflections2 = new Reflections(PROJECT_PREFIX2);
//        Set<Class<?>> annotatedClasses2 = reflections.getSubTypesOf(Object.class);
//        Set<Class<?>> annotatedClasses3 = reflections.getSubTypesOf(Object.class);
//
//        System.out.println("annotatedClasses2.size() = " + annotatedClasses2.size());
//        System.out.println("annotatedClasses3.size() = " + annotatedClasses3.size());
//        for (Class<?> class1 : annotatedClasses2) {
//            System.out.println("class1 = " + class1);
//        }
//        String[] testoooo = new String[]{"app", "SGCG", "SGCG.app", "models", "controllers", "app.models", "app.controllers", "tmp", "bytecode", "classes", "tmp.bytecode", "tmp.classes", "tmp.classes.models"};
//        
//        
//        for (String ttt : testoooo) {
//            System.out.println("new Reflections(\""+ ttt +"\").getSubTypesOf(Object.class) = " + new Reflections(ttt).getSubTypesOf(Object.class).size());
//        }
//        Set<Class<?>> annotatedClasses = new HashSet<>();
//        System.out.println("annotatedClasses = " + annotatedClasses);
//        annotatedClasses = annotatedClasses.stream()
//                .filter(
//                        (clazz) -> {
//                            return Arrays.asList(clazz.getDeclaredFields()).stream()
//                            .anyMatch(
//                                    (field) -> {
//                                        return field.getType().isAssignableFrom(currentClass);
//                                    }
//                            );
//                        }
//                )
//                .collect(Collectors.toSet());
//        annotatedClasses = allClasses.stream()
//                .filter(
//                        (clazz) -> {
//                            return Arrays.asList(clazz.getDeclaredFields()).stream()
//                            .anyMatch(
//                                    (field) -> {
//                                        return field.getType().getName().equals(ctClass.getName());
//                                    }
//                            );
//                        }
//                )
//                .collect(Collectors.toSet());
//        System.out.println("annotatedClasses.size() = " + annotatedClasses.size());
        System.out.println("/......////////////////asdfasdfasdfsadfasdf");
        System.out.println("ctClass.getName() = " + ctClass.getName());
        System.out.println("annotatedClasses = " + annotatedClasses);
        System.out.println("concatenateClassesFQN(annotatedClasses) = " + concatenateClassesFQN(annotatedClasses));
        System.out.println("");

//        ctClass.defrost();
//        CtField referencedByField = new CtField(hashSetClass, FIELD_NAME, ctClass);
////        CtField referencedByField = new CtField(objectClass, FIELD_NAME, ctClass);
//        referencedByField.setModifiers(Modifier.PUBLIC);
//        referencedByField.setModifiers(Modifier.STATIC);
//        referencedByField.setModifiers(Modifier.FINAL);
//        CtField.Initializer referencedByInitializer = CtField.Initializer.byExpr(
//                String.format("new %s<Class<Object>>(%s.asList(new Class<Object>[]{%s}));",
//                        hashSetClassFullyQualifiedName,
//                        arraysClassFullyQualifiedName,
//                        concatenateClassesFQN(annotatedClasses)
//                )
//        );
//        ctClass.addField(referencedByField, referencedByInitializer);
//        ctClass.addField(referencedByField);
//            String referencedByGetter = String.format(
//                    "public %s<Class<?>> getReferencedBy(){ return (%s) referencedBy; }",
//                    setClassFullyQualifiedName,
//                    hashSetClassFullyQualifiedName
//            );
        //Versión anterior
//        String referencedByGetter = String.format(
//                "public static %s<Class<Object>> %s(){"
//                + "    return new %s<Class<Object>>(%s.asList(new Class<Object>[]{%s}));"
//                + "}",
//                setClassFullyQualifiedName,
//                METHOD_NAME,
//                hashSetClassFullyQualifiedName,
//                arraysClassFullyQualifiedName,
//                concatenateClassesFQN(annotatedClasses)
//        );
//        String referencedByGetter = String.format(
//                "public static %s<Class<?>> %s(){"
//                + "    return %s;"
//                + "}",
//                setClassFullyQualifiedName,
//                METHOD_NAME,
//                FIELD_NAME
//        );
//        String referencedByGetter = String.format(
//                "public static Object %s(){"
//                + "    return new %s<Class<?>>(%s.asList(new Class<?>[]{%s}));"
//                + "}",
//                METHOD_NAME,
//                hashSetClassFullyQualifiedName,
//                arraysClassFullyQualifiedName,
//                concatenateClassesFQN(annotatedClasses)
//        );
        String referencedByGetter = String.format(
                "public static Object %s(){"
                + "    %s"
                + "}",
                METHOD_NAME,
                generateGetterBodyArray(candidateClasses)
        );

        System.out.println("referencedByGetter!!!");
        System.out.println(referencedByGetter);

        CtMethod getReferencedByMethod = CtMethod.make(referencedByGetter, ctClass);

        ctClass.addMethod(getReferencedByMethod);

        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
//        ctClass.freeze();
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

    private String generateGetterBodySet(Set<Class<?>> classes) {

        final String setClassFullyQualifiedName = "java.util.Set";
        final String hashSetClassFullyQualifiedName = "java.util.HashSet";
        final String SET_NAME = "classSet";

        StringBuilder sb = new StringBuilder();

        sb.append(
                String.format(
                        "%s %s = new %s<Object>();",
                        setClassFullyQualifiedName,
                        SET_NAME,
                        hashSetClassFullyQualifiedName
                )
        );
        sb.append("\n");
        sb.append(classes.stream()
                .map(
                        (clazz) -> {
                            String statement = String.format(
                                    "%s.add(%s.class);",
                                    SET_NAME,
                                    clazz.getName()
                            );

                            return statement;

                        }
                )
                .collect(Collectors.joining("\n")));
        sb.append("\n");
        sb.append(String.format("return %s;", SET_NAME));

        return sb.toString();
    }

    private String generateGetterBodyArray(List<ApplicationClass> classes) {

        final String ARRAY_NAME = "classArray";
        final int ARRAY_SIZE = classes.size();

        StringBuilder sb = new StringBuilder();

        sb.append(
                String.format(
                        "%s %s = new %s[%d];",
                        "java.lang.Class[]",
                        ARRAY_NAME,
                        "java.lang.Class",
                        ARRAY_SIZE
                )
        );
        sb.append("\n");
        int i = 0;
        for (ApplicationClass clazz : classes) {
            sb.append(String.format("%s[%d] = %s.class;", ARRAY_NAME, i, clazz.name));
            sb.append("\n");

            i++;
        }
//        for (int i = 0; i < ARRAY_SIZE; i++) {
//                sb.append(String.format("%s[%d] = %s.class;", ARRAY_NAME, i));
//                sb.append("\n");
//        }
//        sb.append(classes.stream()
//                .map(
//                        (clazz) -> {
//                            String statement = String.format(
//                                    "%s.add(%s.class);",
//                                    SET_NAME,
//                                    clazz.getName()
//                            );
//
//                            return statement;
//
//                        }
//                )
//                .collect(Collectors.joining("\n")));
//        sb.append("\n");
        sb.append(String.format("return %s;", ARRAY_NAME));

        return sb.toString();
    }
}
