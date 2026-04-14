# SOFARPC Triple: gRPC-Java → Netty HTTP/2 迁移愿景

> 北极星文档 — Harness 每轮读取，AI 据此评估完成度并规划下一 Sprint。

---

## 1. 项目概述

**项目名称**：SOFARPC Triple Netty Migration

**核心价值主张**：
将 `remoting-triple` 模块从 `grpc-netty-shaded` 黑盒实现迁移到纯 Netty HTTP/2 实现，
打破 gRPC-Java 的封闭性，使 SOFARPC 的 Triple 协议具备与 Dubbo Triple 同等的扩展能力：
HTTP/3、WebSocket、REST 均可在同一端口、同一 SPI 体系下接入。

**目标用户**：SOFARPC 框架开发者和维护者；不影响任何业务应用代码（协议名 `tri` 不变）。

---

## 2. 最终目标（Vision）

迁移完成后的 `remoting-triple` 模块：

1. **零 grpc-netty-shaded 依赖** — pom.xml 中不再出现 `io.grpc:grpc-netty-shaded`
2. **SPI 注册不变** — `@Extension("tri")` 保持，用户配置 `protocol="tri"` 无需修改
3. **gRPC 5 帧协议完全兼容** — 与原 gRPC 客户端/服务端互通（通过 GrpcCodecTest 验证）
4. **三层分离架构** — Transport(RpcStream) / Codec(RpcCodec) / Invocation(RpcCall) 清晰分层
5. **所有原有测试通过** — TripleServerTest、TripleClientTransportTest、UniqueIdInvokerTest 全绿
6. **Unary + ServerStream + BidiStream 全支持** — 与原实现功能对等
7. **扩展点就绪** — HttpServerTransportListenerFactory SPI 已接入，新协议只需注册新实现

---

## 3. MUST 验收清单

| 编号 | 验收项 | 验证方式 | 优先级 |
|------|--------|----------|--------|
| M1 | Transport 层接口文件存在（RpcStream/RpcStreamListener/Http2RpcStream） | 文件存在检查 | P0 |
| M2 | 核心抽象类存在（RpcCodec/GrpcCodec/RpcCall/UnaryRpcCall） | grep 关键类名 | P0 |
| M3 | 所有单元测试通过（remoting-triple 模块） | `mvn test -pl remoting/remoting-triple` | P0 |
| M4 | SPI 注册正确（tri= 保持不变） | grep SPI 配置文件 | P0 |
| M5 | 新代码不引入 grpc-shaded 包 | grep import io.grpc 检查 | P0 |
| M6 | pom.xml 移除 grpc-netty-shaded 依赖（S6 完成后） | grep pom.xml | P1 |
| M7 | gRPC wire 协议后向兼容（GrpcCodecTest + BackwardCompatTest） | 专项测试通过 | P0 |

---

## 4. SHOULD 改进项（非阻塞）

- [ ] Http2RpcStream 添加 Netty ChannelInboundHandlerAdapter 实现细节完善
- [ ] 流控（flow control）支持 — `request(int)` 语义正确
- [ ] HTTP/3 扩展点预留（Http3RpcStream 空实现占位）
- [ ] 压缩（gzip/identity）支持
- [ ] 连接池复用（TripleClientTransport 复用 channel）

---

## 5. 架构约束与设计原则

### 三层分离（核心约束）
```
RpcCall  (Invocation Semantics)
    UnaryRpcCall / ServerStreamRpcCall / BidiStreamRpcCall
         |  uses
RpcCodec (Encoding/Decoding)
    GrpcCodec / SofaCodec (未来)
         |  uses
RpcStream (Transport)
    Http2RpcStream (Netty native)
    Http3RpcStream (未来)
```

### 禁止事项
- **禁止在新代码中 import `io.grpc.*`（grpc-netty-shaded 包）**
- 禁止修改 SPI 注册文件中 `tri=` 的映射关系
- 禁止修改对外暴露的 `TripleServer` / `TripleClientTransport` SPI 接口签名
- 禁止修改 `triple.Request` / `triple.Response` 的 protobuf 序列化格式（wire compat）

### 必须遵守
- 代码风格与现有 `remoting-triple` 一致（Java 8，无 lambda stream 滥用）
- 新增接口必须有对应 Test 文件
- `Http2StreamHandler` 必须通过 `HttpServerTransportListenerFactory` SPI 工厂选择 listener，不可硬编码
- `GrpcCodec` 的实现参考 `remoting-triplex/TripleXGrpcCodec`：使用 `CodedInputStream`/`CodedOutputStream`

---

## 6. 阶段规划与 Sprint 依赖图

```
S0: RpcStream 接口 ─────────────────────────────────────┐
S1: RpcCodec + GrpcCodec ───────────────────────────────┤
S2: RpcCall + UnaryRpcCall ─────────────────────────────┤
                                                         ↓
S3: TripleServer 服务端集成 (依赖 S0/S1/S2) ────────────┐
S4: TripleClientTransport 客户端集成 (依赖 S0/S2) ──────┤
                                                         ↓
S5: Streaming 支持 — ServerStream/BidiStream (依赖 S2/S3/S4)
S6: 后向兼容验证 + pom cleanup (依赖全部)
```

| 阶段 | Sprint | 核心目标 | 对应 MUST |
|------|--------|----------|-----------|
| Phase 1 — 传输层 | S0-S2 | 三层接口建立 | M1, M2 |
| Phase 2 — 集成层 | S3-S4 | 服务端/客户端接入 | M3, M4, M5 |
| Phase 3 — 功能完整 | S5 | 流式调用 | M3, M7 |
| Phase 4 — 清理验证 | S6 | pom 清理+兼容测试 | M6, M7 |

---

## 7. 关键文件清单

| 文件/目录 | 作用 |
|-----------|------|
| `remoting/remoting-triple/src/main/java/com/alipay/sofa/rpc/transport/triple/` | Triple 传输层主包 |
| `remoting/remoting-triple/src/main/java/com/alipay/sofa/rpc/server/triple/TripleServer.java` | 服务端入口（portUnificationEnabled） |
| `remoting/remoting-triple/src/main/java/com/alipay/sofa/rpc/transport/triple/TripleClientTransport.java` | 客户端入口（grpc-netty-shaded黑盒，待替换） |
| `remoting/remoting-triple/src/main/java/com/alipay/sofa/rpc/transport/triple/Http2StreamHandler.java` | 每流处理器（需改为工厂模式） |
| `remoting/remoting-triple/src/main/java/com/alipay/sofa/rpc/transport/triple/stream/` | 新建：RpcStream 接口层 (S0) |
| `remoting/remoting-triple/src/main/java/com/alipay/sofa/rpc/transport/triple/codec/` | 新建：RpcCodec 编解码层 (S1) |
| `remoting/remoting-triple/src/main/java/com/alipay/sofa/rpc/transport/triple/call/` | 新建：RpcCall 调用语义层 (S2) |
| `remoting/remoting-triplex/` | 参考实现（TripleXGrpcCodec、TripleXHttp2Handler） |
| `remoting/remoting-triple/src/main/resources/META-INF/services/` | SPI 注册文件 |
| `remoting/remoting-triple/pom.xml` | 依赖声明（S6 移除 grpc-netty-shaded） |

---

## 8. 关键参考

### remoting-triplex 中的核心参考实现
- `TripleXGrpcCodec`: gRPC 5-byte frame encode/decode（无 grpc-shaded 依赖）
- `TripleXHttp2Handler`: per-stream Netty handler（Http2MultiplexHandler 模式）
- `TripleXClientTransport`: 基于 FixedChannelPool 的纯 Netty 客户端

### Http2StreamHandler 核心变更（S3 关键）
```java
// 现状（硬编码，需改为）：
transportListener = new PureHttp2ServerTransportListener(
    http2Channel, serverConfig, invoker, bizExecutor);

// 目标（工厂模式，通过 SPI content-type 路由）：
transportListener = HttpServerTransportListenerFactory.DEFAULT.createTransportListener(
    contentType, http2Channel, serverConfig, invoker, bizExecutor);
```

---

## 9. 评估说明

**评分规则（evaluate.sh 输出）**：
- M1(文件): 15分，M2(类存在): 15分，M3(测试): 25分，M4(SPI): 15分，M5(无grpc-shaded): 15分，M7(兼容): 15分 = 100分
- M6(pom清理) 在 S6 前为 WARN 不扣分，S6 后为 P1 MUST
- MUST 项有 FAIL 时总分上限 69（强制修复）

**vision_achieved = true 的条件**：
1. 所有 P0 MUST 项（M1-M5, M7）状态为 PASS
2. M6 状态为 PASS
3. 总评分 ≥ 85
