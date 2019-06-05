## client端


### RpcClient
 * 最终请求统一处理，发送到远程服务器
 
#### RpcServer未启动情况
 * RpcClient可以正常启动
 * 调用者执行异步调用时
   + cluster路由时，没有找到一个ProviderInfo（提供者信息）
   抛出异常
   + 调用结束

#### Zookeeper未启动
 * client启动时连接
   + 默认会重连3次。3次重连还连接不上，则抛出异常。client启动失败
   + 返回重连超时时间：RcpConfigs: registry.connect.timeout=20000（默认）
   
#### client运行时，Zookeeper挂了
 * client会一直发起重连请求
 * 在未重连上时，调用者发都Rpc请求，则抛出异常
 
 * 重启zookeeper。client连接成功
 
#### client运行，zk运行，server一直抛出异常
 * 单机自动故障剔除
 
#### client运行中，server上下线
 * client的服务发现机制
   + 订阅provider, 结点
   + 删除provider, close netty connection
   
#### client启动时，server运行中
 * client的服务发现机制
   + 订阅provider，会发现所有已经运行的provider, 并初始化连接 
   
#### 集群容错
 * 有种集群
   + FastFail：不容错快速失败
   + FastOver: 支持集群容错
 * 实现方式 
   + 设置响应失败后重试次数
   + 支持方式级别设置重试次数与全局设置
   + 默认全局设置为0，不重试
   
 * 失败
   + 不包领路由失败，如果路由失败，则直接返回
   + 调用时抛出了异常算失败
      - 如：负载均衡失败
      - 调用超时
      - 客户端异常
      - 服务器异常
   
#### 服务器预热
 * 当client订阅到Provider后，跟据provider注册的url信息读取成ProviderInfo
   + 获取weight：预热结束后权重
   + 获取warmupTime：预热时间毫秒
   + 获取warmupWeight：预热时权重
   + 获取startTime： 服务器开始时间
   + client拿到预热信息后，设置ProviderInfo为预热状态
 * 当client执行负载均衡时
   + 获取ProviderInfo的weight. 获取获取weight时都会判断预热时间是否结束
   + 如果结束使用weight, 未结束使用warmupWeight
   
 * 对于单台服务器
   + 使用Random负载均衡时，即使用预热状态不启作用。还是会选则这一台
   
#### 单机自动故障剔除
 * 通过module实现FaultToleranceModule
 * 监听总线ClientSyncReceiveEvent，ClientAsyncReceiveEvent事件实现
   + 通过监听消息返回，触发总调用次数，与异常调用统计
   + 如果是首次返回，则通过发布订阅模式通知触发TimeWindowRegulator调度线程启动
   + 监听维度：providerInfo + consumerConfig 区分不同服务调用封装到InvocationStat
   + 一个InvocationStat代表一个client-server的调用状态
     - 两台服务器发布相同服务，则是两上InvocationStat
     - MeasureModel中会聚合这两个InvocationStat
   
 * 异常处理
   + CLIENT_TIMEOUT
   + SERVER_BUSY
   + 只有这两个异常才会计失败次数
 * 开启条件 
   + 默认sofarpc自带故障剔除。但是没有开启
   + 需要配置consumerConfig的appname
   + 需要通过FaultToleranceConfigManager配置开启
     - 可以使用全局配置，也可以为每个appname单独配置
     - 配置允许降级
     - 如果配置不允许降级，但是非正常链路，则也会降级，但如果链路正常则不会恢复
     
 * MeasureModel
   + 是ConsumerConfig.appname + ConsumerConfig.interfaceId确定一个MeasureModel
   + MeasureModel中存放存所有发布了这个接口的InvocationStat
   
  
 * measure过程 ：数据统计过程-计算度量结果
   + 过滤调用次数与达到配置的需要调控的调用次数
     - 遍历每一个InvocationStat，判断调用次数是否达到需要调控的次数
     - 实现调用次数还会跟据weight情况来计算实际调用次数
     - 如果达到，则累计异常次数与总调用次数
     - 计算平均异常比率 = 异常次数/总调用次数
     
   + 获取到平均异常比率rate1
   
   + 过滤平均异常比率是否达到配置调控异常比率rate2
     - 如果rate1== -1 则表示都正常不需要控制
     - 获取单个InvocationStat state的异常比率rate3= 异常次数/调用次数
     - 如果rate1== 0 则表示都正常不需要控制
     - 计算 state的异常率rate3占总异常庇rate1的比例：rate4 = rate3/rate1
     - 如果rate4 >= rate2 则表示该链路不正常
     - 否则也是正常节点
     
   + 得到所有链路是否正常状态，异常率等信息
   + 本次计算完成，则清除链路InvocationStat的本次使用的总调用次数， 异常次数数据。为下一次做准备
   
   + 将结果交给另一线程进行Regulation处理
 >> measure在一个调度线程池中执行。1秒执行一次。判断是否到达需要measure。默认10次measure一次，即10秒measure一次
   
 * Regulation过程：根据度量结果，判断是否需要执行降级或者恢复
   + 就是判断是否需要降级，或恢复
   + 默认降级，恢复策略，使用的权重weight
   + 判断降级
     - 如果链路非正常状态，则按照降级速率，进行降级。默认最低降为1，providerInfo为降级状态
   + 判断恢复
     - 如果链路是正常状态，则按照恢复速率，进行恢复。providerInfo为恢复状态
     - 如果达到原始weight则恢复结束。providerInfo为正常状态
 >> regulation在一个线程池中执行
 
>> 对于单台，随机曲美他嗪
 
 
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
 * 响应消息类型
   + RPC_REQUEST
   + RPC_RESPONSE
   + HEARTBEAT
   
 * 根据消息类型获取RpcResponseProcessor
   + 执行process
   
#### RpcResponseProcessor->process()
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
   
   
   