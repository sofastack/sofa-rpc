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
package com.alipay.sofa.rpc.common.json;

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.common.utils.CompatibleTypeUtils;
import com.alipay.sofa.rpc.common.utils.DateUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.alipay.sofa.rpc.common.json.JSON.getSerializeFields;

/**
 * Bean serializer of json
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class BeanSerializer {

    /**
     * 序列化对象
     *
     * @param bean 要序列化的对象
     * @return 序列化后的结果，可能是string，number，boolean，list或者map等
     * @throws NullPointerException 如果非空字段为空的化
     */
    public static Object serialize(Object bean) throws NullPointerException {
        return serialize(bean, false);
    }

    /**
     * 序列化对象 是否增加type标识
     *
     * @param bean    要序列化的对象
     * @param addType 是否增加类型标识
     * @return 序列化后的结果，可能是string，number，boolean，list或者map等
     * @throws NullPointerException 如果非空字段为空的话
     */
    public static Object serialize(Object bean, boolean addType) throws NullPointerException {
        if (bean == null) {
            return null;
        }
        if (bean instanceof String || bean instanceof Boolean || bean instanceof Number) {
            return bean;
        } else if (bean instanceof Collection) {
            Collection list = (Collection) bean;
            ArrayList<Object> array = new ArrayList<Object>(list.size());
            for (Object o : list) {
                array.add(serialize(o, addType));
            }
            return array;
        } else if (bean.getClass().isArray()) {
            int length = Array.getLength(bean);
            ArrayList<Object> array = new ArrayList<Object>(length);
            for (int i = 0; i < length; ++i) {
                array.add(serialize(Array.get(bean, i), addType));
            }
            return array;
        } else if (bean instanceof Map) {
            Map map = (Map) bean;
            Iterator itr = map.entrySet().iterator();
            Map.Entry entry = null;
            while (itr.hasNext()) {
                entry = (Map.Entry) itr.next();
                map.put(entry.getKey(), serialize(entry.getValue(), addType));
            }
            return map;
        } else if (bean instanceof Date) {
            return DateUtils.dateToStr((Date) bean);
            //        } else if (bean instanceof BigDecimal) {
            //            BigDecimal bigDecimal = (BigDecimal) bean;
            //            return bigDecimal.toString();
        } else if (bean instanceof Enum) {
            Enum e = (Enum) bean;
            return e.toString();
        }

        Class beanClass = bean.getClass();
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        for (Field field : getSerializeFields(beanClass)) {
            Object value = null;
            String key = field.getName();
            try {
                value = serialize(field.get(bean), addType);

                JSONField jsonField = field.getAnnotation(JSONField.class);
                if (jsonField != null) {
                    boolean isRequired = jsonField.isRequired();
                    if (value == null) {
                        if (isRequired) { // 判断是否不能为空
                            throw new NullPointerException("Field " + field.getName() + " can't be null");
                        }
                        if (jsonField.skipIfNull()) { // 判断为空是否跳过
                            continue;
                        }
                    }
                    if (!jsonField.alias().isEmpty()) {
                        key = jsonField.alias();
                    }
                }
                map.put(key, value);
            } catch (Exception e) {
                throw new RuntimeException("Read bean filed " + beanClass.getName()
                    + "." + field.getName() + " error! ", e);
            }
        }
        if (addType) {
            String typeName = beanClass.getCanonicalName();
            if (!typeName.startsWith("java.")
                && !typeName.startsWith("javax.")
                && !typeName.startsWith("sun.")) {
                map.put(JSON.CLASS_KEY, typeName);
            }
        }
        return map;
    }

    private static <K, V> Map<K, V> mapToMap(Map<K, V> src, Class<? extends Map> dstClazz) {
        if (dstClazz.isInterface()) {
            dstClazz = HashMap.class;
        }
        Map des = ClassUtils.newInstance(dstClazz);
        for (Map.Entry<K, V> entry : src.entrySet()) {
            des.put(deserialize(entry.getKey()), deserialize(entry.getValue()));
        }
        return des;
    }

    private static <T> T mapToObject(Map src, Class<T> dstClazz) {
        String actualType = (String) src.get(JSON.CLASS_KEY);
        Class realClass = actualType != null ? ClassUtils.forName(actualType) : dstClazz;
        Object bean = ClassUtils.newInstance(realClass);
        for (Field field : getSerializeFields(realClass)) {
            Object value = null;
            try {
                JSONField jsonField = field.getAnnotation(JSONField.class);
                String name = null;
                boolean isRequired = false;
                if (jsonField != null) {
                    name = jsonField.alias();
                    isRequired = jsonField.isRequired();
                }
                if (StringUtils.isEmpty(name)) {
                    name = field.getName();
                }

                value = src.get(name);
                if (value == null) {
                    if (isRequired) {
                        throw new NullPointerException("Field " + name + " can't be null");
                    }
                } else {
                    Class fieldClazz = field.getType();
                    if (Collection.class.isAssignableFrom(fieldClazz)) {
                        Class genericType = Object.class;
                        try {
                            genericType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                        } catch (Exception ignore) { // NOPMD
                        }
                        if (value instanceof Collection) {
                            value = collection2Collection((Collection) value, fieldClazz, genericType);
                        } else if (value.getClass().isArray()) {
                            value = arrayToCollection(value, fieldClazz, genericType);
                        } else {
                            throw new RuntimeException("value type is not supported, type=" + value.getClass());
                        }
                    } else {
                        value = deserializeByType(value, fieldClazz);
                    }
                }
                // 赋值
                field.set(bean, value);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Write bean filed " + realClass.getName()
                    + "." + field.getName() + "error! ", e);
            }
        }
        return (T) bean;
    }

    private static <T> Collection<T> arrayToCollection(Object src,
                                                       Class<? extends Collection> clazz, Class<T> genericType) {
        if (clazz.isInterface()) {
            if (List.class.isAssignableFrom(clazz)) {
                clazz = ArrayList.class;
            } else if (Set.class.isAssignableFrom(clazz)) {
                clazz = HashSet.class;
            }
        }
        Collection collection = ClassUtils.newInstance(clazz);
        for (int i = 0; i < Array.getLength(src); ++i) {
            collection.add(deserializeByType(Array.get(src, i), genericType));
        }
        return collection;
    }

    private static <T> Collection<T> collection2Collection(Collection src,
                                                           Class<? extends Collection> clazz, Class<T> genericType) {
        return arrayToCollection(src.toArray(), clazz, genericType);
    }

    private static <T> T[] collectionToArray(Collection<T> src, Class<T> componentType) {
        return array2Array(src.toArray(), componentType);
    }

    private static <T> T[] array2Array(Object[] src, Class<T> componentType) {
        Object array = Array.newInstance(componentType, src.length);
        for (int i = 0; i < src.length; ++i) {
            Array.set(array, i, deserializeByType(src[i], componentType));
        }
        return (T[]) array;
    }

    /**
     * 按类型进行转换
     *
     * @param src   原始对象
     * @param clazz 期望的对象
     * @param <T>   反序列化类型
     * @return 转换后结果
     */
    public static <T> T deserializeByType(Object src, Class<T> clazz) {
        if (src == null) {
            return (T) ClassUtils.getDefaultPrimitiveValue(clazz);
        } else if (src instanceof Boolean) {
            return (T) CompatibleTypeUtils.convert(src, clazz);
        } else if (src instanceof Number) {
            return (T) CompatibleTypeUtils.convert(src, clazz);
        } else if (src instanceof Map) { // map-->可能是map或者自定义对象
            Map srcMap = (Map) src;
            if (clazz == Object.class) { // 需要自省
                if (srcMap.containsKey(JSON.CLASS_KEY)) {
                    return (T) mapToObject(srcMap, Object.class); // 自定义对象
                } else {
                    return (T) mapToMap(srcMap, srcMap.getClass());
                }
            } else {
                if (Map.class.isAssignableFrom(clazz)) { // map转map
                    return (T) mapToMap(srcMap, (Class<? extends Map>) clazz);
                } else {
                    return mapToObject(srcMap, clazz); // 自定义对象
                }
            }
        } else if (src instanceof Collection) {
            Collection list = (Collection) src;
            if (clazz == Object.class) {
                return (T) collection2Collection(list, list.getClass(), Object.class);
            } else if (Collection.class.isAssignableFrom(clazz)) {
                return (T) collection2Collection(list, (Class<? extends Collection>) clazz, Object.class);
            } else if (clazz.isArray()) {
                if (clazz.getComponentType().isPrimitive()) {
                    return (T) CompatibleTypeUtils.convert(list, clazz);
                } else {
                    return (T) collectionToArray(list, clazz.getComponentType());
                }
            } else {
                return (T) list;
            }
        } else if (src.getClass().isArray()) {
            Class componentType = src.getClass().getComponentType();
            if (componentType.isPrimitive()) {
                if (Collection.class.isAssignableFrom(clazz)) {
                    return (T) arrayToCollection(src, (Class<? extends Collection>) clazz, Object.class);
                } else {
                    return (T) src;
                }
            } else {
                Object[] array = (Object[]) src;
                if (clazz == Object.class) {
                    return (T) array2Array(array, array.getClass().getComponentType());
                } else if (clazz.isArray()) {
                    return (T) array2Array(array, clazz.getComponentType());
                } else if (Collection.class.isAssignableFrom(clazz)) {
                    return (T) arrayToCollection(src, (Class<? extends Collection>) clazz, Object.class);
                } else {
                    return (T) src;
                }
            }
        } else if (clazz.isEnum()) { // 枚举 从字符串进行读取
            if (src instanceof String) {
                return (T) Enum.valueOf((Class<? extends Enum>) clazz, (String) src);
            } else {
                throw new RuntimeException("Enum field must set string!");
            }
        } else if (Date.class.isAssignableFrom(clazz)) { // 日期：支持long和标准格式字符串
            if (src instanceof Long) {
                return (T) new Date((Long) src);
            } else if (src instanceof String) {
                try {
                    return (T) DateUtils.strToDate((String) src);
                } catch (Exception e) {
                    throw new RuntimeException("Date field must set string(yyyy-MM-dd HH:mm:ss)!");
                }
            } else {
                throw new RuntimeException("Date field must set long or string(yyyy-MM-dd HH:mm:ss)!");
            }
        } else if (src instanceof String) { // 字符串支持转换
            return (T) CompatibleTypeUtils.convert(src, clazz);
        } else { // 其它返回src
            return (T) src;
        }
    }

    public static Object deserialize(Object object) {
        return deserializeByType(object, Object.class);
    }
}
