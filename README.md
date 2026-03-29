# label-decode-api

食品の一括表示テキストを構造化データに変換する gRPC API。原材料名の正規化、添加物の用途別分類、アレルゲン自動検出を提供する。

> **Warning**: このプロジェクトはMVPです。本番利用前に認証・認可、レート制限、TLS設定が必要です。

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

## API

gRPC サービス: `akaitigo.labeldecode.v1.LabelDecodeService`

| RPC | 説明 |
|-----|------|
| `ParseLabel` | 食品表示テキストを完全に構造化 |
| `DetectAllergens` | アレルゲンのみを検出 |
| `ClassifyAdditives` | 添加物を用途別に分類 |

Proto定義: [`proto/labeldecode/v1/label_decode_service.proto`](proto/labeldecode/v1/label_decode_service.proto)

## ライセンス

MIT
