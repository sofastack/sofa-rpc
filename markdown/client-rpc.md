## client端


### RpcClient
 * 最终请求统一处理，发送到远程服务器
 
### 解析request timeout
 * 决定请求超时时间
 ``` 
    // 先去调用级别配置
    Integer timeout = request.getTimeout();
    if (timeout == null) {
        // 取客户端配置（先方法级别再接口级别）
        timeout = consumerConfig.getMethodTimeout(request.getMethodName());
        if (timeout == null || timeout < 0) {
            // 再取服务端配置。会放到zookeeper中
            timeout = (Integer) providerInfo.getDynamicAttr(ATTR_TIMEOUT);
            if (timeout == null) {
                // 取框架默认值
                timeout = getIntValue(CONSUMER_INVOKE_TIMEOUT);
            }
        }
    }
    return timeout;
 ```
 
 
### 请求过程

#### 调用者
 * 执行rpc call
   + 触发ClientProxyInvoker->invoke(request)
   
 * BoltResponseFuture future = (BoltResponseFuture) SofaResponseFuture.getFuture();
 * Object ret = future.get();
   + 等待结果

#### ClientProxyInvoker->invoke(request)
 * 创建RpcInternalContext
 * decorateRequest
   + serviceName
   + serializeType
   + setInvokeType
   + HEAD_APP_NAME
   + HEAD_PROTOCOL
   
 * 触发ClientStartInvokeEvent事件
 * 执行 cluster.invoke(request)
   + 路由
   + 负载均衡
   + filterChain
     - 最后执行ConsumerInvoker 是一个FilterInvoker
     调用cluster->sendMsg(request)
     
 * 返回
   + 返回response
   
 * decorateResponse
   + 将ResponseFuture保存到RpcInvokeContext
  
 * 删除RpcInternalContext
   + 解绑Thread上的当前RpcInternalContext
   
 * 返回调用者
     
   
### AbstractCluster->sendMsg(ProviderInfo providerInfo, SofaRequest request)
 * 根据providerInfo获取BoltClientTransport
 * 解析request timeout
 * 根据request的invokeType。选择发送类型
 
 * 以异步发送为例：
   + transport.asyncSend(request, timeout)
 
 * 返回
   + 将ResponseFuture保存到RpcInternalContext
   + 返回SofaResponse
 
#### BoltClientTransport->asyncSend(request, timeout)
 * 检查连接是否可用
   + 如果拿到连接则返回
   + 没连接，则创建。如果创建失败（超时）抛出异常
   
 * 创建InvokeContext调用上下文
   
 * 触发ClientBeforeSendEvent 事件
   + 会执行Tracer逻辑，主要是解绑当前Thread的Span
   并将Span缓存到RpcInternalContext
   
 * 缓存RpcInternalContext到InvokeContext
   + 因为异步返回后，RpcInternalContext会被从Thread解绑
    为了响应时找到对应RpcInternalContext，所以需要缓存
    
 * 创建ResponseFuture
   + BoltResponseFuture
 * 创建InvokeCallback
   + BoltFutureInvokeCallback
   
 * 执行发送
 ``` 
    RPC_CLIENT.invokeWithCallback(url, request, 
    invokeContext, callback, timeoutMillis);
 ```
   
 * 返回ResponseFuture
   + 当返回到Cluster时
   将ResponseFuture存到RpcInternalContext。为了调用方获取响应
   同时返回一个空的response
   
 * 执行afterSend
   + 
   
#### RpcRemoting->invokeWithCallback(url, request, invokeContext, callback, timeoutMillis)
 * RpcClientRemoting
 * 根据url获取 connection
 * 根据request创建RpcRequestCommand，
   + 生成requestId
   + 配置自定义序列化类型
   + 判断是否开启CRC校验。默认开启
   + 设置request timeout
   + 设置接口类
   + 设置InvokeContext
   + serialize
      - 序列化class
      - header
      - content
   + 记录command 日志 debug级别
   
 * 当requstId 存到InvokeContext
 
 * 创建 InvokeFuture
   + DefaultInvokeFuture
   + requestId
   + RpcInvokeCallbackListener
   + invokeCallback
   + ProtocolCode
   + getCommandFactory
   + invokeContext
 
 * 将 InvokeFuture 绑定到Connection
   + key: requestId, value: InvokeFuture
   + ConcurrentHashMap实现，初始大小4
   
 * 启个Timer 监听 请求超时
   + 如果超时，InvokeFuture.setResponse()
   + 执行callback返回响应流程
   
 * 执行netty写请求
   + 如果写失败，也会触发InvokeFuture返回
   
 * 整个请求流程结束
 
 
### 响应过程
 
#### RpcHandler->channelRead(ctx, msg)
 * 执行
 ``` 
    protocol.getCommandHandler().handleCommand(
                new RemotingContext(ctx, new InvokeContext(), serverSide=false, userProcessors=null), msg);
 ```

#### RpcCommandHandler->(RemotingContext ctx, Object msg)
 * 将msg 转为RpcCommand
 `RpcCommand cmd = (RpcCommand) msg`
 * 获取Processor
   + 只有三种Processor
     - request
     - response
     - heartbeat
   + 获取responseProcessor
 * 使用Bolt-default-executor线程池，执行一个任务处理后续响应逻辑
   + 此时netty线程返回
   + 执行responseProcessor->doResponse(RemotingContext ctx, RemotingCommand cmd)
   
   
 * doResponse
   + 获取Connection
   + 跟据requestId获取InvokeFuture
   + future设置结果
   + future取消，超时定时器
   + future执行回调响应
   
### InvokeFuture->executeInvokeCallback()
 * 获取InvokeCallback
 * 使用RPC-CB 线程池，执行一个任务处理后续响应逻辑
   + 此时Bolt-default-executor线程返回
   
 * 获取响应response
   + 从future获取
 * 正常response
   + response设置InvokeContext
   + response返序列化
   + InvokeCallback.onResponse(msg)
 
 * 异常response
   + 执行InvokeCallback.onException(e)
   
 
### InvokeCallback->onResponse(msg)   //onException类似
 * 切换RpcInternalContext
   + 将请求时的RpcInternalContext，绑定到当前响应Thread上
   
 * 发送ClientAsyncReceiveEvent事件
 
 * 执行filterChain
   + onAsyncResponse
   
 * 记录请求响应时间
   + recordClientElapseTime
   
 * 发送ClientEndInvokeEvent事件
 
 * BoltResponseFuture.setSuccess(response)
   + 返回调用者（调用者被唤醒）
   
 * RPC-CB 线程结束
   
   
   