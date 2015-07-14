package com.uach.safedelete;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

/**
 * Play! module to add the method {@code getReferencedBy} to a model, where said
 * model:
 * <ul>
 * <li>is a subclass of
 * <pre>{@code extends play.modules.morphia.Model}</pre>
 * </li>
 * <li>is annotated with
 * <pre>{@code com.google.code.morphia.annotations.Entity}</pre>
 * </li>
 * <li>is located in the package
 * <pre>{@code models}</pre>
 * </li>
 * </ul>
 * Said method allows a model to access an array of classes ({@code Class[]}) of
 * other models that contain at least 1 reference to the former class.
 * 
*/
public class SafeDeleteEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {

        final CtClass ctModelClass = classPool.getCtClass("play.modules.morphia.Model");
        final CtClass ctClass = makeClass(applicationClass);

        final String entityFullyQualifiedName = "com.google.code.morphia.annotations.Entity";
        final String modelKeyWord = "models";

        //Only if current class is in the package "models"
        if (!ctClass.getName().startsWith(modelKeyWord)) {
            return;
        }
        //Only for morphia's Model
        if (!ctClass.subtypeOf(ctModelClass)) {
            return;
        }
        //Only if annotated with morphia's @Entity
        if (!hasAnnotation(ctClass, entityFullyQualifiedName)) {
            return;
        }

        final String METHOD_NAME = "getReferencedBy";

        //Verify that the method isn't already declared in the class
        try {
            CtMethod ctMethod = ctClass.getDeclaredMethod(METHOD_NAME);
            if (METHOD_NAME.equals(ctMethod.getName()) && ctMethod.getDeclaringClass().equals(ctClass)) {
                throw new Exception(String.format("Cannot enhance class %s with SafeDelete; method %s is already defined.", ctClass.getName(), METHOD_NAME));
            }
        } catch (NotFoundException noReferencedByMethod) {
        }

        //Load all available classes
        List<ApplicationClass> allClasses = Play.classes.all();

        List<ApplicationClass> classesThatReferenceCurrentClass = allClasses.stream()
                .filter(
                        (appClass) -> {
                            try {
                                //Verify the class's FullyQualifiedName; should start with "models"
                                if (!appClass.name.startsWith(modelKeyWord)) {
                                    return false;
                                }
                                CtClass tmpCtClass = makeClass(appClass);
                                //Only for morphia's Model
                                Boolean referencesCurrentClass = tmpCtClass.subtypeOf(ctModelClass)
                                //Only if annotated with morphia's @Entity
                                && hasAnnotation(tmpCtClass, entityFullyQualifiedName)
                                //Check its fields for a reference to the current class
                                && Arrays.asList(tmpCtClass.getDeclaredFields()).stream()
                                .anyMatch(
                                        (ctField) -> {
                                            try {
                                                return checkCtFieldForCtClassReference(ctField, ctClass);
                                            } catch (NotFoundException e) {
                                                return false;
                                            }
                                        }
                                );
                                //Detatch from clasPool
                                tmpCtClass.detach();

                                return referencesCurrentClass;
                            } catch (IOException | NotFoundException | ClassNotFoundException e) {
                                return false;
                            }
                        }
                )
                .collect(Collectors.toList());

        //Create method getReferencedBy using the classes that reference the current class
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

    /**
     * @deprecated @param classesToConcatenate -
     * @return -
     */
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

    /**
     * @deprecated @param classes -
     * @return -
     */
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

    /**
     * Creates the body of a method that returns a {@code Class[]} of all the
     * classes defined by <i>classes</i>.
     *
     * @param classes {@code List<ApplicationClass>} of classes to return.
     * @return {@code Class[]} of defined classes
     */
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
        for (int i = 0; i < classes.size(); i++) {
            sb.append(String.format("%s[%d] = %s.class;", ARRAY_NAME, i, classes.get(i).name));
            sb.append("\n");
        }
        sb.append(String.format("return %s;", ARRAY_NAME));

        return sb.toString();
    }

    /**
     * @deprecated Usar {@link hasAnnotation(CtClass, String)}
     * @param ctClass -
     * @param annotationName -
     * @return -
     */
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

    /**
     * Checks if a {@code CtField} references {@code CtClass}. If the field uses
     * generics, such as a {@code List<>}, then the method also checks the
     * field's generic signature.
     *
     * @param ctField {@code CtField} to check
     * @param ctClass {@code CtClass} to check for reference
     * @return {@code true} if the <i>ctField</i> references the <i>ctClass</i>
     * @throws NotFoundException
     */
    private Boolean checkCtFieldForCtClassReference(CtField ctField, CtClass ctClass) throws NotFoundException {
        String genericSignature = ctField.getGenericSignature();

        if (genericSignature == null) {
            //Campo sin <>
            return ctField.getType().getName().equals(ctClass.getName());
        } else {
            //Campo con <>
            return genericSignature.contains(ctClass.getName().replace(".", "/"));
        }
    }
}
