#!/usr/bin/env bash
# must-check.sh — MUST 项快速检查（输出追加到 assess_vision 提示词）
# 不评分，仅输出当前 MUST 项状态摘要供 AI 参考

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TRIPLE_SRC="$PROJECT_ROOT/remoting/remoting-triple/src/main/java/com/alipay/sofa/rpc"
TRIPLE_POM="$PROJECT_ROOT/remoting/remoting-triple/pom.xml"
SPI_DIR="$PROJECT_ROOT/remoting/remoting-triple/src/main/resources/META-INF/services/sofa-rpc"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

echo "=== SOFARPC Triple Migration MUST Check ==="
echo ""

# M1: Transport 层接口
echo "--- M1: Transport 层接口 ---"
for f in RpcStream RpcStreamListener Http2RpcStream; do
    fp="$TRIPLE_SRC/transport/triple/stream/${f}.java"
    [[ -f "$fp" ]] && pass "$f.java" || fail "$f.java"
done

echo ""
echo "--- M2: 核心抽象类 ---"
for f in RpcCodec GrpcCodec; do
    fp="$TRIPLE_SRC/transport/triple/codec/${f}.java"
    [[ -f "$fp" ]] && pass "codec/$f.java" || fail "codec/$f.java"
done
for f in RpcCall UnaryRpcCall; do
    fp="$TRIPLE_SRC/transport/triple/call/${f}.java"
    [[ -f "$fp" ]] && pass "call/$f.java" || fail "call/$f.java"
done

echo ""
echo "--- M4: SPI 注册 ---"
server_spi="$SPI_DIR/com.alipay.sofa.rpc.server.Server"
transport_spi="$SPI_DIR/com.alipay.sofa.rpc.transport.ClientTransport"
[[ -f "$server_spi" ]] && grep -q "^tri=" "$server_spi" && pass "Server SPI tri=" || fail "Server SPI tri= 缺失"
[[ -f "$transport_spi" ]] && grep -q "^tri=" "$transport_spi" && pass "ClientTransport SPI tri=" || fail "ClientTransport SPI tri= 缺失"

echo ""
echo "--- M5: 无 grpc-shaded import ---"
violations=0
for pkg in stream codec call; do
    pkg_dir="$TRIPLE_SRC/transport/triple/$pkg"
    if [[ -d "$pkg_dir" ]]; then
        count=$(grep -r "import io\.grpc\." "$pkg_dir" 2>/dev/null | wc -l | tr -d ' ')
        if [[ "$count" -gt 0 ]]; then
            fail "$pkg/: $count 处 io.grpc.* import"
            violations=$(( violations + count ))
        else
            pass "$pkg/: 无 io.grpc.* import"
        fi
    else
        warn "$pkg/: 目录不存在（尚未实现）"
    fi
done

echo ""
echo "--- M6: pom.xml grpc-netty-shaded ---"
if grep -q "grpc-netty-shaded" "$TRIPLE_POM" 2>/dev/null; then
    warn "pom.xml 仍包含 grpc-netty-shaded（S6 前可接受）"
else
    pass "pom.xml 已移除 grpc-netty-shaded"
fi

echo ""
echo "=== 检查完成 ==="
