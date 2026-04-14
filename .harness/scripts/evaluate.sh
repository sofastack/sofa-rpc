#!/usr/bin/env bash
# evaluate.sh — SOFARPC Triple Netty Migration 评估脚本
#
# 用法：
#   .harness/scripts/evaluate.sh <sprint_number>
#
# 评分维度（共 100 分）：
#   M1 文件完整性    15分  (RpcStream/RpcStreamListener/Http2RpcStream 存在)
#   M2 核心类存在    15分  (RpcCodec/GrpcCodec/RpcCall/UnaryRpcCall)
#   M3 测试通过      25分  (mvn test -pl remoting/remoting-triple)
#   M4 SPI 正确      15分  (tri= 注册不变)
#   M5 无grpc-shaded 15分  (新代码不引入 io.grpc.* import)
#   M7 gRPC兼容      15分  (GrpcCodecTest 或 BackwardCompatTest 通过)
#
# M6 pom 清理: S6 前为 WARN(不扣分)，S6 后升级为 FAIL

set -eo pipefail

SPRINT_N="${1:?用法: evaluate.sh <sprint_number>}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
HARNESS_DIR="$PROJECT_ROOT/.harness"
SPRINTS_DIR="$HARNESS_DIR/sprints"
REPORT_FILE="$SPRINTS_DIR/sprint-${SPRINT_N}-qa-report.md"
TRIPLE_MODULE="remoting/remoting-triple"
TRIPLE_SRC="$PROJECT_ROOT/$TRIPLE_MODULE/src/main/java/com/alipay/sofa/rpc"
TRIPLE_TEST="$PROJECT_ROOT/$TRIPLE_MODULE/src/test/java/com/alipay/sofa/rpc"

mkdir -p "$SPRINTS_DIR"

# ── 颜色 ─────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'
BOLD='\033[1m'; NC='\033[0m'
log()      { echo -e "$1"; }
hi_pass()  { log "${GREEN}✅ $1${NC}"; }
hi_fail()  { log "${RED}❌ $1${NC}"; }
hi_info()  { log "${CYAN}ℹ  $1${NC}"; }
hi_warn()  { log "${YELLOW}⚠  $1${NC}"; }

# ── 评分变量 ─────────────────────────────────────────────────────────────────
TOTAL_SCORE=0
MUST_FAIL_COUNT=0
REPORT_LINES=()

add_score() {
    local item="$1" score="$2" max="$3" status="$4" detail="$5"
    TOTAL_SCORE=$(( TOTAL_SCORE + score ))
    REPORT_LINES+=("| $item | $score/$max | $status | $detail |")
    if [[ "$status" == "FAIL" ]]; then
        MUST_FAIL_COUNT=$(( MUST_FAIL_COUNT + 1 ))
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# M1: Transport 层接口文件存在 (15分)
# ─────────────────────────────────────────────────────────────────────────────
check_m1_transport_files() {
    hi_info "[M1] 检查 Transport 层接口文件..."

    local stream_pkg="$TRIPLE_SRC/transport/triple/stream"
    local required_files=(
        "$stream_pkg/RpcStream.java"
        "$stream_pkg/RpcStreamListener.java"
        "$stream_pkg/Http2RpcStream.java"
    )

    local missing=0
    for f in "${required_files[@]}"; do
        if [[ -f "$f" ]]; then
            hi_pass "  $(basename $f)"
        else
            hi_fail "  缺少: $f"
            missing=$(( missing + 1 ))
        fi
    done

    if [[ $missing -eq 0 ]]; then
        add_score "M1-Transport文件" 15 15 "PASS" "RpcStream/RpcStreamListener/Http2RpcStream 均存在"
    elif [[ $missing -lt ${#required_files[@]} ]]; then
        local score=$(( 15 * (${#required_files[@]} - missing) / ${#required_files[@]} ))
        add_score "M1-Transport文件" $score 15 "FAIL" "缺少 $missing/${#required_files[@]} 个文件"
    else
        add_score "M1-Transport文件" 0 15 "FAIL" "Transport 层接口文件全部缺失（S0 未完成）"
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# M2: 核心抽象类存在 (15分)
# ─────────────────────────────────────────────────────────────────────────────
check_m2_core_classes() {
    hi_info "[M2] 检查核心抽象类..."

    local codec_pkg="$TRIPLE_SRC/transport/triple/codec"
    local call_pkg="$TRIPLE_SRC/transport/triple/call"
    local required_files=(
        "$codec_pkg/RpcCodec.java"
        "$codec_pkg/GrpcCodec.java"
        "$call_pkg/RpcCall.java"
        "$call_pkg/UnaryRpcCall.java"
    )

    local missing=0
    for f in "${required_files[@]}"; do
        if [[ -f "$f" ]]; then
            hi_pass "  $(basename $f)"
        else
            hi_fail "  缺少: $f"
            missing=$(( missing + 1 ))
        fi
    done

    if [[ $missing -eq 0 ]]; then
        add_score "M2-核心类" 15 15 "PASS" "RpcCodec/GrpcCodec/RpcCall/UnaryRpcCall 均存在"
    elif [[ $missing -lt ${#required_files[@]} ]]; then
        local score=$(( 15 * (${#required_files[@]} - missing) / ${#required_files[@]} ))
        add_score "M2-核心类" $score 15 "FAIL" "缺少 $missing/${#required_files[@]} 个核心类"
    else
        add_score "M2-核心类" 0 15 "FAIL" "核心类全部缺失（S1/S2 未完成）"
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# M3: 测试通过 (25分)
# ─────────────────────────────────────────────────────────────────────────────
check_m3_tests() {
    hi_info "[M3] 运行 remoting-triple 模块测试..."

    local test_log="/tmp/sofarpc-triple-test-${SPRINT_N}.txt"

    if mvn install -pl remoting/remoting-triple -am \
        -DfailIfNoTests=false \
        -Dmaven.test.failure.ignore=false \
        -q 2>"$test_log"; then
        hi_pass "所有测试通过"
        add_score "M3-测试" 25 25 "PASS" "mvn test -pl remoting/remoting-triple 通过"
    else
        local failures
        failures=$(grep -c "FAILURE\|ERROR" "$test_log" 2>/dev/null || echo "?")
        hi_fail "测试失败（约 $failures 个失败）"
        tail -30 "$test_log" >&2
        add_score "M3-测试" 0 25 "FAIL" "测试失败，详见 $test_log"
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# M4: SPI 注册正确 (15分)
# ─────────────────────────────────────────────────────────────────────────────
check_m4_spi() {
    hi_info "[M4] 检查 SPI 注册..."

    local spi_dir="$PROJECT_ROOT/$TRIPLE_MODULE/src/main/resources/META-INF/services/sofa-rpc"
    local server_spi="$spi_dir/com.alipay.sofa.rpc.server.Server"
    local transport_spi="$spi_dir/com.alipay.sofa.rpc.transport.ClientTransport"

    local spi_ok=0
    local spi_total=2

    # 检查 TripleServer SPI (tri=)
    if [[ -f "$server_spi" ]] && grep -q "^tri=" "$server_spi" 2>/dev/null; then
        hi_pass "  ServerFactory SPI: tri= 存在"
        spi_ok=$(( spi_ok + 1 ))
    else
        hi_fail "  ServerFactory SPI: tri= 缺失或文件不存在"
    fi

    # 检查 TripleClientTransport SPI (tri=)
    if [[ -f "$transport_spi" ]] && grep -q "^tri=" "$transport_spi" 2>/dev/null; then
        hi_pass "  ClientTransportFactory SPI: tri= 存在"
        spi_ok=$(( spi_ok + 1 ))
    else
        hi_fail "  ClientTransportFactory SPI: tri= 缺失或文件不存在"
    fi

    if [[ $spi_ok -eq $spi_total ]]; then
        add_score "M4-SPI注册" 15 15 "PASS" "tri= 在 Server/ClientTransport 均正确注册"
    elif [[ $spi_ok -gt 0 ]]; then
        add_score "M4-SPI注册" 8 15 "FAIL" "SPI 注册不完整（$spi_ok/$spi_total）"
    else
        add_score "M4-SPI注册" 0 15 "FAIL" "tri= SPI 注册全部缺失"
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# M5: 新代码不引入 grpc-shaded 包 (15分)
# ─────────────────────────────────────────────────────────────────────────────
check_m5_no_grpc_shaded() {
    hi_info "[M5] 检查新代码是否引入 grpc-shaded..."

    # 只检查新建的 stream/、codec/、call/ 包
    local new_pkgs=(
        "$TRIPLE_SRC/transport/triple/stream"
        "$TRIPLE_SRC/transport/triple/codec"
        "$TRIPLE_SRC/transport/triple/call"
    )

    local violations=0
    for pkg_dir in "${new_pkgs[@]}"; do
        if [[ -d "$pkg_dir" ]]; then
            local found
            found=$( (grep -r "import io\.grpc\." "$pkg_dir" 2>/dev/null || true) | wc -l | tr -d ' ')
            if [[ "$found" -gt 0 ]]; then
                hi_fail "  $pkg_dir 中存在 $found 处 io.grpc.* import"
                (grep -r "import io\.grpc\." "$pkg_dir" 2>/dev/null || true) | head -5 >&2
                violations=$(( violations + found ))
            fi
        fi
    done

    if [[ $violations -eq 0 ]]; then
        hi_pass "  新代码包中无 io.grpc.* import"
        add_score "M5-无grpc-shaded" 15 15 "PASS" "stream/codec/call 包均不引入 grpc-shaded"
    else
        add_score "M5-无grpc-shaded" 0 15 "FAIL" "发现 $violations 处 io.grpc.* 引入（违反架构约束）"
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# M6: pom.xml 移除 grpc-netty-shaded (S6前WARN，S6后FAIL)
# ─────────────────────────────────────────────────────────────────────────────
check_m6_pom_cleanup() {
    hi_info "[M6] 检查 pom.xml grpc-netty-shaded 依赖..."

    local pom="$PROJECT_ROOT/$TRIPLE_MODULE/pom.xml"
    if grep -q "grpc-netty-shaded" "$pom" 2>/dev/null; then
        if [[ "$SPRINT_N" -ge 6 ]]; then
            hi_fail "  pom.xml 仍包含 grpc-netty-shaded（S6 应已移除）"
            add_score "M6-pom清理" 0 10 "FAIL" "S6 后 pom.xml 仍有 grpc-netty-shaded 依赖"
        else
            hi_warn "  pom.xml 仍包含 grpc-netty-shaded（S${SPRINT_N}，待 S6 清理）"
            add_score "M6-pom清理" 5 10 "WARN" "grpc-netty-shaded 尚未移除（S6 前可接受）"
        fi
    else
        hi_pass "  pom.xml 已移除 grpc-netty-shaded"
        add_score "M6-pom清理" 10 10 "PASS" "pom.xml 已不含 grpc-netty-shaded"
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# M7: gRPC wire 协议后向兼容 (15分)
# ─────────────────────────────────────────────────────────────────────────────
check_m7_grpc_compat() {
    hi_info "[M7] 检查 gRPC wire 协议兼容性..."

    local grpc_codec_test="$TRIPLE_TEST/transport/triple/codec/GrpcCodecTest.java"
    local backcompat_test="$TRIPLE_TEST/transport/triple/GrpcBackwardCompatTest.java"

    if [[ -f "$grpc_codec_test" ]] || [[ -f "$backcompat_test" ]]; then
        # 如果测试文件存在，尝试运行
        local test_log="/tmp/sofarpc-grpc-compat-${SPRINT_N}.txt"
        if mvn install -pl remoting/remoting-triple -am \
            -Dtest="GrpcCodecTest,GrpcBackwardCompatTest" \
            -DfailIfNoTests=false \
            -Dmaven.test.failure.ignore=false \
            -q 2>"$test_log" 2>/dev/null; then
            hi_pass "  gRPC 兼容性测试通过"
            add_score "M7-gRPC兼容" 15 15 "PASS" "GrpcCodecTest/BackwardCompatTest 通过"
        else
            hi_fail "  gRPC 兼容性测试失败"
            add_score "M7-gRPC兼容" 0 15 "FAIL" "兼容性测试失败，详见 $test_log"
        fi
    else
        hi_warn "  GrpcCodecTest 未创建（S1/S6 前可接受）"
        if [[ "$SPRINT_N" -ge 1 ]]; then
            add_score "M7-gRPC兼容" 5 15 "WARN" "GrpcCodecTest 尚未创建，建议 S1 添加"
        else
            add_score "M7-gRPC兼容" 8 15 "WARN" "S0 阶段，GrpcCodecTest 尚未创建"
        fi
    fi
}

# ─────────────────────────────────────────────────────────────────────────────
# Sprint 特定检查（根据当前 Sprint 动态调整期望）
# ─────────────────────────────────────────────────────────────────────────────
check_sprint_specific() {
    hi_info "[Sprint ${SPRINT_N}] Sprint 特定检查..."

    case "$SPRINT_N" in
        0)
            # S0: 仅要求 stream/ 包，codec/call/ 可以不存在
            if [[ -d "$TRIPLE_SRC/transport/triple/stream" ]]; then
                hi_pass "  S0: stream/ 包已创建"
            else
                hi_fail "  S0: stream/ 包缺失"
            fi
            ;;
        1)
            # S1: 要求 codec/ 包存在且有 GrpcCodec
            if [[ -f "$TRIPLE_SRC/transport/triple/codec/GrpcCodec.java" ]]; then
                hi_pass "  S1: GrpcCodec 已实现"
            else
                hi_fail "  S1: GrpcCodec 缺失"
            fi
            ;;
        2)
            # S2: 要求 call/ 包存在且有 UnaryRpcCall
            if [[ -f "$TRIPLE_SRC/transport/triple/call/UnaryRpcCall.java" ]]; then
                hi_pass "  S2: UnaryRpcCall 已实现"
            else
                hi_fail "  S2: UnaryRpcCall 缺失"
            fi
            ;;
        3)
            # S3: Http2StreamHandler 应使用工厂模式
            if grep -q "HttpServerTransportListenerFactory" \
                "$TRIPLE_SRC/transport/triple/Http2StreamHandler.java" 2>/dev/null; then
                hi_pass "  S3: Http2StreamHandler 已使用工厂模式"
            else
                hi_warn "  S3: Http2StreamHandler 尚未改为工厂模式"
            fi
            ;;
        4)
            # S4: TripleClientTransport 应不再直接实例化 NettyChannelBuilder
            if grep -q "NettyChannelBuilder" \
                "$TRIPLE_SRC/transport/triple/TripleClientTransport.java" 2>/dev/null; then
                hi_warn "  S4: TripleClientTransport 仍使用 NettyChannelBuilder（grpc-shaded）"
            else
                hi_pass "  S4: TripleClientTransport 已迁移出 NettyChannelBuilder"
            fi
            ;;
        5)
            # S5: 应存在 ServerStreamRpcCall 或 BidiStreamRpcCall
            if [[ -f "$TRIPLE_SRC/transport/triple/call/ServerStreamRpcCall.java" ]] ||
               [[ -f "$TRIPLE_SRC/transport/triple/call/BidiStreamRpcCall.java" ]]; then
                hi_pass "  S5: 流式 RpcCall 实现存在"
            else
                hi_fail "  S5: 流式 RpcCall 未实现（ServerStreamRpcCall/BidiStreamRpcCall）"
            fi
            ;;
        6)
            # S6: pom 必须移除 grpc-netty-shaded
            local pom="$PROJECT_ROOT/$TRIPLE_MODULE/pom.xml"
            if grep -q "grpc-netty-shaded" "$pom" 2>/dev/null; then
                hi_fail "  S6: pom.xml 仍有 grpc-netty-shaded（S6 必须清理）"
            else
                hi_pass "  S6: pom.xml 已移除 grpc-netty-shaded"
            fi
            ;;
    esac
}

# ─────────────────────────────────────────────────────────────────────────────
# 主流程
# ─────────────────────────────────────────────────────────────────────────────
main() {
    log ""
    log "${BOLD}SOFARPC Triple Migration — Sprint ${SPRINT_N} 评估开始${NC}"
    log "项目根目录: $PROJECT_ROOT"
    log "报告路径:   $REPORT_FILE"
    log ""

    cd "$PROJECT_ROOT"

    check_m1_transport_files
    check_m2_core_classes
    check_m3_tests
    check_m4_spi
    check_m5_no_grpc_shaded
    check_m6_pom_cleanup
    check_m7_grpc_compat
    check_sprint_specific

    # WARN 项不计入 MUST_FAIL
    # MUST FAIL 时总分上限 69
    if [[ $MUST_FAIL_COUNT -gt 0 ]]; then
        TOTAL_SCORE=$(( TOTAL_SCORE > 69 ? 69 : TOTAL_SCORE ))
        hi_fail "存在 $MUST_FAIL_COUNT 个 MUST FAIL，总分上限 69"
    fi

    log ""
    log "${BOLD}评估结果${NC}"
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log "总分: ${BOLD}${TOTAL_SCORE}/100${NC}"
    [[ $MUST_FAIL_COUNT -gt 0 ]] && hi_fail "MUST FAIL 数: $MUST_FAIL_COUNT" || hi_pass "所有 MUST 项通过"
    log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    # 写入 qa-report.md
    {
        echo "# Sprint ${SPRINT_N} QA Report — SOFARPC Triple Migration"
        echo ""
        echo "**生成时间**: $(date -u +"%Y-%m-%d %H:%M:%S UTC")"
        echo "**总分**: ${TOTAL_SCORE}/100"
        echo "**MUST FAIL 数**: ${MUST_FAIL_COUNT}"
        echo ""
        echo "## 评分明细"
        echo ""
        echo "| 检查项 | 得分 | 状态 | 说明 |"
        echo "|--------|------|------|------|"
        for line in "${REPORT_LINES[@]}"; do
            echo "$line"
        done
        echo ""
        echo "## 最终评定"
        echo ""
        if [[ $TOTAL_SCORE -ge 85 && $MUST_FAIL_COUNT -eq 0 ]]; then
            echo "✅ **PASS** — Sprint ${SPRINT_N} 质量达标（${TOTAL_SCORE}/100）"
        elif [[ $TOTAL_SCORE -ge 70 ]]; then
            echo "⚠️ **ACCEPTABLE** — Sprint ${SPRINT_N} 基本达标（${TOTAL_SCORE}/100），建议修复 FAIL 项"
        else
            echo "❌ **FAIL** — Sprint ${SPRINT_N} 质量不达标（${TOTAL_SCORE}/100），需修复后重新评估"
        fi
        echo ""
        echo "总分: ${TOTAL_SCORE}/100"
    } > "$REPORT_FILE"

    hi_pass "评估报告已写入: $REPORT_FILE"
    log ""
}

main
