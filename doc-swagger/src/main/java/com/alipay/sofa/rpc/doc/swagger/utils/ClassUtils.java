/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.doc.swagger.utils;

import io.swagger.util.ReflectionUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * copy from spring-core
 *
 * Miscellaneous class utility methods.
 * Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @see ReflectionUtils
 * @since 1.1
 */
public abstract class ClassUtils {

    /**
     * Suffix for array class names: "[]"
     */
    public static final String                   ARRAY_SUFFIX               = "[]";

    /**
     * Prefix for internal array class names: "["
     */
    private static final String                  INTERNAL_ARRAY_PREFIX      = "[";

    /**
     * Prefix for internal non-primitive array class names: "[L"
     */
    private static final String                  NON_PRIMITIVE_ARRAY_PREFIX = "[L";

    /**
     * The package separator character: '.'
     */
    private static final char                    PACKAGE_SEPARATOR          = '.';

    /**
     * The inner class separator character: '$'
     */
    private static final char                    INNER_CLASS_SEPARATOR      = '$';

    /**
     * Map with primitive wrapper type as key and corresponding primitive
     * type as value, for example: Integer.class -> int.class.
     */
    private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap    = new IdentityHashMap<Class<?>, Class<?>>(8);

    /**
     * Map with primitive type as key and corresponding wrapper
     * type as value, for example: int.class -> Integer.class.
     */
    private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap  = new IdentityHashMap<Class<?>, Class<?>>(8);

    /**
     * Map with primitive type name as key and corresponding primitive
     * type as value, for example: "int" -> "int.class".
     */
    private static final Map<String, Class<?>>   primitiveTypeNameMap       = new HashMap<String, Class<?>>(32);

    /**
     * Map with common "java.lang" class name as key and corresponding Class as value.
     * Primarily for efficient deserialization of remote invocations.
     */
    private static final Map<String, Class<?>>   commonClassCache           = new HashMap<String, Class<?>>(32);

    static {
        primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
        primitiveWrapperTypeMap.put(Byte.class, byte.class);
        primitiveWrapperTypeMap.put(Character.class, char.class);
        primitiveWrapperTypeMap.put(Double.class, double.class);
        primitiveWrapperTypeMap.put(Float.class, float.class);
        primitiveWrapperTypeMap.put(Integer.class, int.class);
        primitiveWrapperTypeMap.put(Long.class, long.class);
        primitiveWrapperTypeMap.put(Short.class, short.class);

        for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
            primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
            registerCommonClasses(entry.getKey());
        }

        Set<Class<?>> primitiveTypes = new HashSet<Class<?>>(32);
        primitiveTypes.addAll(primitiveWrapperTypeMap.values());
        primitiveTypes.addAll(Arrays.asList(new Class<?>[] {
            boolean[].class, byte[].class, char[].class, double[].class,
            float[].class, int[].class, long[].class, short[].class }));
        primitiveTypes.add(void.class);
        for (Class<?> primitiveType : primitiveTypes) {
            primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
        }

        registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
            Float[].class, Integer[].class, Long[].class, Short[].class);
        registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
            Object.class, Object[].class, Class.class, Class[].class);
        registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
            Error.class, StackTraceElement.class, StackTraceElement[].class);
    }

    /**
     * Register the given common classes with the ClassUtils cache.
     */
    private static void registerCommonClasses(Class<?>... commonClasses) {
        for (Class<?> clazz : commonClasses) {
            commonClassCache.put(clazz.getName(), clazz);
        }
    }

    /**
     * Return the default ClassLoader to use: typically the thread context
     * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
     * class will be used as fallback.
     * <p>Call this method if you intend to use the thread context ClassLoader
     * in a scenario where you clearly prefer a non-null ClassLoader reference:
     * for example, for class path resource loading (but not necessarily for
     * {@code Class.forName}, which accepts a {@code null} ClassLoader
     * reference as well).
     *
     * @return the default ClassLoader (only {@code null} if even the system
     * ClassLoader isn't accessible)
     * @see Thread#getContextClassLoader()
     * @see ClassLoader#getSystemClassLoader()
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = ClassUtils.class.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }

    /**
     * Replacement for {@code Class.forName()} that also returns Class instances
     * for primitives (e.g. "int") and array class names (e.g. "String[]").
     * Furthermore, it is also capable of resolving inner class names in Java source
     * style (e.g. "java.lang.Thread.State" instead of "java.lang.Thread$State").
     *
     * @param name        the name of the Class
     * @param classLoader the class loader to use
     *                    (may be {@code null}, which indicates the default class loader)
     * @return Class instance for the supplied name
     * @throws ClassNotFoundException if the class was not found
     * @throws LinkageError           if the class file could not be loaded
     * @see Class#forName(String, boolean, ClassLoader)
     */
    public static Class<?> forName(String name, ClassLoader classLoader) throws ClassNotFoundException, LinkageError {

        Class<?> clazz = resolvePrimitiveClassName(name);
        if (clazz == null) {
            clazz = commonClassCache.get(name);
        }
        if (clazz != null) {
            return clazz;
        }

        // "java.lang.String[]" style arrays
        if (name.endsWith(ARRAY_SUFFIX)) {
            String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[Ljava.lang.String;" style arrays
        if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[[I" or "[[Ljava.lang.String;" style arrays
        if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
            String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        ClassLoader clToUse = classLoader;
        if (clToUse == null) {
            clToUse = getDefaultClassLoader();
        }
        try {
            return (clToUse != null ? clToUse.loadClass(name) : Class.forName(name));
        } catch (ClassNotFoundException ex) {
            int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
            if (lastDotIndex != -1) {
                String innerClassName =
                        name.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
                try {
                    return (clToUse != null ? clToUse.loadClass(innerClassName) : Class.forName(innerClassName));
                } catch (ClassNotFoundException ex2) {
                    // Swallow - let original exception get through
                }
            }
            throw ex;
        }
    }

    /**
     * Resolve the given class name into a Class instance. Supports
     * primitives (like "int") and array class names (like "String[]").
     * <p>This is effectively equivalent to the {@code forName}
     * method with the same arguments, with the only difference being
     * the exceptions thrown in case of class loading failure.
     *
     * @param className   the name of the Class
     * @param classLoader the class loader to use
     *                    (may be {@code null}, which indicates the default class loader)
     * @return Class instance for the supplied name
     * @throws IllegalArgumentException if the class name was not resolvable
     *                                  (that is, the class could not be found or the class file could not be loaded)
     * @see #forName(String, ClassLoader)
     */
    public static Class<?> resolveClassName(String className, ClassLoader classLoader) throws IllegalArgumentException {
        try {
            return forName(className, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Cannot find class [" + className + "]", ex);
        } catch (LinkageError ex) {
            throw new IllegalArgumentException(
                "Error loading class [" + className + "]: problem with class file or dependent class.", ex);
        }
    }

    /**
     * Resolve the given class name as primitive class, if appropriate,
     * according to the JVM's naming rules for primitive classes.
     * <p>Also supports the JVM's internal class names for primitive arrays.
     * Does <i>not</i> support the "[]" suffix notation for primitive arrays;
     * this is only supported by {@link #forName(String, ClassLoader)}.
     *
     * @param name the name of the potentially primitive class
     * @return the primitive class, or {@code null} if the name does not denote
     * a primitive class or primitive array class
     */
    public static Class<?> resolvePrimitiveClassName(String name) {
        Class<?> result = null;
        // Most class names will be quite long, considering that they
        // SHOULD sit in a package, so a length check is worthwhile.
        if (name != null && name.length() <= 8) {
            // Could be a primitive - likely.
            result = primitiveTypeNameMap.get(name);
        }
        return result;
    }

}
