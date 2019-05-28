## 通用功能

### Module扩展加载
 * jvm参数配置：
   + 扩展位置：extension.load.path=[]
     - 默认：
         ``` 
          [
              "META-INF/services/sofa-rpc/",
              "META-INF/services/"
          ]
         ```
 * com.alipay.sofa.rpc.ext.ExtensionLoader
   + 负责加载模块
   