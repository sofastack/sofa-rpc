## server端执行过程 

### 接收请求处理过程

#### RpcHandler->channelRead(ChannelHandlerContext ctx, Object msg)
 * 接收到请求
 * 交给CommandHandler处理
 ``` 
     protocol.getCommandHandler().handleCommand(
                new RemotingContext(ctx, new InvokeContext(), 
                serverSide=true, userProcessors={BoltServerProcessor}), 
                msg);
 ```
 
#### CommandHandler->handleCommand()
 * 请求消息类型
   + RPC_REQUEST
   + RPC_RESPONSE
   + HEARTBEAT
   
 * 跟据请求消息类型获取RpcRequestProcessor
   + 执行process
 
#### RpcRequestProcessor->process(RemotingContext ctx, RpcRequestCommand cmd, ExecutorService defaultExecutor)
 * 解析cmd 获取requestClass
   + SofaRequest
 * 根据requestClass获取UserProcessor
   + BlotServerProcessor
 * 获取BlotServerProcessor线程池 SOFA-SEV-BOLT-BIZ-
   + 执行一个任务来处理后续请求
   
 * netty线程返回
 
#### SOFA-SEV-BOLT-BIZ-线程处理
 * 执行RpcRequestProcessor->doProcess(RemotingContext ctx, RpcRequestCommand cmd)
   + 解析请求到达时间
   + 客户端配置的请求超时时间
   + 请求类型
 * 如果BlotServerProcessor配置了允许请求超时，请求失败
   + 默认true
   + 则比较请求到达时间与当前时间，判断是否超时，超时直接返回null.不处理
   
 * 未超时
   + 反序列化请求
   
 * 跟据接口名称找到接口实现
 * 执行过滤器
 * 执行业务方法调用
 * 业务返回结果
 * 生成RpcResponseCommand
   + 设置cmdId = requestId
 * 序列化响应
 * 执行netty write
 * 触发ServerSendEvent事件
 * 触ServerEndHandleEvent事件
 * 删除RpcInvokeContext，RpcInternalContext
 * 调用结束
 
   
   