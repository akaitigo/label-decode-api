#!/usr/bin/env bash
# =============================================================================
# gRPC 契約テスト — スモークテスト
#
# サービスが契約（proto定義）通りに動作することを検証する。
# grpcurl を使って実際にRPCを呼び出し、レスポンスの構造を確認する。
#
# 前提:
#   - grpcurl がインストール済み
#   - 対象サービスが起動済み
# =============================================================================

set -euo pipefail

SERVICE_HOST="${GRPC_HOST:-localhost}"
SERVICE_PORT="${GRPC_PORT:-9090}"
TARGET="${SERVICE_HOST}:${SERVICE_PORT}"

GRPCURL="grpcurl -plaintext"

PASS=0
FAIL=0

run_test() {
    local name="$1"
    shift
    echo -n "  CONTRACT: ${name} ... "
    if OUTPUT=$("$@" 2>&1); then
        echo "PASS"
        PASS=$((PASS + 1))
    else
        echo "FAIL"
        echo "    Output: ${OUTPUT}"
        FAIL=$((FAIL + 1))
    fi
}

echo "=== 契約テスト: スモーク ==="
echo "Target: ${TARGET}"
echo ""

# --- サービスがproto定義通りのRPCを公開しているか ---

echo "[サービスメソッド確認]"
run_test "LabelDecodeService が公開されている" \
    bash -c "$GRPCURL ${TARGET} list | grep -q 'akaitigo.labeldecode.v1.LabelDecodeService'"

run_test "ParseLabel メソッドが存在する" \
    bash -c "$GRPCURL ${TARGET} describe akaitigo.labeldecode.v1.LabelDecodeService.ParseLabel"

run_test "DetectAllergens メソッドが存在する" \
    bash -c "$GRPCURL ${TARGET} describe akaitigo.labeldecode.v1.LabelDecodeService.DetectAllergens"

run_test "ClassifyAdditives メソッドが存在する" \
    bash -c "$GRPCURL ${TARGET} describe akaitigo.labeldecode.v1.LabelDecodeService.ClassifyAdditives"

# --- レスポンスの構造確認 ---

echo ""
echo "[レスポンス構造確認]"

run_test "ParseLabel のレスポンスに label フィールドがある" \
    bash -c "$GRPCURL -d '{\"raw_text\": \"小麦粉、砂糖/ソルビン酸K（保存料）\"}' ${TARGET} akaitigo.labeldecode.v1.LabelDecodeService/ParseLabel | jq -e '.label'"

run_test "DetectAllergens のレスポンスに allergens 配列がある" \
    bash -c "$GRPCURL -d '{\"raw_text\": \"小麦粉、卵、乳\"}' ${TARGET} akaitigo.labeldecode.v1.LabelDecodeService/DetectAllergens | jq -e '.allergens'"

# --- エラーコード確認 ---

echo ""
echo "[エラーコード確認]"

run_test "空テキストで INVALID_ARGUMENT が返る" \
    bash -c "$GRPCURL -d '{\"raw_text\": \"\"}' ${TARGET} akaitigo.labeldecode.v1.LabelDecodeService/ParseLabel 2>&1 | grep -q 'INVALID_ARGUMENT'"

# --- 結果 ---

echo ""
echo "=== 契約テスト結果 ==="
echo "  PASS: ${PASS}"
echo "  FAIL: ${FAIL}"
echo "  TOTAL: $((PASS + FAIL))"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
