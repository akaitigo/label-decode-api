# label-decode-api

食品の一括表示テキストを構造化データに変換する gRPC API。原材料名の正規化、添加物の用途別分類、アレルゲン自動検出を提供する。

APIキー認証・レート制限・TLS設定を備え、本番利用に対応しています（[ADR-003](docs/adr/003-api-key-auth-rate-limit-tls.md)）。

## 技術スタック

- Kotlin / Quarkus（gRPC Server）
- Python（NLP パーサー）
- PostgreSQL（添加物マスタ）
- buf（Proto管理）

## クイックスタート

```bash
# リポジトリをクローン
git clone git@github.com:akaitigo/label-decode-api.git
cd label-decode-api

# セットアップ（ツール自動インストール）
bash startup.sh

# PostgreSQL を起動（Docker）
docker run -d --name labeldecode-db \
  -e POSTGRES_DB=labeldecode \
  -e POSTGRES_USER=labeldecode \
  -e POSTGRES_PASSWORD=labeldecode \
  -p 5432:5432 postgres:16

# 環境変数を設定
export DB_USERNAME=labeldecode
export DB_PASSWORD=labeldecode
export DB_URL=jdbc:postgresql://localhost:5432/labeldecode

# ビルド & テスト
make check

# サーバー起動
./gradlew quarkusDev

# gRPC 呼び出し例
grpcurl -plaintext -d '{"raw_text": "小麦粉、砂糖、バター、卵/ソルビン酸K（保存料）、カラメル色素"}' \
  localhost:9090 akaitigo.labeldecode.v1.LabelDecodeService/ParseLabel
```

## デモ出力

```json
// ParseLabel("小麦粉、砂糖、バター、卵/ソルビン酸K（保存料）、カラメル色素")
{
  "label": {
    "ingredients": [
      {"name": "小麦粉", "allergenSources": []},
      {"name": "砂糖", "allergenSources": []},
      {"name": "バター", "allergenSources": []},
      {"name": "卵", "allergenSources": []}
    ],
    "additives": [
      {"name": "ソルビン酸K", "category": "保存料"},
      {"name": "カラメル色素", "category": "その他"}
    ],
    "allergens": [
      {"name": "小麦", "type": "ALLERGEN_TYPE_MANDATORY", "sourceText": "小麦"},
      {"name": "卵", "type": "ALLERGEN_TYPE_MANDATORY", "sourceText": "卵"},
      {"name": "乳", "type": "ALLERGEN_TYPE_MANDATORY", "sourceText": "乳"}
    ],
    "originalText": "小麦粉、砂糖、バター、卵/ソルビン酸K（保存料）、カラメル色素"
  }
}
```

## アーキテクチャ

```
Client ──gRPC──▶ Kotlin/Quarkus Server
                    ├── Label Parser (Python NLP)
                    ├── Additive Classifier
                    ├── Allergen Detector
                    └── PostgreSQL (添加物マスタ)
```

- gRPC サーバーがリクエストを受付、Python パーサーで食品表示テキストを解析
- 添加物マスタ（PostgreSQL）を参照して用途別分類を実施
- 特定原材料等28品目のアレルゲンを自動検出

## API

gRPC サービス: `akaitigo.labeldecode.v1.LabelDecodeService`

| RPC | 説明 |
|-----|------|
| `ParseLabel` | 食品表示テキストを完全に構造化 |
| `DetectAllergens` | アレルゲンのみを検出 |
| `ClassifyAdditives` | 添加物を用途別に分類 |

Proto定義: [`src/main/proto/akaitigo/labeldecode/v1/label_decode_service.proto`](src/main/proto/akaitigo/labeldecode/v1/label_decode_service.proto)

## セキュリティ（本番運用）

設定値のサンプルは [`.env.example`](.env.example)、設計判断は [ADR-003](docs/adr/003-api-key-auth-rate-limit-tls.md) を参照。

### API キー認証

prod プロファイルではデフォルト有効。`API_KEYS` 環境変数（カンマ区切りで複数可）でキーを注入する。
未設定の場合は起動に失敗する（認証なしで本番起動する事故を防ぐ）。

```bash
# キー生成例
export API_KEYS=$(openssl rand -hex 32)

# 呼び出し時は x-api-key メタデータを付与（欠落・不正キーは UNAUTHENTICATED = HTTP 401 相当）
grpcurl -H "x-api-key: $API_KEYS" \
  -proto akaitigo/labeldecode/v1/label_decode_service.proto -import-path src/main/proto \
  -d '{"raw_text": "小麦粉、砂糖"}' \
  <host>:9090 akaitigo.labeldecode.v1.LabelDecodeService/ParseLabel
```

`dev` / `test` プロファイルでは無効（ローカルの `quarkusDev` はキーなしで呼び出せる）。

### レート制限

APIキー単位のインメモリ・トークンバケット（外部サービス不要）。超過は `RESOURCE_EXHAUSTED`（HTTP 429 相当）。

| 環境変数 | 既定値 | 説明 |
|---|---|---|
| `RATE_LIMIT_REQUESTS_PER_MINUTE` | 120 | 毎分の補充レート |
| `RATE_LIMIT_BURST_CAPACITY` | 30 | 瞬間バースト許容量 |

### TLS

PEM 証明書/鍵のパスを環境変数で注入すると gRPC サーバーが TLS で待ち受ける。

```bash
export QUARKUS_GRPC_SERVER_SSL_CERTIFICATE=/etc/tls/server.crt
export QUARKUS_GRPC_SERVER_SSL_KEY=/etc/tls/server.key
```

Cloud Run 等のマネージド環境では前段で TLS が終端されるため設定不要。直接公開する場合のみ設定する。

## ライセンス

MIT
