## provider端执启动过程

### ProviderConfig类
 * 执行export()启动
   + 加载ProviderBootstrap模块
   + 查找指定Bootstrap
     - 调用方指定
     - RpcConfigs配置：server-rpc-starting.md = sofa (默认)
     使用DefaultProviderBootstrap
     - 初始化完成
   + 执行 bootstrap.export()
     - 如果配置了delay参数，则启动一个线程去延迟启动
     RpcConfigs配置：provider.delay = -1 (默认)
     - 默认不延迟启动
     
 
### ProviderBootstrap类
 * 
 
### BoltServer类
 * 根据RpcConfigs配置：default.protocol=bolt (默认)
 创建BoltServer对象
   + 创建BoltServerProcessor是一个异部处理器
   + 初始化业务线程池
 
 * 接口
   + destroy()
     - 如果有正在处理的请求或等待处理的请求时
     会判断采用优雅停机。先处理任务
     RpcConfig配置：server.stop.timeout=20000（默认）
     优雅停机时间
     - 关闭线程池
     - 关闭netty
   
### BoltServerProcessor类
 * 是BoltServer的一辅助类。两者是合成关系
 * 初始化
   + 序列化对象
 * 是一个异步处理器