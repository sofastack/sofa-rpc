## server端执启动过程

### ServerConfig类
 * 当第一次遇到这个类时，进行类加载操作。
 * 首先加载父类AbstractIdConfig
   - 加载静态代码块
   - 执行RpcRuntimeContext.now()
   ``` 
       static {
           RpcRuntimeContext.now();
       }   
   ```
   - ServerConfig初始化完成
 * 
   
 
### RpcRuntimeContext
 * 全局的运行时上下文
   + 过程public的静态属性和方法，保证全局访问
 * 在加载AbstractIdConfig类时，由AbstractIdConfig类的静态代码块触发加载
   + 执行此类加载
   + 执行此类静态代码块
   ``` 
    private final static Logger LOGGER = LoggerFactory.getLogger(RpcRuntimeContext.class);
    
    static {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Welcome! Loading SOFA RPC Framework : {}, PID is:{}", Version.BUILD_VERSION, PID);
        }
        put(RpcConstants.CONFIG_KEY_RPC_VERSION, Version.RPC_VERSION);
        // 初始化一些上下文
        initContext();
        // 初始化其它模块
        ModuleFactory.installModules();
        // 增加jvm关闭事件
        if (RpcConfigs.getOrDefaultValue(RpcOptions.JVM_SHUTDOWN_HOOK, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("SOFA RPC Framework catch JVM shutdown event, Run shutdown hook now.");
                    }
                    destroy(false);
                }
            }, "SOFA-RPC-ShutdownHook"));
        }
    }
   ```
   + 执行initContext()
     - 读取配置： 应用Id：sofa.app.id（默认无）
     - 读取配置： 应用名称：sofa.app.name（默认无）
     - 读取配置： 应用实例Id：sofa.instance.id（默认无）
     - 以上参数可以通过配置RpcConfigs
     - 读取jvm配置：user.dir
   + ModuleFactory.installModules()
     - 初始化Module模块
     - Module
     - 根据RpcConfig: module.load.list=默认(*)
     加载相应Module
     
   + 注册ShutdownHook
     - 根据RpcConfig: jvm.shutdown.hook=默认(true) 
     
 * 接口
   + destroy
   + cacheConsumerConfig
   + cacheProviderConfig
     
   
### LoggerFactory 
 * sofa自己定义的日志框架
 * 在加载RpcRuntimeContext类时，由RpcRuntimeContext类中的静态属性LOGGER触发加载
 * 静态初始化
   + 静态属性private static String implClass = RpcConfigs.getStringValue(RpcOptions.LOGGER_IMPL);
   + 配置参数：logger.impl=[默认: com.alipay.sofa.rpc.log.MiddlewareLoggerImpl]
      - 这个参数目前只对rpc框架有效。
      - rpc依赖项目bolt日志框架也使用com.alipay.sofa.rpc.log.MiddlewareLoggerImpl实现。
      但是固定的，logger.impl参数不启作用
      - 建议本地调试rpc框架时，logger.impl=com.alipay.sofa.rpc.log.SLF4JLoggerImpl
      可以通过jvm参数配置
   + LoggerFactory初始化完成
   
 * 接口
   + getLogger
   new 一个新的logger
 * 完成
   
### RpcConfigs
 * sofa rpc配置文件
 * 在加载LoggerFactory类时，由LoggerFactory类中静态属性触发加载
 * 静态初始化
   + 执行静态代码块
   ``` 
        static {
            init(); // 加载配置文件
        }
   ```
   + 执行init()
      - 4 加载rpc-config-default.json配置文件
      - 3 加载sofa-rpc/rpc-config.jsons配置文件
      - 2 加载META-INF/sofa-rpc/rpc-config.json配置文件
      - 1 加载System.getProperties()
      - 配置优化级是从下到上 1-4
      
   + 自定义配置
      - 三种方式。但只能是类路径下的配置
      - 3 加载sofa-rpc/rpc-config.jsons配置文件
      默认sofa-rpc包下会自带一个 rpc.config.order=200
      - 2 加载META-INF/sofa-rpc/rpc-config.json配置文件
      - 1 加载System.getProperties() 配置系统参数
      
   + 加载自定义配置时的特殊处理
      - 3 加载sofa-rpc/rpc-config.jsons配置文件
      - 2 加载META-INF/sofa-rpc/rpc-config.json配置文件
      - 可能会加载到多个同名的配置
      如：自己项目也创建一个sofa-rpc/rpc-config.json。默认sofa-rpc包下会自带一个
      通过设置rpc.config.order=1，越大优先级越高，越后加载
      
   + 静态初始化完成后，整个类就初始化完成了 
   
 * 接口
   + putValue
     - 运行时可以自定义配置
   + removeValue
     - 运行时可以删除配置
   + subscribe
     - 运行时可以配置项目添加变量监听器
     - 当putValue, removeValue时触发相应配置项目的监听器
   + unSubscribe
     - 运行时可以删除某个配置项目的监听器
   
 * 配置RpcConfigListener
   + 当运行时修改某个配置项目时，会通知监听器配置更新
   
 * 完成
 
### ModuleFactory
 * 负责所有模块管理
 * 在加载RpcRuntimeContext类时，由RpcRuntimeContext类中静态调用触发加载
 * 接口
   + installModules()
     - 加载全部模块
   + uninstallModule()
     - 卸载模块
     
### ExtensionLoaderFactory
 * 管理扩展模块加载器管理
 * 在调用ModuleFactory.installModules()中调用ExtensionLoaderFactory.getExtensionLoader(Module.class)时触发加载
 * 接口
   + getExtensionLoader
   
### ExtensionLoader
 * 具体扩展模块加载实现
 * 创建该对象时需要指定加载的接口类型，同时接口必须有Extensible注解
 * 具体扩展类需要注解Extension
 * 扩展查找路径：Rpc配置：extension.load.path
   + 默认：
   ``` 
      [ 
        "META-INF/services/sofa-rpc/",
        "META-INF/services/"
      ]
   ```
   + 默认扩展类型 
   ``` 
    com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap
    com.alipay.sofa.rpc.bootstrap.ProviderBootstrap
    com.alipay.sofa.rpc.client.AddressHolder
    com.alipay.sofa.rpc.client.aft.DegradeStrategy
    com.alipay.sofa.rpc.client.aft.MeasureStrategy
    com.alipay.sofa.rpc.client.aft.RecoverStrategy
    com.alipay.sofa.rpc.client.aft.RegulationStrategy
    com.alipay.sofa.rpc.client.aft.Regulator
    com.alipay.sofa.rpc.client.Cluster
    com.alipay.sofa.rpc.client.ConnectionHolder
    com.alipay.sofa.rpc.client.LoadBalancer
    com.alipay.sofa.rpc.client.Router
    com.alipay.sofa.rpc.codec.Compressor
    com.alipay.sofa.rpc.codec.Serializer
    com.alipay.sofa.rpc.filter.Filter
    com.alipay.sofa.rpc.module.Module
    com.alipay.sofa.rpc.protocol.TelnetHandler
    com.alipay.sofa.rpc.proxy.Proxy
    com.alipay.sofa.rpc.registry.Registry
    com.alipay.sofa.rpc.server.Server
    com.alipay.sofa.rpc.tracer.Tracer
    com.alipay.sofa.rpc.transport.ClientTransport
    com.alipay.sofa.rpc.transport.ServerTransport
   ``` 
  
   + 默认加载 ConsumerBootstrap.class
   ``` 
    sofa=com.alipay.sofa.rpc.bootstrap.DefaultConsumerBootstrap
    bolt=com.alipay.sofa.rpc.bootstrap.bolt.BoltConsumerBootstrap
    dubbo=com.alipay.sofa.rpc.bootstrap.dubbo.DubboConsumerBootstrap
    h2c=com.alipay.sofa.rpc.bootstrap.http.Http2ClearTextConsumerBootstrap
    rest=com.alipay.sofa.rpc.bootstrap.rest.RestConsumerBootstrap
   ```
   + 默认加载 ProviderBootstrap.class
   ``` 
    sofa=com.alipay.sofa.rpc.bootstrap.DefaultProviderBootstrap
    bolt=com.alipay.sofa.rpc.bootstrap.bolt.BoltProviderBootstrap
    dubbo=com.alipay.sofa.rpc.bootstrap.dubbo.DubboProviderBootstrap
    h2c=com.alipay.sofa.rpc.bootstrap.http.Http2ClearTextProviderBootstrap
    rest=com.alipay.sofa.rpc.bootstrap.rest.RestProviderBootstrap

   ```
   + 默认加载 AddressHolder.class
   ``` 
    singleGroup=com.alipay.sofa.rpc.client.SingleGroupAddressHolder
   ```
   + 默认加载 DegradeStrategy.class
   ``` 
    log=com.alipay.sofa.rpc.client.aft.impl.LogPrintDegradeStrategy
    weight=com.alipay.sofa.rpc.client.aft.impl.WeightDegradeStrategy
   ```
   + 默认加载 MeasureStrategy.class
   ``` 
    serviceHorizontal=com.alipay.sofa.rpc.client.aft.impl.ServiceHorizontalMeasureStrategy
   ```
   + 默认加载 RecoverStrategy.class
   ``` 
    weight=com.alipay.sofa.rpc.client.aft.impl.WeightRecoverStrategy
   ```
   + 默认加载 RegulationStrategy.class
   ``` 
    serviceHorizontal=com.alipay.sofa.rpc.client.aft.impl.ServiceHorizontalRegulationStrategy
   ```
   
   + 默认加载 Regulator.class
   ``` 
    timeWindow=com.alipay.sofa.rpc.client.aft.impl.TimeWindowRegulator
   ```
   + 默认加载 Cluster.class
   ``` 
    failfast=com.alipay.sofa.rpc.client.FailFastCluster
    failover=com.alipay.sofa.rpc.client.FailoverCluster
   ```
   + 默认加载 ConnectionHolder.class
   ``` 
    all=com.alipay.sofa.rpc.client.AllConnectConnectionHolder
    elastic=com.alipay.sofa.rpc.client.ElasticConnectionHolder   
   ```
   + 默认加载 LoadBalancer.class
   ``` 
    consistentHash=com.alipay.sofa.rpc.client.lb.ConsistentHashLoadBalancer
    localPref=com.alipay.sofa.rpc.client.lb.LocalPreferenceLoadBalancer
    random=com.alipay.sofa.rpc.client.lb.RandomLoadBalancer
    roundRobin=com.alipay.sofa.rpc.client.lb.RoundRobinLoadBalancer
    weightRoundRobin=com.alipay.sofa.rpc.client.lb.WeightRoundRobinLoadBalancer
    weightConsistentHash=com.alipay.sofa.rpc.client.lb.WeightConsistentHashLoadBalancer      
   ```
   + 默认加载 Router.class
   ``` 
    directUrl=com.alipay.sofa.rpc.client.router.DirectUrlRouter
    registry=com.alipay.sofa.rpc.client.router.RegistryRouter
    mesh=com.alipay.sofa.rpc.client.router.MeshRouter  
   ```
   + 默认加载 Compressor.class
   ``` 
    snappy=com.alipay.sofa.rpc.codec.snappy.SnappyRpcCompressor      
   ```
   + 默认加载 Serializer.class
   ``` 
    hessian2=com.alipay.sofa.rpc.codec.sofahessian.SofaHessianSerializer
    protobuf=com.alipay.sofa.rpc.codec.protobuf.ProtobufSerializer
    json=com.alipay.sofa.rpc.codec.jackson.JacksonSerializer      
   ```
   + 默认加载 Filter.class
   ``` 
    com.alipay.sofa.rpc.filter.ProviderExceptionFilter             # -20000
    com.alipay.sofa.rpc.filter.ConsumerExceptionFilter             # -20000
    com.alipay.sofa.rpc.filter.RpcServiceContextFilter             # -19500
    com.alipay.sofa.rpc.filter.RpcReferenceContextFilter           # -19500
    hystrix=com.alipay.sofa.rpc.hystrix.HystrixFilter
    com.alipay.sofa.rpc.filter.ConsumerGenericFilter               # -18000
    com.alipay.sofa.rpc.filter.ProviderBaggageFilter               # -11000
    com.alipay.sofa.rpc.filter.sofatracer.ConsumerTracerFilter     # -10000
    com.alipay.sofa.rpc.filter.sofatracer.ProviderTracerFilter     # -10000      
   ```
   + 默认加载 Module.class
   ```  
    fault-tolerance=com.alipay.sofa.rpc.module.FaultToleranceModule
    sofaTracer=com.alipay.sofa.rpc.module.SofaTracerModule
    com.alipay.sofa.rpc.module.RestTracerModule
    lookout=com.alipay.sofa.rpc.module.LookoutModule
   ```
   + 默认加载 TelnetHandler.class
   ``` 
    help=com.alipay.sofa.rpc.protocol.telnet.HelpTelnetHandler      
   ```
   + 默认加载 Proxy.class
   ``` 
    jdk=com.alipay.sofa.rpc.proxy.jdk.JDKProxy
    javassist=com.alipay.sofa.rpc.proxy.javassist.JavassistProxy
    bytebuddy=com.alipay.sofa.rpc.proxy.bytebuddy.BytebuddyProxy       
   ```
   + 默认加载 Registry.class
   ``` 
    consul=com.alipay.sofa.rpc.registry.consul.ConsulRegistry
    local=com.alipay.sofa.rpc.registry.local.LocalRegistry
    zookeeper=com.alipay.sofa.rpc.registry.zk.ZookeeperRegistry
    nacos=com.alipay.sofa.rpc.registry.nacos.NacosRegistry
    mesh=com.alipay.sofa.rpc.registry.mesh.MeshRegistry
    sofa=com.alipay.sofa.rpc.registry.sofa.SofaRegistry      
   ```
   + 默认加载 Server.class
   ``` 
    bolt=com.alipay.sofa.rpc.server.bolt.BoltServer
    http=com.alipay.sofa.rpc.server.http.Http1Server
    h2c=com.alipay.sofa.rpc.server.http.Http2ClearTextServer
    rest=com.alipay.sofa.rpc.server.rest.RestServer      
   ```
   + 默认加载 Tracer.class
   ``` 
    sofaTracer=com.alipay.sofa.rpc.tracer.sofatracer.RpcSofaTracer      
   ```
   + 默认加载 ClientTransport.class
   ``` 
    bolt=com.alipay.sofa.rpc.transport.bolt.BoltClientTransport
    h2c=com.alipay.sofa.rpc.transport.http.Http2ClearTextClientTransport
    h2=com.alipay.sofa.rpc.transport.http.Http2ClientTransport
    rest=com.alipay.sofa.rpc.transport.rest.RestClientTransport      
   ```
   + 默认加载 ServerTransport.class
   ``` 
    http=com.alipay.sofa.rpc.transport.http.Http1ServerTransport
    h2c=com.alipay.sofa.rpc.transport.http.Http2ClearTextServerTransport      
   ```

 * 加载过程。看源码很简单