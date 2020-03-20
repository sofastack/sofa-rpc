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
package com.alipay.sofa.rpc.log;

import com.alipay.sofa.rpc.common.utils.IOUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC-01001
 * 插件名(-) | 日志版本(1位) | 日志级别(1位) | 日志码(3位)
 * <p>
 * 日志级别：
 * 0: 普通日志输出。
 * 1: 业务警告：这类日志一般是业务使用不当时的输出，比如不推荐使用哪些接口、发现业务潜在的风险时，打印的日志。业务开发人员需要知道这样的输出，并能正确解决
 * 2: 业务错误：这类日志一般是业务异常时的输出，当出现这个日志时，系统的某个服务可能会不可用，或者状态异常。业务开发人员需要知道这样的输出，并能正确解决
 * 3: 框架警告：这类日志一般是框架内部警告，出现这个日志时应该不影响业务使用，但是会存在潜在的风险，业务开发人员应该需要联系框架同学分析
 * 4: 框架错误：这类日志一般是框架内部异常，出现这个日志时，业务开发人员应该难以解决，需要联系框架同学
 * 9: debug日志
 * <p>
 * 日志码：（3位目前够用了，如果要增加到4位的话，直接改日志码版本吧）
 * 三位日志码，第一位代表日志所属内部模块：
 * 0：通用
 * 1：代理层
 * 2：路由层
 * 3：服务调用
 * 4：TR
 *
 * @author <a href=mailto:hongwei.yhw@antfin.com>hongwei.yhw</a>
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class LogCodes {

    private final static Logger                LOGGER                                     = LoggerFactory
                                                                                              .getLogger(LogCodes.class);
    protected final static String              LOG                                        = "RPC-%s: %s %s";
    protected static final Map<String, String> LOG_CODES                                  = new ConcurrentHashMap<String, String>();
    public static final String                 CODE_DOES_NOT_EXIST                        = "LogCodes.getLog error, code does not exist:";
    public static final String                 LITE_LOG_FORMAT_ERROR                      = "LogCode.getLiteLog format error,codeOrMsg=";
    public static final String                 LOG_FORMAT_ERROR                           = "LogCode.getLog format error,code=";

    protected static String                    NOTE                                       = "";

    protected final static String              NOTE_CODE                                  = "999999999";

    //01 启动
    //01000 provider
    public static final String                 INFO_ROUTE_REGISTRY_PUB                    = "010000001";
    public static final String                 INFO_ROUTE_REGISTRY_UNPUB                  = "010000002";
    public static final String                 INFO_ROUTE_REGISTRY_PUB_START              = "010000003";
    public static final String                 INFO_ROUTE_REGISTRY_PUB_OVER               = "010000004";
    public static final String                 ERROR_PROVIDER_TARGET_NULL                 = "010000005";
    public static final String                 ERROR_PROVIDER_TARGET_REGISTERED           = "010000006";
    public static final String                 ERROR_PROXY_PUBLISH_FAIL                   = "010000007";
    public static final String                 ERROR_PROXY_PRE_UNPUBLISH_FAIL             = "010000008";
    public static final String                 ERROR_PROXY_POST_UNPUBLISH_FAIL            = "010000009";
    public static final String                 LOCALFILEREGISTRY_FAIL_WRITECONFLICT       = "010000010";
    public static final String                 ERROR_SERVICE_PUBLISHING                   = "010000011";
    public static final String                 ERROR_SERVICE_UNPUBLISHING                 = "010000012";
    public static final String                 ERROR_ROUTE_ADDRESS_SUBTOKEN_DECRY         = "010000013";
    public static final String                 ERROR_DUPLICATE_PROVIDER_CONFIG            = "010000014";
    public static final String                 WARN_DUPLICATE_PROVIDER_CONFIG             = "010000015";
    public static final String                 ERROR_REGISTER_PROCESSOR_TO_SERVER         = "010000016";
    public static final String                 ERROR_BUILD_PROVIDER_PROXY                 = "010000017";
    public static final String                 ERROR_REFERENCE_AND_INTERFACE              = "010000018";
    public static final String                 ERROR_SERVER_EMPTY                         = "010000019";
    public static final String                 ERROR_REGISTER_TO_REGISTRY                 = "010000020";
    public static final String                 ERROR_PROVIDER_ATTRIBUTE_COMPARE           = "010000021";
    public static final String                 ERROR_PROVIDER_ATTRIBUTE_CHANGE            = "010000022";
    public static final String                 ERROR_CONVERT_URL                          = "010000023";
    public static final String                 ERROR_START_SERVER                         = "010000024";
    public static final String                 ERROR_START_BOLT_SERVER                    = "010000025";
    public static final String                 ERROR_OVERLOADING_METHOD                   = "010000026";
    public static final String                 ERROR_GET_PROXY_CLASS                      = "010000027";
    public static final String                 ERROR_START_SERVER_WITH_PORT               = "010000028";
    public static final String                 ERROR_HTTP2_BIND                           = "010000029";
    public static final String                 ERROR_STOP_SERVER_WITH_PORT                = "010000030";
    public static final String                 ERROR_UNREG_PROCESSOR                      = "010000031";

    //01001 consumer
    public static final String                 INFO_ADDRESS_WAIT_START                    = "010010001";
    public static final String                 INFO_ADDRESS_WAIT_OVER                     = "010010002";
    public static final String                 INFO_ROUTE_REGISTRY_SUB                    = "010010003";
    public static final String                 INFO_ROUTE_REGISTRY_UNSUB                  = "010010004";
    public static final String                 WARN_BINDING_ADDRESS_WAIT_TIME             = "010010005";
    public static final String                 WARN_CONSUMER_NOT_PERMIT                   = "010010006";
    public static final String                 ERROR_STANDALONE_APPNAME_CHECK_FAIL        = "010010007";
    public static final String                 ERROR_STANDALONE_REFER_GENERIC_CHECK_FAIL  = "010010008";
    public static final String                 ERROR_PROXY_CONSUME_FAIL                   = "010010009";
    public static final String                 ERROR_PROXY_UNCOSUME_FAIL                  = "010010010";
    public static final String                 ERROR_INIT_METHOD_SPECIAL                  = "010010011";
    public static final String                 WARN_PROCESS_ADDRESS_WAIT                  = "010010012";
    public static final String                 WARN_PROCESS_ADDRESS_WAIT_CONTINUE         = "010010013";
    public static final String                 ERROR_DUPLICATE_CONSUMER_CONFIG            = "010010014";
    public static final String                 ERROR_BUILD_CONSUMER_PROXY                 = "010010015";
    public static final String                 ERROR_SUBSCRIBE_FROM_REGISTRY              = "010010016";
    public static final String                 ERROR_CONSUMER_ATTRIBUTE_COMPARING         = "010010017";
    public static final String                 ERROR_CONSUMER_ATTRIBUTE_CHANGE            = "010010018";
    public static final String                 ERROR_CONSUMER_REFER_AFTER_CHANGE          = "010010019";
    public static final String                 ERROR_SWITCH_CLUSTER_NEW                   = "010010020";
    public static final String                 WARN_SWITCH_CLUSTER_DESTROY                = "010010021";
    public static final String                 ERROR_START_CLIENT                         = "010010022";

    //01002 dynamic
    //01003 ext
    public static final String                 ERROR_METRIC_REPORT_ERROR                  = "010030001";
    public static final String                 ERROR_TRACER_INIT                          = "010030002";
    public static final String                 ERROR_FILTER_CONSTRUCT                     = "010030003";
    public static final String                 ERROR_CREATE_EXT_INSTANCE                  = "010030004";
    public static final String                 ERROR_EXTENSION_CLASS_NULL                 = "010030005";
    public static final String                 ERROR_EXTENSION_NOT_FOUND                  = "010030006";
    public static final String                 ERROR_LOAD_EXT                             = "010030007";

    //01004 listener
    //01005 module

    public static final String                 ERROR_GUICE_MODULE_CANNOT_FOUND            = "010050001";
    public static final String                 ERROR_ROUTE_ADDRESS_HANDLER_NULL           = "010050002";
    public static final String                 ERROR_ROUTE_ADDRESS_HANDLER_REGISTERED     = "010050003";
    public static final String                 ERROR_ROUTE_ADDRESS_HANDLERS_NULL          = "010050004";
    public static final String                 ERROR_ROUTE_ADDRESS_PHASE_EXIST            = "010050005";
    //01006 registry

    public static final String                 INFO_ROUTE_REGISTRY_URLS_HANDLE            = "010060001";
    public static final String                 LOCALFILEREGISTRY_WRITE_FILEOVER           = "010060002";
    public static final String                 INFO_REGISTRY_IGNORE                       = "010060003";
    public static final String                 LOCALFILEREGISTRY_FAIL_READFILE            = "010060004";
    public static final String                 ERROR_RPC_NETWORK_ADDRESS_LOAD             = "010060005";
    public static final String                 ERROR_DESTRORY_REGISTRY                    = "010060006";
    public static final String                 ERROR_REG_PROVIDER                         = "010060007";
    public static final String                 ERROR_UNREG_PROVIDER                       = "010060008";
    public static final String                 ERROR_SUB_PROVIDER                         = "010060009";
    public static final String                 ERROR_UNSUB_LISTENER                       = "010060010";
    public static final String                 ERROR_SUB_PROVIDER_CONFIG                  = "010060011";
    public static final String                 ERROR_SUB_PROVIDER_OVERRIDE                = "010060012";
    public static final String                 ERROR_UNSUB_PROVIDER_CONFIG                = "010060013";
    public static final String                 ERROR_REG_CONSUMER_CONFIG                  = "010060014";
    public static final String                 ERROR_UNREG_CONSUMER_CONFIG                = "010060015";
    public static final String                 ERROR_UNSUB_CONSUMER_CONFIG                = "010060016";
    public static final String                 ERROR_LOCAL_FILE_NULL                      = "010060017";
    public static final String                 ERROR_HEALTH_CHECK_URL                     = "010060018";
    public static final String                 ERROR_ZOOKEEPER_CLIENT_UNAVAILABLE         = "010060019";
    public static final String                 ERROR_ZOOKEEPER_CLIENT_START               = "010060020";
    public static final String                 ERROR_EMPTY_ADDRESS                        = "010060021";
    public static final String                 ERROR_INIT_NACOS_NAMING_SERVICE            = "010060022";
    public static final String                 ERROR_READ_BACKUP_FILE                     = "010060023";
    public static final String                 ERROR_CHECK_PASS                           = "010060024";
    public static final String                 ERROR_WATCH_HEALTH                         = "010060025";
    public static final String                 ERROR_CLOSE_PATH_CACHE                     = "010060026";
    public static final String                 ERROR_INVALID_ATTRIBUTE                    = "010060027";
    public static final String                 ERROR_REGISTRY_NOT_SUPPORT                 = "010060028";
    public static final String                 ERROR_REGISTRY_INIT                        = "010060029";

    //01007 log
    //01008 proxy generate
    public static final String                 ERROR_PROXY_CONSTRUCT                      = "010080001";
    //01009 transmit

    public static final String                 INFO_TRANSMIT_INIT_FINISH                  = "010090001";
    public static final String                 INFO_TRANSMIT_URLS_HANDLE                  = "010090002";
    public static final String                 ERROR_TRANSMIT_PARSE                       = "010090003";
    public static final String                 ERROR_TRANSMIT_PARSE_APPNAME               = "010090004";
    public static final String                 ERROR_TRANSMIT_PARSE_CONFIG                = "010090005";
    public static final String                 ERROR_TRANSMIT_PARSE_URL                   = "010090006";
    public static final String                 ERROR_TRANSMIT                             = "010090007";
    //01010 event

    public static final String                 ERROR_RPC_EVENT_HANDLE_ERROR               = "010100001";
    public static final String                 ERROR_CE_SERVER_STARTED_LISTENER_ERROR     = "010100002";
    //01999 common通用的

    public static final String                 INFO_ACTIVATOR_START                       = "019990001";
    public static final String                 INFO_ACTIVATOR_END                         = "019990002";
    public static final String                 INFO_GET_CONFIG_DEFAULT_APP                = "019990003";
    public static final String                 INFO_GET_CONFIG_PROPERTY                   = "019990004";
    public static final String                 ERROR_PROXY_BINDING_CLASS_CANNOT_FOUND     = "019990005";
    public static final String                 ERROR_PROVIDER_TR_START                    = "019990006";
    public static final String                 ERROR_PROVIDER_TR_STOP                     = "019990007";
    public static final String                 WARN_PROVIDER_CUT_CAUSE                    = "019990008";
    public static final String                 WARN_PROVIDER_STOPPED                      = "019990009";
    public static final String                 ERROR_SOFA_FRAMEWORK_INVALID               = "019990010";
    public static final String                 ERROR_RPC_LOG_LOAD                         = "019990011";
    public static final String                 ERROR_RPC_CONFIG_LOAD                      = "019990012";
    public static final String                 ERROR_APPLICATION_CONTEXT_NULL             = "019990013";
    public static final String                 ERROR_OSGI_BUNDLECONTEXT_NULL              = "019990014";
    public static final String                 ERROR_OSGI_RESGISTER_SERVICE               = "019990015";
    public static final String                 ERROR_OSGI_UNRESGISTER_SERVICE             = "019990016";
    public static final String                 ERROR_ADDRESSING_CHAIN_EMPTY               = "019990017";
    public static final String                 ERROR_PROVIDER_TRIPLE_START                = "019990018";
    public static final String                 ERROR_RESTART_SCHEDULE_SERVICE             = "019990019";
    public static final String                 ERROR_GET_HOST_FAIL                        = "019990020";
    public static final String                 ERROR_LOAD_RPC_CONFIGS                     = "019990021";
    public static final String                 ERROR_NOT_FOUND_KEY                        = "019990022";
    public static final String                 ERROR_BIND_PORT_ERROR                      = "019990023";
    public static final String                 ERROR_HOST_NOT_FOUND                       = "019990024";
    public static final String                 ERROR_QUERY_ATTRIBUTE                      = "019990025";
    public static final String                 ERROR_UPDATE_ATTRIBUTE                     = "019990026";
    public static final String                 ERROR_SERVER_PROTOCOL_NOT_SUPPORT          = "019990027";

    //02 运行
    // 02000 泛化
    // 02001 Cluster & invoke & transport
    public static final String                 INFO_PROCESS_PROFILER_CLIENT_INVOKE        = "020010001";
    public static final String                 INFO_PROCESS_PROVIDER_TR_IN                = "020010002";
    public static final String                 INFO_PROCESS_PROVIDER_TR_OUT               = "020010003";
    public static final String                 ERROR_RESPONSE_FUTURE_NULL                 = "020010004";
    public static final String                 ERROR_RESPONSE_FUTURE_NOT_CLEAR            = "020010005";
    public static final String                 WARN_PROCESS_PARSE_TARGET_METHOD           = "020010006";
    public static final String                 ERROR_LOAD_CLUSTER                         = "020010007";
    public static final String                 ERROR_DESTORY_ALL_TRANSPORT                = "020010008";
    public static final String                 ERROR_BUILD_PROXY                          = "020010009";
    public static final String                 ERROR_GET_CONNECTION                       = "020010010";
    public static final String                 ERROR_INIT_PROVIDER_TRANSPORT              = "020010011";
    public static final String                 ERROR_CHECK_ALIVE_PROVIDER                 = "020010012";
    public static final String                 ERROR_CLIENT_DESTROYED                     = "020010013";
    public static final String                 ERROR_CLOSE_CONNECTION                     = "020010014";
    public static final String                 ERROR_CATCH_EXCEPTION                      = "020010015";

    // 02002 connectionholder
    public static final String                 INFO_CONNECT_PUT_TO_ALIVE                  = "020020001";
    public static final String                 INFO_CONNECT_PUT_TO_RETRY                  = "020020002";
    public static final String                 INFO_CONNECT_RETRY_START                   = "020020003";
    public static final String                 INFO_CONNECT_RETRY_SUCCES                  = "020020004";
    public static final String                 INFO_CONNECT_RETRY_REMOVE                  = "020020005";
    public static final String                 INFO_CONNECT_ALIVE_REMOVE                  = "020020006";
    public static final String                 INFO_NEGOTIATION_RESULT                    = "020020007";
    public static final String                 ERROR_INVOKE_GET_CLIENT                    = "020020008";
    public static final String                 ERROR_TARGET_URL_INVALID                   = "020020009";
    public static final String                 LOCALFILEREGISTRY_FAIL_INVOKE              = "020020010";
    public static final String                 ERROR_NOTIFY_CONSUMER_STATE_UN             = "020020011";
    public static final String                 WARN_NOTIFY_CONSUMER_STATE                 = "020020012";
    public static final String                 ERROR_UPDATE_PROVIDERS                     = "020020013";
    public static final String                 ERROR_DELETE_PROVIDERS                     = "020020014";
    public static final String                 ERROR_LOAD_CONNECTION_HOLDER               = "020020015";
    // 02003 loadbalancer
    public static final String                 ERROR_LOAD_LOAD_BALANCER                   = "020030001";
    // 02004 router
    // 02005 codec
    public static final String                 WARN_DESERIALIZE_HEADER_ERROR              = "020050001";
    public static final String                 ERROR_DECODE_REQ_CLASS_CANNOT_FOUND        = "020050002";
    public static final String                 ERROR_DECODE_RES_CLASS_CANNOT_FOUND        = "020050003";
    public static final String                 ERROR_DECODE_REQ_PROTOCOL_INVALID          = "020050004";
    public static final String                 ERROR_DECODE_RES_PROTOCOL_INVALID          = "020050005";
    public static final String                 ERROR_DECODE_CLASS_NOT_FOUND               = "020050006";
    public static final String                 ERROR_COMPRESSOR_NOT_FOUND                 = "020050007";
    public static final String                 ERROR_SERIALIZER_NOT_FOUND                 = "020050008";
    public static final String                 ERROR_SERIALIZER                           = "020050009";
    public static final String                 ERROR_UNSUPPORTED_SERIALIZE_TYPE           = "020050010";
    public static final String                 ERROR_UNSUPPORTED_CONTENT_TYPE             = "020050011";
    public static final String                 ERROR_ONLY_ONE_PARAM                       = "020050012";
    public static final String                 ERROR_PROTOBUF_RETURN                      = "020050013";
    public static final String                 ERROR_METHOD_NOT_FOUND                     = "020050014";
    public static final String                 ERROR_VOID_RETURN                          = "020050015";
    public static final String                 ERROR_UNSUPPORT_TYPE                       = "020050016";

    // 02006 addressholder
    public static final String                 ERROR_NO_AVAILBLE_PROVIDER                 = "020060001";
    public static final String                 LOCALFILEREGISTRY_FAIL_READURL             = "020060002";
    public static final String                 ERROR_LOAD_ADDRESS_HOLDER                  = "020060003";
    // 02007 cache
    // 02008 context
    public static final String                 ERROR_ATTACHMENT_KEY                       = "020080001";
    public static final String                 ERROR_ASYNC_THREAD_POOL_REJECT             = "020080002";
    // 02009 tracer
    public static final String                 ERROR_TRACER_UNKNOWN_EXP                   = "020090001";
    public static final String                 ERROR_FAIL_LOAD_TRACER_EXT                 = "020090002";
    public static final String                 ERROR_TRACER_CONSUMER_STACK                = "020090003";
    public static final String                 ERROR_TRACER_PROVIDER_STACK                = "020090004";
    public static final String                 ERROR_LOOKOUT_PROCESS                      = "020090005";

    // 02010 server process
    public static final String                 INFO_SERVICE_METADATA_IS_NULL              = "020100001";
    public static final String                 WARN_CANNOT_FOUND_SERVICE_4_SERVER         = "020100002";
    public static final String                 ERROR_SERVICE_INTERFACE_CANNOT_FOUND       = "020100003";
    public static final String                 ERROR_CANNOT_FOUND_SERVICE_4_CLIENT        = "020100004";
    public static final String                 ERROR_INVOKE_TIMEOUT                       = "020100005";
    public static final String                 ERROR_INVOKE_TIMEOUT_NO_TARGET             = "020100006";
    public static final String                 ERROR_INVOKE_NO_TR_INVOKE_SERVICE          = "020100007";
    public static final String                 ERROR_PROVIDER_TR_POOL_REJECTION           = "020100008";
    public static final String                 ERROR_PROVIDER_TR_POOL_FULL                = "020100009";
    public static final String                 ERROR_PROVIDER_SERVICE_CANNOT_FOUND        = "020100010";
    public static final String                 ERROR_PROVIDER_SERVICE_METHOD_CANNOT_FOUND = "020100011";
    public static final String                 ERROR_PROVIDER_PROCESS                     = "020100012";
    public static final String                 ERROR_DECODE_REQ_SIG_CLASS_NOT_FOUND       = "020100013";
    public static final String                 ERROR_DISCARD_TIMEOUT_REQUEST              = "020100014";
    public static final String                 ERROR_DISCARD_TIMEOUT_RESPONSE             = "020100015";
    public static final String                 ERROR_UNSUPPORTED_PROTOCOL                 = "020100016";
    public static final String                 ERROR_GET_SERVER                           = "020100017";
    public static final String                 ERROR_DESTROY_SERVER                       = "020100018";
    public static final String                 ERROR_PROCESS_UNKNOWN                      = "020100019";
    // 02011 protocol
    public static final String                 ERROR_PROTOCOL_NOT_FOUND                   = "020110001";
    // 02012 filter
    //    public static final String                 ERROR_BUILD_FILTER_CHAIN                   = "020120001";
    public static final String                 ERROR_NEXT_FILTER_AND_INVOKER_NULL         = "020120002";
    public static final String                 ERROR_NEED_DECODE_METHOD                   = "020120003";
    // 02013 event

    // 02014 faulttorenence
    public static final String                 INFO_REGULATION_ABNORMAL                   = "020140001";
    public static final String                 INFO_REGULATION_ABNORMAL_NOT_DEGRADE       = "020140002";
    public static final String                 WARN_SUCCESS_BY_RETRY                      = "020140003";
    public static final String                 ERROR_ORIGIN_WEIGHT_ZERO                   = "020140004";
    public static final String                 ERROR_WHEN_DO_MEASURE                      = "020140005";
    public static final String                 ERROR_WHEN_DO_REGULATE                     = "020140006";
    public static final String                 ERROR_HYSTRIX_FALLBACK_FAIL                = "020140007";

    // 02999 common通用的
    // 未知错误

    public static final String                 LOCALFILEREGISTRY_FAIL_WRITEFILE           = "029990001";

    static {
        init("logcodes-common");
    }

    /**
     * 初始化 Log Codes
     *
     * @param filename 用户名
     */
    public static void init(String filename) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader newClassLoader = LogCodes.class.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(newClassLoader);
            // 由于 ConfigUtil 类是在 sofa-rpc-api 工程里的，core 依赖了 log
            // 所以不能直接使用 ConfigUtil，以免导致循环依赖
            // 故直接获取环境变量
            String encoding = Locale.getDefault().toString();
            if (encoding == null || encoding.length() == 0) {
                encoding = Locale.ENGLISH.toString();
            }
            String name = "sofa-rpc/" + filename + "_" + encoding + ".properties";
            // 如果没有找到文件，默认读取 $filename_en.properties
            if (LogCodes.class.getClassLoader().getResource(name) == null) {
                name = "sofa-rpc/" + filename + "_" + Locale.ENGLISH.toString() + ".properties";
            }
            InputStreamReader reader = null;
            InputStream in = null;
            try {
                Properties properties = new Properties();
                in = LogCodes.class.getClassLoader().getResourceAsStream(name);
                reader = new InputStreamReader(in, "UTF-8");
                properties.load(reader);
                for (Map.Entry entry : properties.entrySet()) {
                    LOG_CODES.put((String) entry.getKey(), (String) entry.getValue());
                }
                NOTE = LOG_CODES.get(NOTE_CODE) == null ? "" : LOG_CODES.get(NOTE_CODE);
            } catch (Exception e) {
                LOGGER.error("初始化日志码失败：" + name, e);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(reader);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

    }

    /**
     * 该方法不应该由日志输出类直接使用，RPC 的所有的日志输出均应该使用 {@link Logger} 类
     * <p>
     * 仅当需要设置 Exception 的提示消息的时候才可使用该方法
     *
     * @param code 日志码
     * @return 日志内容
     */
    public static String getLog(String code) {
        if (!LOG_CODES.containsKey(code)) {
            LOGGER.error(CODE_DOES_NOT_EXIST + code);
            return CODE_DOES_NOT_EXIST + code;
        }
        try {
            return String.format(LOG, code, LOG_CODES.get(code), LogCodes.NOTE);
        } catch (Throwable e) {
            LOGGER.error(LOG_FORMAT_ERROR + code, e);
        }
        return LOG_FORMAT_ERROR + code;
    }

    /**
     * 当输入为日志码的时候，输出日志码对应的日志内容
     * 否则直接输出日志内容
     *
     * @param codeOrMsg 日志码或日志输出
     * @return 基本日志输出，不包含日志码
     */
    public static String getLiteLog(String codeOrMsg) {
        if (!LOG_CODES.containsKey(codeOrMsg)) {
            return codeOrMsg;
        }
        try {
            return LOG_CODES.get(codeOrMsg);
        } catch (Throwable e) {
            LOGGER.error(LITE_LOG_FORMAT_ERROR + codeOrMsg, e);
        }
        return LITE_LOG_FORMAT_ERROR + codeOrMsg;
    }

    public static String getLog(String code, Object... messages) {
        String message = LOG_CODES.get(code);

        if (message == null) {
            LOGGER.error(CODE_DOES_NOT_EXIST + code);
            return CODE_DOES_NOT_EXIST + code;
        }

        try {
            return String.format(LOG, code, MessageFormat.format(message, messages), LogCodes.NOTE);
        } catch (Throwable e) {
            LOGGER.error(LOG_FORMAT_ERROR + code, e);
        }
        return LOG_FORMAT_ERROR + code;
    }

    /**
     * 当输入为日志码的时候，输出日志码对应的日志内容
     * 否则直接输出日志内容
     *
     * @param codeOrMsg 日志码或日志输出
     * @return 基本日志输出，不包含日志码
     */
    public static String getLiteLog(String codeOrMsg, Object... messages) {
        String message = LOG_CODES.get(codeOrMsg);

        if (message == null) {
            message = codeOrMsg;
        }

        try {
            return MessageFormat.format(message, messages);
        } catch (Throwable e) {
            LOGGER.error(LITE_LOG_FORMAT_ERROR + codeOrMsg, e);
        }
        return LITE_LOG_FORMAT_ERROR + codeOrMsg;
    }
}
