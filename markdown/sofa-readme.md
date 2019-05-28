## Sofa

### 启动参数
``` 
   -Dlog4j.configurationFile=config/log4j2-test.xml
   -Dlogging.path=/data/home/user00/log/battle
   -Dlogger.impl=com.alipay.sofa.rpc.log.SLF4JLoggerImpl
```
 * 建议调式时使用-Dlogger.impl=com.alipay.sofa.rpc.log.SLF4JLoggerImpl

### Sofa Rpc
 * 服务发布
   + [文档](https://www.sofastack.tech/sofa-rpc/docs/Programing-RPC)
   + 启动zk
   
 * 同一个服务发布
   + 多协议
   + 多注册中心
 * 配置覆盖
   + 线程调用级别设置 >> 服务调用方方法级别设置 >> 服务调用方 Reference 级别设置 >> 服务提供方方法级别设置 >> 服务提供方 Service 级别设置
   
 
#### 协议
 * bolt
   + [参考](https://blog.csdn.net/beyondself_77/article/details/81027204)
   + 调用方式
     - 同步
     - 异步
     - 回调: 配置时回调，调用接口时回调
     - 单向
   + 超时
     - 默认3秒
     - 针对client：配置时设置，方法设置
   + 泛化调用
     - 不需要依赖服务器商品调用 
     - 目前只有bolt协议使用hessian2序列化使用
   + 序列化
     - 目前支持hessian2和protobuf
     
   + 自定义业务线程池
   
 * restful协议
 
#### 注册中心
 * zk
   + 支持认证
 
#### 负载均衡
 * 相对于调用方的负载均衡
 * random 
   + 默认
 * roundRobin
   + 方法级别轮询
   
#### 自定义过滤器
 * 对请求，响应过滤
 
#### 自定义路由
 * 相对于调用方
 
#### 重试
 * 默认集群模式
   + FailOver
 * 只有服务端框架层出错，或超时走会重试。业务异常不会重试
 
#### 链路追踪
 * 默认开启：sofaTracer
 
#### 自定义线程池
 * 可以设置独立的业务线程池与rpc框架线程池隔离
 
#### 链路数据透传
 
#### 预热权重
 * 集群机器预热
 
#### 集群使用
 * 自动故障剔除
 
#### 优雅关闭 
 * 
 
### 开发手册 
 * 
 
#### 配置文件
 * appName
   + 代表服务名称 
   + 多个服务可以共享同一个AppName
   
 * rpc配置
   + SofaConfigs负责加载
   + 5.5.3 版本中。
     - System.getProperty()
     - ExternalConfigLoader 扩展外部配置 默认没有
     - config/rpc-config.properties 类路径下 默认没有文件
   + 框架内部有几个地方使用
   
 * 框架配置
   + RpcConfigs负责加载
   + 5.5.3 版本中。
     - 加载rpc-config-default.json 默认有此文件
     - 加载sofa-rpc/rpc-config.json 默认有此文件
     - 加载META-INF/sofa-rpc/rpc-config.json 默认没有文件
     - 加载System.getProperties()
     - 只能读取类路径中的文件，不支持外部文件
   + 可以配置方法黑白名单
     - provider.include：白名单：默认所有
     - provider.exclude：黑名单：默认没有
     
   + 调用putValue, removeValue
   会触发配置变量listener。
   现在Provider启动时，会配置监听器，发现配置改变会重新发部服务
     
### 启动过程
#### 服务器
 * 启动过程 
   + 通过ProviderConfig->export()启动
   + 通过bootstrap->default.provider.bootstrap(默认: soft)
     - 获取bootstrap名称
   + 通过bootstrap名称，查找ProviderBootstrap扩展类
   + 调用扩展类的export()启动
   
   + 必要参数
     - interfaceId 确定proxyClass与key
     - ref 接口实现类
     - ServerConfig
     
   + ServerConfig
     - 一个Provider可以配置多个ServerConfig
     - 即一个服务可以提供多种协议不同接口调用
     - 通过interfaceId + ":" + uniqueId + ":" + protocol
     判断一个服务发布次数。默认相同uniqueId只能发布一次
     
   + RegistryConfig
     - 默认需要初始化注册中心：service.register(默认：true)
     - 如果有RegistryConfig则注册
     - 一个服务器最多配置3个注册中心
     
   + 创建Server
     - 跟据端口判断是否Server已经存在
     - 跟据protocal获取扩展Server: 默认配置：default.protocol=bolt
     - 调用server.init(ServerConfig)
        - 初始化默认业务线程池，ThreadPoolExecutor实现
        默认：server.pool.queue=0，使用SynchronousQueue。否则使用new LinkedBlockingQueue<Runnable>(
                                                                  queueSize)
        -如果自定线程池，则使用自定线程池。
        
     - 调用server.registerProcessor(providerConfig, providerConfigProxy)
        - 生成key=interfaceId + version + uniqueId
        - value = providerConfigProxy
     - 判断自动启动：默认配置：server.auto.start=true
    
     - 启动RemoveServer: netty实现
       - RpcHandler处理业务
     
   + 配置注册中心配置变更监听器ProviderAttributeListener
   + 启动注册中心client
     - 注册ServerConfig地址
     
#### 服务器运行
  * 处理请求：通过key= interfaceId + version + uniqueId
  找到对应providerConfigProxy处理
     
  * netty与rpc框架怎么结合
    + 通过创建remoteServer时传消息处理器
    + 如BoltServer，使用BoltServerProcessor，处理SofaRequest
    + 当netty handler接收到消息为SofaRequest后最终发BoltServerProcessor处理
    
  * Netty Hanlder处理过程
    + 整体处理过程是一个行为设计模式的命令模式
    + RpcHandler(即netty handler) 客户角色。创建命令，创建执行者
      - 封装上下文对象。用于接收响应消息
      
    + 通过协议处理器（命令请求者）中的RpcCommandHandler处理命令
      - 负责调用命令，执行请求
      
    + 执行者：就是BoltServerProcessor。通过创建RemoteServer传过来的
    + RpcCommandHandler。也有自己的消息处理器
      - netty层消息处理器，先对消息做一个处理。查发送消息
      - 如：RpcRequestProcessor中确定业务线程池，然后异步调用业务
      - 最终由如:BoltServerProcessor处理请求
      
#### 服务器rpc框架内部接收请求
 * Bolt协议
 * BoltServerProcessor处理请求
   + 跟据不同key=interfaceId + version + uniqueId。选则相应Invoker执行
   + 执行FilterChain传递请求处理
   
 
#### 客户端rpc框架内部发送请求
   
 * 客户端启动
   + 通过ConsumerConfig->refer()启动
   + 通过bootstrap->default.provider.bootstrap(默认: soft)
        - 获取bootstrap名称
        - 跟据Protocal名称。default.protocol(默认：bolt)
        - 默认配置default.consumer.bootstrap(默认: soft)
   + 通过bootstrap名称，查找ConsumerBootstrap扩展类
   + protocol + "://" + interfaceId + ":" + uniqueId; 相同的不能大于3个：默认
   + 确定cluster, 默认：consumer.cluster=failover 故障转移，支持重试和指定地址调用
     - failfast：快速失败
   + Consumer配置发生变化监听器
   + 配置集群服务端地址监听器
   + 初始化cluster
   + 创建Invokder
   + 创建Integer代理类
      - refer()返回的就是代理类。通过代理类远程调用
      
 * Cluster
   + 构造Router链
     + 选择ProviderInfo
   + 负载均衡策略 考虑是否可动态替换？默认consumer.loadBalancer=random
   + 地址管理器。默认：consumer.addressHolder=singleGroup
   + 连接管理器。默认：consumer.connectionHolder=all
     - 有地址是就会创建一个ClientTransport
     每个ConsumerConfig+host+port就是一个ClientTransport
     - 并创建连接。并初始化连接池。返回一个到服务器的长连接。用于判断连接状态。
     如果配置consumer.connection.num。连接池大小。默认为1
     
   + 构造Filter链,最底层是调用过滤器
     - 最后一个过滤器是ConsumerInvoker。
   + 启动重连线程
   + 得到服务端列表
     + 从注册中心
     + 更新到地址管理器，更新连接管理器。给Router使用
   + 初始化服务端连接（建立长连接)
   
 * 调用
   + 通过Proxy请求
   + 走cluster
     - 选择路由
     - 负载均衡一个地址
     - 去连接管理器判断地址是否可连接
     - 执行过滤器链.最后一个过滤器是ConsumerInvoker。
     会调用cluster->sendMsg()
     - 调用ClientTransport->send(msg)
     - 调用rpcClient->send(msg)
     - 调用rpcRemoting->send(Msg)
     - 从连接池中获取连接
     - 调用netty connection发送
     - 最后返回cluster
     - 请求失败重试
     
 * BoltClientTransport 默认：consumer.connection.num:1
   + 
   
### 注意
 * rpc接口不要使用同名方法
 
 
### sofa 日志框架
 * 调用
   ```
     如： 
     import com.alipay.sofa.rpc.log.Logger;
     import com.alipay.sofa.rpc.log.LoggerFactory;
     private final static Logger LOGGER = LoggerFactory.getLogger(RpcRuntimeContext.class);
   ```
   + 所有使用这种方式获取LOGGER都会创建一个LOGGER对象实现。
     - 默认LOGGER实现：MiddlewareLoggerImpl
     
 * 初始化日志入口
   + com.alipay.sofa.rpc.log.LoggerFactory
   + 跟据配置的logger.impl日志实现类。默认：sofa-rpc/rpc-config.json->"logger.impl": "com.alipay.sofa.rpc.log.MiddlewareLoggerImpl"
   + 所以默认加载com.alipay.sofa.rpc.log.MiddlewareLoggerImpl进行实际日志框架初始化
   
#### 如何嵌入现有项目日志
 * 配置jvm参数：-Dlogger.impl=com.alipay.sofa.rpc.log.SLF4JLoggerImpl
   + com.alipay.sofa.rpc.log.SLF4JLoggerImpl是sofa实现的。
   + 也可以自己实现
   
 * sofa默认使用MiddlewareLoggerImpl
   + 不好与现在项目日志统一
   
#### sofa 自己的日志框架 MiddlewareLoggerImpl实现
   + 内部是对org.slf4j.Logger DEFAULT_LOGGER的一个代理封装
   + 首先要创建DEFAULT_LOGGER这个被代理的logger
     - 调用RpcLoggerFactory.getLogger(name, null);创建
     - 通过不同build创建不同AbstractLoggerSpaceFactory
     - com.alipay.sofa.rpc是一个名称空间。同一个名称空间使用同一个AbstractLoggerSpaceFactory
     AbstractLoggerSpaceFactory是实现了org.slf4j的Logger工厂类
     意思是虽然创建不同的MiddlewareLoggerImpl，但共用AbstractLoggerSpaceFactory
   + 读取配置文件
     - 只能读取类路径下的配置
     - 如果jvm参数配置了：log.env.suffix。日志配置文件后缀
     com.alipay.sofa.rpc:1&com.alipay.sofa.rpc:2&com.alipay.sofa.rpc:3
     解析完：suffix = .3
     - com/alipay/sofa/rpc/log/logback/log-conf.xml + suffix
     即log-conf.xml.3
     com/alipay/sofa/rpc/log/logback/log-conf.xml是sofarpc默认自带的配置
     配置文件：com/alipay/sofa/rpc/log/logback/log/config.properties + suffix
     即config.properties.3
     - 如果没找到：且suffix 不为空则再找
     com/alipay/sofa/rpc/log/logback/log-conf-suffix[1:].xml
     即log-conf-3.xml
     - 最后判断jvm参数：logging.config.com.alipay.sofa.rpc是否存在
     如果在。则加载这个配置
     
 * jvm参数
   + sofa.middleware.log.disable 默认：true
     - RpcLoggerFactory.getLogger(name, null)是否可以创建LoggerImpl
     - 如果：false, 则所有使用默认com.alipay.sofa.common.log Logger
     
   + logback.middleware.log.disable 默认：false
     - 如果系统使用logback打日志会读取这个配置
     - 如果为true, 会打印警告。同时使用com.alipay.sofa.common.log Logger
   + log4j2.middleware.log.disable  默认：false
     - 如果系统使用log4j2打日志会读取这个配置
     - 如果为true, 会打印警告。同时使用com.alipay.sofa.common.log Logger
     
   + sofa.middleware.log.internal.level 默认：WARN
     - 在未创建LoggerImpl时，需要打印的日志是通过System.out直接打印
     - 默认 level是 Warn 
     - 可以通过这个参数改为debug。排查日志框架创建失败问题
     
   + logging.path.com.alipay.sofa.rpc
     - 日志输出路径
     - 如果没有配置使用log.path
   + log.path
     - 日志输出路径
     - 默认System.getProperty("user.home")+ File.separator + "logs"
      windows: C://playcrab/logs
   + logging.level.com.alipay.sofa.rpc
     - 日志级别
     - 默认：INFO