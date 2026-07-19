package com.akaitigo.labeldecode.security

import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.spi.Prioritized

/** レート制限インターセプタの実行優先度。認証の後に実行する。 */
internal const val RATE_LIMIT_INTERCEPTOR_PRIORITY = 50

private const val UNKNOWN_CLIENT_ID = "unknown"

/**
 * クライアント単位のレート制限。超過は `RESOURCE_EXHAUSTED`（HTTP 429 相当）で拒否する。
 *
 * クライアント識別は API キー（認証有効時）、なければ接続元アドレスを用いる。 認証インターセプタが先に実行されるため、ここに到達する API キーは検証済み。
 */
@ApplicationScoped
@GlobalInterceptor
class RateLimitInterceptor(
    private val config: SecurityConfig,
) : ServerInterceptor, Prioritized {
  private val limiter: TokenBucketRateLimiter by lazy {
    TokenBucketRateLimiter(
        requestsPerMinute = config.rateLimit().requestsPerMinute(),
        burstCapacity = config.rateLimit().burstCapacity(),
    )
  }

  override fun <ReqT, RespT> interceptCall(
      call: ServerCall<ReqT, RespT>,
      headers: Metadata,
      next: ServerCallHandler<ReqT, RespT>,
  ): ServerCall.Listener<ReqT> {
    val allowed =
        !config.rateLimit().enabled() || limiter.tryAcquire(resolveClientId(call, headers))
    if (!allowed) {
      call.close(
          Status.RESOURCE_EXHAUSTED.withDescription("rate limit exceeded, retry later"),
          Metadata(),
      )
      return NoopServerCallListener()
    }
    return next.startCall(call, headers)
  }

  override fun getPriority(): Int = RATE_LIMIT_INTERCEPTOR_PRIORITY

  private fun <ReqT, RespT> resolveClientId(
      call: ServerCall<ReqT, RespT>,
      headers: Metadata,
  ): String =
      headers.get(ApiKeyAuthInterceptor.API_KEY_METADATA_KEY)
          ?: call.attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)?.toString()
          ?: UNKNOWN_CLIENT_ID
}
