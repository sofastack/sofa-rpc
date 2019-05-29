## client端上下文

### 
 * client 端的request Header
   + ConsumerConfig中的AppName
   + ConsumerConfig中的Protocol
   
 * RemotingConstants
   + 发送到远程的常量

### RpcInternalContext
 * ClientProxyInvoker执行invoke方法时触发创建
   + 业务线程触发
   因为业务在调用实际rpc方法时，则会触发ClientProxyInvoker调用
   ClientProxyInvoker是实际接口的一个代理对象（因为没有具体实现类）
   
 * 


### RpcInvokeContext