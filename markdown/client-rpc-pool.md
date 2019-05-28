## client端线程池

### Thread Factory 
 * NamedThreadFactory
 * 可以参考sofa-rpc NamedThreadFactory的实现
   + poolNumber 表示框架创建的第几个线程池

### Rpc-netty-client-worker Thread Pool
 * 在RpcConnectionFactory初始化时创建
 * RpcConnectionFactory
   + 负责创建本地到远程服务端的连接
   + 即netty客户端
   + 初始workerGroup
   + init
      - 初始他Netty Bootstrap对象
      - 所有connection都使用同一个Bootstrap对象创建
      - 所以Bootstrap是共享的
      
 * workerGroup
   + 是配置到Bootstrap上。所以是所有connection共享的
   + 线程池不支持自定义配置
   + 大小cpu核数
   + daemon线程
   + name-prefix: Rpc-netty-client-worker
   
### Bolt-conn-event-executor Thread Pool
 * 在DefaultConnectionManager.init()时，创建的
 * 监听netty userEventTriggered事件
 * 大小1个
 * keepalive 60
 * 队列10000
 * 名称：Bolt-conn-event-executor
 * daemon线程 
 
### RpcTaskScannerThread Thread Pool
 * RpcTaskScanner初始化时创建
 * 用于检测DefaultConnectionManager管理的连接是否可用
 * 是一个newSingleThreadScheduledExecutor
   + 10000毫秒执行一次
 
### Bolt-default-executor Thread Pool
 * 创建RpcClient 创建RpcClientRemoting时
 加载RpcClientRemoting的父类RpcRemoting时执行静态代码块
 ``` 
     static {
            RpcProtocolManager.initProtocols();
     }
 ```
   + 初始化协议
   时创建RpcCommandHandler再创建ProcessorManager时生成的
   
 * 作用：处理netty客户端响应
 * 配置都是jvm参数配置
   + bolt.tp.min：20（默认）
   + bolt.tp.max：400（默认）
   + bolt.tp.queue：600（默认）
   + bolt.tp.keepalive：60（默认）
   + daemon:true
   + name-prefix: Bolt-default-executor
   
 * 处理响应过程
   + 跟据响应的requestId拿到connection中的调用InvokeFuture
   + 向InvokeFuture设置response结果
   + 调用InvokeFuture.executeInvokeCallback()
   + 此时default线程处理结束
     - 同步请求就是设置response后结束
     - 异步请求就是执行完executeInvokeCallback()结束
     因为callback会有callback线程再去执行后续处理
     
 * 请求过程 
   + 生成requestId
   + 创建Command
   + 创建InvokeFuture
   + add到connection<requestId, InvokeFuture>
   
### RPC-CB Thread Pool
 * BoltFutureInvokeCallback
   + AsyncRuntime
 * 配置 rpc config
   + async.pool.core= 10（默认）
   + async.pool.max=200（默认）
   + async.pool.queue=256（默认）
   + async.pool.time=60000（默认）
   + daemon=true
 * 作用
   + BoltFutureInvokeCallback.onResponse()
   + 设置future.setSuccess()
     - 通知客户端调用者返回结果
     
    
### CLI-RC- Thread Pool
 * 在AllConnectConnectionHolder执行init时创建的
 连接重连检测线程
 * 是一个ScheduledService
 * 配置 rpc config
   + consumer.reconnect.period=10000默认
   + 最小2000

