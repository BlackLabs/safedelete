package com.uach.safedelete;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

/**
 * Agrega un método {@code Set<Class<?>>} llamado
 * <i>getReferencedBy</i> a las clases modelos de Morphia (anotadas con
 * {@link Entity}), que devuelve un arreglo con las clases que hacen referencia a esta clase. Además,
 * agrega un método para acceder al Set creado
 * 
*/
public class SafeDeleteEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {

        final CtClass ctModelClass = classPool.getCtClass("play.modules.morphia.Model");
        final CtClass ctClass = makeClass(applicationClass);

        final String entityFullyQualifiedName = "com.google.code.morphia.annotations.Entity";

        //Sólo para modelos
        if (!ctClass.subtypeOf(ctModelClass)) {
            return;
        }
        //Sólo si tiene la anotación de @Entity de morphia
        if (!checkCtClassForAnnotation(ctClass, entityFullyQualifiedName)) {
            return;
        }

        final String METHOD_NAME = "getReferencedBy";

        //Verificar la no-existencia del método
        try {
            CtMethod ctMethod = ctClass.getDeclaredMethod(METHOD_NAME);
            if (METHOD_NAME.equals(ctMethod.getName()) && ctMethod.getDeclaringClass().equals(ctClass)) {
                //return;
                throw new Exception(String.format("Error al realizar enhance de SafeDelete sobre la clase %s, el método %s ya se encuentra definido.", ctClass.getName(), METHOD_NAME));
            }
        } catch (NotFoundException noReferencedByMethod) {
        }

        //Cargar todas las clases disponibles
        List<ApplicationClass> allClasses = Play.classes.all();

        //Filtrar para obtener aquellas clases que hacen referencia a la actual
        List<ApplicationClass> classesThatReferenceCurrentClass = allClasses.stream()
                .filter(
                        (appClass) -> {
                            try {
                                //Crear CtClass temporal
                                CtClass tmpCtClass = makeClass(appClass);
                                //Debe extender al Model de morphia
                                return tmpCtClass.subtypeOf(ctModelClass)
                                //Debe tener tambíen la anotación
                                && checkCtClassForAnnotation(tmpCtClass, entityFullyQualifiedName)
                                //Debe tener algún campo que haga referencia a la clase actual
                                && Arrays.asList(tmpCtClass.getDeclaredFields()).stream()
                                .anyMatch(
                                        (ctField) -> {
                                            try {
                                                return ctField.getType().getName().equals(ctClass.getName());
                                            } catch (NotFoundException e) {
                                                return false;
                                            }
                                        }
                                );
                            } catch (IOException | NotFoundException e) {
                                return false;
                            }
                        }
                )
                .collect(Collectors.toList());

        String referencedByGetter = String.format(
                "public static Class[] %s(){\n"
                + "    %s"
                + "}",
                METHOD_NAME,
                generateGetterBodyArray(classesThatReferenceCurrentClass)
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
        sb.append(String.format("return %s;", ARRAY_NAME));

        return sb.toString();
    }

    private Boolean checkCtClassForAnnotation(CtClass ctClass, String annotationName) {
        Object[] annotations;
        try {
            annotations = ctClass.getAnnotations();
        } catch (ClassNotFoundException ex) {
            return false;
        }
        for (Object annotation : annotations) {
            if (annotation.toString().contains(annotationName)) {
                return true;
            }
        }
        return false;
    }
}
