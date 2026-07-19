package com.akaitigo.labeldecode.security

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import java.util.Optional

/**
 * セキュリティ関連の設定。
 *
 * プロパティは `labeldecode.security.*` に対応する（例: `labeldecode.security.auth.api-keys`）。
 */
@ConfigMapping(prefix = "labeldecode.security")
interface SecurityConfig {
  fun auth(): Auth

  fun rateLimit(): RateLimit

  interface Auth {
    /** API キー認証の有効/無効。prod ではデフォルト有効。 */
    @WithDefault("true") fun enabled(): Boolean

    /** 受け入れる API キーの一覧（カンマ区切り）。`API_KEYS` 環境変数から注入する。 */
    fun apiKeys(): Optional<List<String>>
  }

  interface RateLimit {
    /** レート制限の有効/無効。prod ではデフォルト有効。 */
    @WithDefault("true") fun enabled(): Boolean

    /** クライアントあたりの毎分リクエスト数（トークン補充レート）。 */
    @WithDefault("120") fun requestsPerMinute(): Long

    /** 瞬間的に許容するバースト量（トークンバケット容量）。 */
    @WithDefault("30") fun burstCapacity(): Long
  }
}
