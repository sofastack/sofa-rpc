package com.alipay.sofa.rpc.codec.fury;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.LogCodes;

/**
 * @author lipan
 */
public class FuryHelper {
    /**
     * 请求参数类型缓存 {service+method:class}
     */
    private ConcurrentMap<String, Class> requestClassCache  = new ConcurrentHashMap<String, Class>();

    /**
     * 返回结果类型缓存 {service+method:class}
     */
    private ConcurrentMap<String, Class> responseClassCache = new ConcurrentHashMap<String, Class>();

    /**
     * 从缓存中获取请求值类
     *
     * @param service    接口名
     * @param methodName 方法名
     * @return 请求参数类
     */
    public Class getReqClass(String service, String methodName) {

        String key = buildMethodKey(service, methodName);
        Class reqClass = requestClassCache.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = ConfigUniqueNameGenerator.getInterfaceName(service);
            Class clazz = ClassUtils.forName(interfaceClass, true);
            loadProtoClassToCache(key, clazz, methodName);
        }
        return requestClassCache.get(key);
    }

    /**
     * 从缓存中获取返回值类
     *
     * @param service    接口名
     * @param methodName 方法名
     * @return 请求参数类
     */
    public Class getResClass(String service, String methodName) {
        String key = service + "#" + methodName;
        Class reqClass = responseClassCache.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = ConfigUniqueNameGenerator.getInterfaceName(service);
            Class clazz = ClassUtils.forName(interfaceClass, true);
            loadProtoClassToCache(key, clazz, methodName);
        }
        return responseClassCache.get(key);
    }

    /**
     * 拼装缓存的key
     *
     * @param serviceName 接口名
     * @param methodName  方法名
     * @return Key
     */
    private String buildMethodKey(String serviceName, String methodName) {
        return serviceName + "#" + methodName;
    }

    /**
     * 加载fury接口里方法的参数和返回值类型到缓存，不需要传递
     *
     * @param key        缓存的key
     * @param clazz      接口名
     * @param methodName 方法名
     */
    private void loadProtoClassToCache(String key, Class clazz, String methodName) {
        Method pbMethod = null;
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (methodName.equals(method.getName())) {
                pbMethod = method;
                break;
            }
        }
        if (pbMethod == null) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_METHOD_NOT_FOUND, clazz.getName(),
                methodName));
        }
        Class[] parameterTypes = pbMethod.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length != 1) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_ONLY_ONE_PARAM, "protobuf",
                clazz.getName()));
        }
        Class reqClass = parameterTypes[0];
        requestClassCache.put(key, reqClass);
        Class resClass = pbMethod.getReturnType();
        if (resClass == void.class) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_PROTOBUF_RETURN, clazz.getName()));
        }
        responseClassCache.put(key, resClass);
    }
}
