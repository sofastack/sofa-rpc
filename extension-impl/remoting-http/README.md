# 《HTTP协议设计》

本设计包含 HTTP/1.1 和 HTTP/2 通用设计。

- HTTP/1.1 只提供服务端，
- HTTP/2 支持服务端和客户端，但暂不支持流式调用功能。

## 服务端
- 默认端口 `12300`
- 默认 `keepAlive=true`

## 客户端
- 默认 `keepAlive=true`

## URL & URI
格式：`${protocol}://${ip}:${port}/${interfaceName}[:${uniqueId}]/methodName`

推荐用 `POST` 进行调用

```plain
POST h2c://127.0.0.1:12300/com.alipay.sofa.rpc.test.HelloService/sayHello
HEAD：
DATA：序列化后的数据
```

简单的函数也可以直接用 `GET`，例如

```plain
GET http://127.0.0.1:12300/com.alipay.sofa.rpc.test.HelloService:1.0:groupA/sayHello?name=zhang&age=123
```


## Request 格式
### Request Head
| HEAD                     | 含义       | 必填 | 备注                                |
|:-------------------------|:----------|:----|:-----------------------------------|
| sofa_head_serialize_type | 序列化类型 | 是   | 如果没有设置这个值将读取 content-type |
| content-type             | 序列化     | 否  |                                    |
| sofa_head_target_app     | 目标应用   | 否   |                                    |
| sofa_head_req_props      | 请求附加值 | 否   |                                    |
| sofa_head_req_baggage    | 请求透传值 | 否   |                                    |


#### content-type
| 序列化    | content-type           |
|:---------|:-----------------------|
| hessian2 | x-application/hessian  |
| json     | application/json       |
| protobuf | application/x-protobuf |


### Request Body
Body 部分为序列化后的参数值。例如 `json` 序列化 `Object[]`.
```
["zhang", 123]
```

## Response

### Response Head
| HEAD                     | 含义        | 必填 | 备注                               |
|:-------------------------|:-----------|:-----|:----------------------------------|
| sofa_head_serialize_type | 序列化类型  | 是   | 如果没有设置这个值将读取 content-type |
| content-type             | 序列化      | 是   | 默认 application/json              |
| sofa_head_resp_error     | 返回是否异常 | 否   |                                   |
| sofa_head_resp_baggage   | 响应透传值  | 否   |                                   |

### Response Body

#### 正常情况
- HTTP Response 的响应码还是 `200`。
- BODY 为序列化后的响应值。例如 `json` 序列化后的 `String`.
    ```
    "Hello world"
    ```

#### 业务异常

- HTTP Response 的响应码还是 `200`。
- HEAD 里的 `sofa_head_resp_error` 是 `true`。
- BODY 为错误描述字符串，默认为 UTF-8 编码，例如：
    ```
    java.lang.RuntimeException: 业务异常
    ```

#### RPC框架异常
- HTTP Response 的响应码不是 `200`，例如找不到服务是 `404`，请求不对是 `400` 等。
- BODY 为错误描述字符串，默认为 UTF-8 编码，例如：
    ```
    RPC-02411: 未找到业务服务，服务名称：[com.alipay.sofa.rpc.server.http.HttpService:1.0]
    ```