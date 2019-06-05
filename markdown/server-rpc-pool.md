## server端线程池

### Thread Factory 
 * NamedThreadFactory
 * 可以参考sofa-rpc NamedThreadFactory的实现
   + poolNumber 表示框架创建的第几个线程池
   
### Rpc-netty-server-boss Thread Pool
 * netty boss线程池
 * 大小一个
 * 不同配置
 * daemon: false

### Rpc-netty-server-worker Thread Pool
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
   + 大小cpu核数 * 2
   + daemon线程
   + name-prefix: Rpc-netty-server-worker
   
### Bolt-conn-event-executor Thread Pool
 * 在DefaultConnectionManager.init()时，创建的
 * 监听netty userEventTriggered事件
 * 大小1个
 * keepalive 60
 * 队列10000
 * 名称：Bolt-conn-event-executor
 * daemon线程 
 
### SOFA-SEV-BOLT-BIZ- Thread Pool
 * BoltServer执行init时创建
 * 用于接收netty的请求。根据请求处理业务
 * 配置 rpc config
   + server.pool.core=20（默认）
   + server.pool.max=200（默认）
   + server.pool.queue=0（默认）
     - 为0使用SynchronousQueue
     - 大于0使用LinkedBlockingQueue(size)
   + server.pool.aliveTime=60000（默认）
   + name-prefix=SEV-BOLT-BIZ- + 端口号
   + server.pool.pre.start= false
     - 是否预热该线程池
   + 饱和策略
     - 打印日志并抛出异常
   + 所以接口共用一个线程池
   
 * 该线程池可以被自定义线程池代替
   + 通过用户向UserThreadPoolManager注册一个UserThreadPool
   + 可以为每个不同方法设置不同线程池
 
 * 如果该线程池不可用。则使用Bolt-default-executor处理业务
   + 默认client接收到响应后的业务线程就是Bolt-default-executor
   
 * 处理完业务。返回响应
   + 返回给netty线程。由netty返回
   
 
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
   
