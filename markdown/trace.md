## SofaTrace

### openTracing
 * [文档](https://github.com/opentracing-contrib/opentracing-specification-zh)
 
### SofaTrace
 * [文档](https://www.sofastack.tech/sofa-tracer/docs/Home)
 
 
### 线程池
 
#### Tracer-AsyncConsumer-Thread-CommonProfileErrorAppender
 * 大小3
 
#### Tracer-AsyncConsumer-Thread-NetworkAppender
 * 大小3
 
#### Tracer-AsyncConsumer-Thread-SelfLogAppender
 * 大小1
 
#### Tracer-Daemon-Thread
 * 大小1
 
#### Tracer-TimeAppender-1-60  
 * 60表示采样频率
 * 大小1个定时调试线程池
 
 
 
### 调用
 * SofaTracerSpanContext与SofaTracerSpan一一对应
   + 记录tracerId, spanId, parentSpanId
#### startRpc
 * 在执行ClientProxyInvoker调用invoker时
   + 执行cluster.invoke()之前触发
   
 * 由ClientStartInvokeEvent事件触发
 * 每一个rpc请求调用，都生成一个新的SofaTracerSpan对象
   + 创建SofaTracerSpan对象后生成SofaTracerSpanContext
      - 如果当前线程之前有未结束的请求。则创建的SofaTracerSpan对象是上一个SofaTracerSpan的孩子
      - 上一个SofaTracerSpan是当前SofaTracerSpan的父亲
      - 如果是孩子SofaTracerSpan则生成spanId会依赖父亲的spanId
      - 调用Tracer扩展，如MDC扩展
      
   + 设置一些clientSpan Tags
      - SPAN_KIND = client
      - LOCAL_APP
      - PROTOCOL
      - SERVICE
      - METHOD
      - CURRENT_THREAD_NAME
      
   + 绑定SofaTracerSpan到当前Thread
   
>> 注意孩子知道父亲，但父亲不知道孩子
>> 父亲是相同线程，当前请求的上一个请求。可能父亲还有父亲再等待处理
>>> 如果请求快，响应慢。则会是一条长链条
      
### client ConsumerTracerFilter
 * 设置tag
   + INVOKE_TYPE
   + ROUTE_RECORD
   + REMOTE_APP
   + REMOTE_IP
    
### clientBeforeSend
 * 在BoltClientTransport send请求时触发
 * 由ClientBeforeSendEvent事件处理发
 * 获取当前请求的SofaTracerSpan
 * 获取SofaTracerSpan中的SofaTracerSpanContext
 * 序列化SofaTracerSpanContext
   + tracerId
   + spanId
   + parentSpanId
   + isSampled
 * 将序列化数据放到request的Header
   + key: NEW_RPC_TRACE_NAME
   + 当tracer数据发送到远程服务器
 
 * 如果request是异步
   + 将当前SofaTracerSpan从当前线程解绑
   + 记录日志
     - SofaTracerSpan.log()是将日志记录到LinkedList()。等待处理
   + 将当前 request 的SofaTracerSpan存到当前请求上下文中RpcInternalContext
     - key: INTERNAL_KEY_TRACER_SPAN
   + 如果当前SofaTracerSpan有父亲
     - 恢复父亲当前SofaTracerSpan绑定到当前Thread
     即恢复上一个请求SofaTracerSpan绑定到当前Thread
   
 * 如果request是同步
   + 直接返回。不涉及到上下文切换
   
### client clientReceived
 * 由BoltFutureInvokeCallback.onResponse()触发
 * 由ClientAsyncReceiveEvent事件触发 
 * 此时线程是client-CB线程
 * 切换请求时的SofaTracerSpan，绑定到当前Thread
 * 记录CLIENT_RECV_EVENT_VALUE事件
 * 设置tag
   - REQ_SERIALIZE_TIME
   - RESP_DESERIALIZE_TIME
   - RESP_SIZE
   - REQ_SIZE
   - CLIENT_CONN_TIME
   - CLIENT_ELAPSE_TIME (可能设置不上)
   - LOCAL_IP
   - LOCAL_PORT
   - RESULT_CODE
   
 * 执行SofaTracerSpan.finish()
   + 记录日志
   
 * 如果SofaTracerSpan有父亲，恢复父亲到当前线程
 
 >> 对于异步调用时跟踪日志中没有client.elapse.time值
 >> 因为client.elapse.time计算是在ClientAsyncReceiveEvent事件前
 ClientEndInvokeEvent事件后
 >> 所以异步调用没有，只有同步调用会显示
 
### client clientReceived
 * 由BoltFutureInvokeCallback.onResponse()触发
 * 由ClientEndInvokeEvent触发
   + 只有request类型是同步情况，才处理。
   + 异步不处理
   
 * 检查状态
   + 此时一个请求跟踪已经可以结束了。
   + 如果当前Thread还有多由一个SofaTracerSpan则打印错误日志提醒