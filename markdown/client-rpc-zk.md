## client端zookeeper 
 * 启动时处理
 * 运行时动态更新
 * 结束时处理

### 启动时处理与运行时动态更新
 * cluster初始化时触发
 * DefaultConsumerBootstrap执行subscribe方法
 * 启动zookeeper
 * Registry执行subscribe方法
   >>如果服务端已经启动成功了
   >>subscribe后会拿到接口对应的注册地址
   >>执行绑定AddressHolder, connectionHolder
   >>> connectionHolder会跟据地址创建连接。初始化连接池
   
 * 首先创建consumer 结点
   + 
   
 * 订阅：sofa-rpc/xxx/configs
   + 监听该节点变化
   
 * 订阅：sofa-rpc/xxx/overrides
   + 监听该节点变化 
   
 * 订阅：sofa-rpc/xxx/providers
   + 监听该节点变化
   + add
     - 有服务器加入时触发
     - 
   + remove
   + update
   + 第一次如果有获取所有
     - 这种种情况就是有服务器正常注册成功了
     - 拿到服务器ProviderInfo
     - 创建连接
   + ProviderInfo匹配
     - 只拿protocal相同, uniqueId相同
     
### 结束
 * DefaultConsumerBootstrap.unRefer触发
 * 删除Consumer结点
 * 删除监听器
 * 删除Provider监听器
     
#### ProviderInfo 带表一个服务端地址
 * 可以看equals方法
   + port相同
   + rpcVersion
   + protocolType
   + host
   + path
   + serializationType
   + 都同，则带表相同服务端地址
   
 * 每个ProviderInfo都跟据协议类型创建一个ClientTransport
 
 * ProviderInfo为负载均衡使用