#!/usr/bin/env bash
# =============================================================================
# セッション起動ルーチン
#
# セッション開始時に実行し、ツールの自動インストールとヘルスチェックを行う。
# 状態管理は git log + GitHub Issues で行う。
#
# オプション:
#   --dev    開発サーバーも起動する（Web App/API 向け）
#   --skip-checks  ヘルスチェックをスキップ（デバッグ用）
# =============================================================================
set -euo pipefail

START_DEV=false
SKIP_CHECKS=false

for arg in "$@"; do
  case "$arg" in
    --dev) START_DEV=true ;;
    --skip-checks) SKIP_CHECKS=true ;;
  esac
done

echo "=== Session Startup ==="

# 1. 作業ディレクトリ確認
[ -d ".git" ] || { echo "ERROR: Not in git repository"; exit 1; }

# 1.5. 言語検出と必須ツールの自動インストール
echo "=== Tool auto-install ==="
if [ -f "build.gradle.kts" ] || [ -f "build.gradle" ]; then
  echo "Detected: Kotlin/JVM"
  if [ -f "gradlew" ]; then
    chmod +x gradlew
    echo "Gradle wrapper found. Running detekt check..."
    ./gradlew detekt --dry-run 2>/dev/null && echo "detekt configured." || echo "WARN: detekt not configured in build.gradle.kts"
  else
    echo "WARN: gradlew not found. Run 'gradle wrapper' to generate."
  fi
fi
if [ -f "pyproject.toml" ]; then
  echo "Detected: Python"
  command -v ruff &>/dev/null || { echo "Installing ruff..."; pip install ruff 2>/dev/null || echo "WARN: ruff install failed"; }
fi
# buf (proto管理)
command -v buf &>/dev/null || { echo "Installing buf..."; go install github.com/bufbuild/buf/cmd/buf@v1.50.0 2>/dev/null || echo "WARN: buf install failed"; }
# grpcurl (gRPCテスト)
command -v grpcurl &>/dev/null || { echo "Installing grpcurl..."; go install github.com/fullstorydev/grpcurl/cmd/grpcurl@v1.9.2 2>/dev/null || echo "WARN: grpcurl install failed"; }
# lefthook（全言語共通: git hooks 管理）
command -v lefthook &>/dev/null || { echo "Installing lefthook..."; go install github.com/evilmartians/lefthook@v1.10.10 2>/dev/null || npm install -g lefthook@1.10.10 2>/dev/null || echo "WARN: lefthook install failed"; }
echo "Tool check complete."

# 2. Gitログ読取
echo "=== Recent commits ==="
git log --oneline -10

# 3. ヘルスチェック
if [ "$SKIP_CHECKS" = true ]; then
  echo "=== Health check SKIPPED (--skip-checks) ==="
else
  echo "=== Health check ==="
  if make check 2>&1 | tail -10; then
    echo "All checks passed. Ready to work."
  else
    echo "WARN: Checks failed. Review issues before proceeding."
  fi
fi

# 4. 開発サーバー起動（オプション）
if [ "$START_DEV" = true ]; then
  echo "=== Starting dev server ==="
  if [ -f "Makefile" ] && grep -q "run-dev" Makefile; then
    make run-dev &
    echo "Dev server started (make run-dev)"
  else
    echo "Starting Quarkus dev mode..."
    ./gradlew quarkusDev &
    echo "Dev server started (quarkusDev)"
  fi
fi

echo ""
echo "=== Session started at $(date -u +"%Y-%m-%dT%H:%M:%SZ") ==="
echo "Ready to work. State management: git log + GitHub Issues."
