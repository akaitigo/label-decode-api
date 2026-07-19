package com.akaitigo.labeldecode.security

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.quarkus.grpc.GlobalInterceptor
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.spi.Prioritized
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** 認証インターセプタの実行優先度。レート制限より先に実行する（値が大きいほど先）。 */
internal const val AUTH_INTERCEPTOR_PRIORITY = 100

/**
 * `x-api-key` メタデータによる API キー認証。
 *
 * 有効時はキー未提示・不正キーを `UNAUTHENTICATED`（HTTP 401 相当）で拒否する。 dev / test プロファイルでは
 * `labeldecode.security.auth.enabled=false` で無効化される。
 */
@ApplicationScoped
@GlobalInterceptor
class ApiKeyAuthInterceptor(
    private val config: SecurityConfig,
) : ServerInterceptor, Prioritized {
  companion object {
    val API_KEY_METADATA_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)
  }

  private val configuredKeys: List<ByteArray> by lazy {
    config.auth().apiKeys().orElse(emptyList()).mapNotNull { key ->
      key.trim().ifBlank { null }?.toByteArray(StandardCharsets.UTF_8)
    }
  }

  /** 認証が有効なのにキー未設定のまま起動しない（fail-fast）。 */
  @Suppress("UnusedParameter") // CDI の @Observes 契約上、イベント引数が必要
  fun validateOnStartup(
      @Observes event: StartupEvent,
  ) {
    if (config.auth().enabled()) {
      check(configuredKeys.isNotEmpty()) {
        "API key authentication is enabled but no API keys are configured. " +
            "Set the API_KEYS environment variable (comma-separated keys)."
      }
    }
  }

  override fun <ReqT, RespT> interceptCall(
      call: ServerCall<ReqT, RespT>,
      headers: Metadata,
      next: ServerCallHandler<ReqT, RespT>,
  ): ServerCall.Listener<ReqT> {
    val providedKey = headers.get(API_KEY_METADATA_KEY)
    val authorized = !config.auth().enabled() || (providedKey != null && isAuthorized(providedKey))
    if (!authorized) {
      call.close(
          Status.UNAUTHENTICATED.withDescription("missing or invalid API key"),
          Metadata(),
      )
      return NoopServerCallListener()
    }
    return next.startCall(call, headers)
  }

  override fun getPriority(): Int = AUTH_INTERCEPTOR_PRIORITY

  private fun isAuthorized(providedKey: String): Boolean {
    val provided = providedKey.toByteArray(StandardCharsets.UTF_8)
    // タイミング攻撃によるキー推測を防ぐため、全キーと定数時間比較を行い早期 return しない
    var matched = false
    for (key in configuredKeys) {
      if (MessageDigest.isEqual(key, provided)) {
        matched = true
      }
    }
    return matched
  }
}

/** 拒否済みコールに対して何もしないリスナー。 */
internal class NoopServerCallListener<ReqT> : ServerCall.Listener<ReqT>()
