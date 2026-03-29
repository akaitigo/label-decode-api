# label-decode-api
<!-- メンテナンス指針: 各行に問う「この行を消したらエージェントは間違えるか？」→ No なら削除 -->

## コマンド
- ビルド: `make build`
- テスト: `make test`
- lint: `make lint`
- フォーマット: `make format`
- 全チェック: `make check`

## ワークフロー
1. research.md を作成（調査結果の記録）
2. plan.md を作成（実装計画。人間承認まで実装禁止）
3. 承認後に実装開始。plan.md のtodoを進捗管理に使用

## ルール
- ADR: docs/adr/ 参照。新規決定はADRを書いてから実装
- テスト: 機能追加時は必ずテストを同時に書く
- lint設定の変更禁止（ADR必須）
- critical ruleは本ファイルの先頭に配置（earlier-instruction bias対策）
- Kotlin: `~/.claude/rules/kotlin.md` のルールに従うこと（`!!` 禁止）
- Proto/gRPC: `~/.claude/rules/proto.md` のルールに従うこと（フィールド番号再利用禁止）

## 構造
```
src/main/kotlin/com/akaitigo/labeldecode/  -- Kotlin gRPC サーバー
src/main/resources/                         -- 設定ファイル
src/test/kotlin/com/akaitigo/labeldecode/  -- テスト
src/main/proto/akaitigo/labeldecode/v1/     -- Proto定義
parser/                                     -- Python NLP パーサー
docs/adr/                                   -- Architecture Decision Records
```

## 禁止事項
- `!!`（Kotlin非null断言）→ `?.let {}` / `?: throw` / `requireNotNull()` を使う
- print文のコミット（detekt ForbiddenMethodCall で検出）
- TODO/FIXME コメントのコミット（detekt ForbiddenComment で検出、Issue化すること）
- .env・credentials のコミット
- lint設定の無効化（ルール単位の disable 含む）
- Proto フィールド番号の再利用（`reserved` で予約）
- 詳細は `detekt.yml` がソースオブトゥルース

## Hooks
- 設定: .claude/settings.json 参照
- PostToolUse: Edit/Write 時に detekt 自動実行
- PostToolUse: .proto 編集時に buf lint/format 自動実行

## 状態管理
- git log + GitHub Issues でセッション間の状態を管理
- セッション開始: `bash startup.sh`（ツール自動インストール + ヘルスチェック）

## コンテキスト衛生
- .gitignore / .claudeignore で不要ファイルを除外
- バイナリ、キャッシュ、node_modules等がコンテキストを汚染しないこと
- 1000行超のファイルはシグネチャのみ参照
