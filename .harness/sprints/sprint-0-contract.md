# Sprint 0 — RpcStream Transport 层接口

> Sprint 合约：先行设计，后写代码。

---

## 元数据

| 字段 | 值 |
|------|----|
| Sprint 编号 | 0 |
| 标题 | 建立 RpcStream 传输层接口抽象 |
| 优先级 | HIGH |
| 关联 MUST 项 | M1 |
| 预计影响文件数 | 6 |
| 依赖 Sprint | 无 |

---

## 任务描述

建立三层分离架构的最底层：Transport 层（RpcStream）。

这是整个迁移的基石。后续 S1（RpcCodec）、S2（RpcCall）都建立在 RpcStream 之上。
参考 Dubbo Triple 的 `ClientStream` 接口设计，以及 `remoting-triplex` 的 `TripleXHttp2Handler`。

**核心设计思路**：
- `RpcStream` 是对一条 HTTP/2 Stream 的抽象，独立于 gRPC 语义
- `RpcStreamListener` 是接收事件的回调接口
- `Http2RpcStream` 是基于 Netty `Channel` 的具体实现
- 不涉及任何 gRPC 语义（那是 RpcCall 层的职责）

---

## 实现方案

### 目标包路径
```
remoting/remoting-triple/src/main/java/com/alipay/sofa/rpc/transport/triple/stream/
├── RpcStream.java            (接口)
├── RpcStreamListener.java    (接口)
├── Http2RpcStream.java       (Netty 实现，占位)
└── RpcStreamFactory.java     (工厂接口，用于 Http2/Http3 切换)

remoting/remoting-triple/src/test/java/com/alipay/sofa/rpc/transport/triple/stream/
├── RpcStreamTest.java        (接口契约测试)
└── Http2RpcStreamTest.java   (Http2RpcStream 基础测试)
```

### RpcStream 接口设计
```java
public interface RpcStream {
    /** 发送一帧数据（已经过 RpcCodec 编码的字节） */
    void writeMessage(byte[] data, boolean compressed);

    /** 发送 headers（请求头/响应头） */
    void writeHeaders(Map<String, String> headers);

    /** 半关闭（发送方不再发送，等待对端响应） */
    void halfClose();

    /** 取消 Stream */
    void cancel(Throwable cause);

    /** 注册监听器 */
    void setListener(RpcStreamListener listener);

    /** 流控：允许接收 count 帧 */
    void request(int count);

    /** 当前流是否可写（背压控制） */
    boolean isWritable();
}
```

### RpcStreamListener 接口设计
```java
public interface RpcStreamListener {
    /** 收到请求头 */
    void onHeaders(Map<String, String> headers, boolean endStream);

    /** 收到一帧数据（原始字节，待 RpcCodec 解码） */
    void onMessage(byte[] data);

    /** 流正常结束（对端 halfClose） */
    void onComplete();

    /** 流异常 */
    void onError(Throwable cause);

    /** 可写状态变更 */
    void onWritabilityChanged();
}
```

### Http2RpcStream 设计
```java
// 基于 Netty Channel，持有 streamId 和 ctx
// 参考 remoting-triplex 的 Http2Channel.java
public class Http2RpcStream implements RpcStream {
    private final ChannelHandlerContext ctx;
    private final Http2ConnectionEncoder encoder;
    private final int streamId;
    private volatile RpcStreamListener listener;

    // writeMessage: encoder.writeData(ctx, streamId, Unpooled.wrappedBuffer(data), ...)
    // halfClose: encoder.writeData(ctx, streamId, Unpooled.EMPTY_BUFFER, 0, true, ...)
    // cancel: ctx.channel().close()
}
```

### 影响分析

- **新增文件**：
  - `stream/RpcStream.java` — 传输层核心接口
  - `stream/RpcStreamListener.java` — 事件监听接口
  - `stream/Http2RpcStream.java` — Netty HTTP/2 实现
  - `stream/RpcStreamFactory.java` — 工厂接口（为 Http3 扩展预留）
  - `stream/RpcStreamTest.java` — 接口契约测试（Mock 实现验证）
  - `stream/Http2RpcStreamTest.java` — 基础单元测试（EmbeddedChannel）
- **修改文件**：无（S0 仅新增，不修改现有代码）
- **删除文件**：无

### 风险与约束

- `Http2RpcStream` 依赖 `netty-codec-http2`（已在 pom 中声明，无需新增依赖）
- 不引入任何 `io.grpc.*` 依赖
- S0 不接入任何现有代码路径，纯新增，零回归风险

---

## 验收标准

- [ ] `stream/` 包下 4 个 Java 文件存在
- [ ] `RpcStream` 接口包含 `writeMessage`、`halfClose`、`cancel`、`setListener`、`request`、`isWritable` 方法
- [ ] `RpcStreamListener` 包含 `onHeaders`、`onMessage`、`onComplete`、`onError` 方法
- [ ] `Http2RpcStream` 实现 `RpcStream` 接口，不 import `io.grpc.*`
- [ ] 2 个测试文件存在且能通过 `mvn test -pl remoting/remoting-triple`
- [ ] evaluate.sh sprint 0 评分 ≥ 70

---

## 测试计划

| 测试类型 | 测试文件 | 覆盖场景 |
|----------|----------|----------|
| 接口契约 | `RpcStreamTest.java` | Mock 实现验证接口契约（writeMessage/halfClose/cancel） |
| 单元测试 | `Http2RpcStreamTest.java` | EmbeddedChannel 验证写数据、halfClose、cancel 行为 |

---

## 参考资料

- Dubbo Triple `ClientStream` 接口: `dubbo-rpc-triple/src/main/java/org/apache/dubbo/rpc/protocol/tri/stream/ClientStream.java`
- SOFARPC triplex `Http2Channel`: `remoting/remoting-triplex/src/main/java/com/alipay/sofa/rpc/transport/triplex/Http2Channel.java`
- Netty Http2ConnectionEncoder API

---

*合约创建时间: 2026-04-14*
