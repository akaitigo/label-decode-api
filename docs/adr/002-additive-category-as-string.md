# ADR-002: 添加物カテゴリは String 型を維持する

## ステータス
承認済み

## コンテキスト
レビューで Additive.category を enum にする提案があった。しかし Proto の `Additive.category` フィールドは `string` 型（フィールド番号 2）で公開済み。

## 決定
**Proto の category フィールドは string 型を維持する。** Kotlin 側で enum `AdditiveCategory` を導入するかは v2 で検討。

### 理由
1. Proto フィールドの型変更は後方互換性を破壊する（proto.md ルール違反）
2. 添加物カテゴリは厚労省の分類更新で追加される可能性があり、string の方が柔軟
3. DB マスタとの連携で動的にカテゴリが増える設計を意図

## 結果
- バリデーションは gRPC サービス層で行う
- 既知のカテゴリ一覧は LabelParser の正規表現 + DB マスタで管理
