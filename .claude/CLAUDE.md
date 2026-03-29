# label-decode-api アーキテクチャ概要

## サービス構成

Kotlin/Quarkus gRPC サーバーが食品表示テキスト構造化APIを提供する。

### コンポーネント

- **gRPC Server (Kotlin/Quarkus)**: クライアント向けAPI。リクエスト受付・レスポンス整形
- **Label Parser (Python)**: 食品表示テキストのNLP解析。スラッシュルール対応
- **Additive Classifier**: 添加物の用途別分類（保存料・着色料・甘味料等）
- **Allergen Detector**: 特定原材料等28品目の検出
- **Master Data (PostgreSQL)**: 添加物マスタ・分類辞書

### MVP アーキテクチャ決定

- Python パーサーは Kotlin プロセスからコマンドライン呼び出し（v2 で gRPC 化）
- 添加物マスタは初期データをSQL migration で投入
- 認証なし（MVP スコープ外）

## 外部サービス連携

- GCP Cloud Run（デプロイ先）
- PostgreSQL（添加物マスタデータ）

## 主要な設計判断

- ADR は docs/adr/ に記録
