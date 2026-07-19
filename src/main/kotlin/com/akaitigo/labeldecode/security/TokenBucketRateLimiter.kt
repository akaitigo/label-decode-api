package com.akaitigo.labeldecode.security

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

private const val NANOS_PER_MINUTE = 60_000_000_000.0

/**
 * クライアント単位のインメモリ・トークンバケット式レートリミッタ。
 *
 * 外部サービス（Redis 等）に依存せず、単一インスタンス内で完結する。 バケット容量は [burstCapacity]、補充レートは
 * [requestsPerMinute]（毎分）で連続的に補充する。
 *
 * @param nanoTimeSource テスト用に差し替え可能な単調増加ナノ秒時刻源
 */
class TokenBucketRateLimiter(
    requestsPerMinute: Long,
    private val burstCapacity: Long,
    private val maxTrackedClients: Int = DEFAULT_MAX_TRACKED_CLIENTS,
    private val nanoTimeSource: () -> Long = System::nanoTime,
) {
  companion object {
    const val DEFAULT_MAX_TRACKED_CLIENTS = 10_000
  }

  init {
    require(requestsPerMinute > 0) { "requestsPerMinute must be positive: $requestsPerMinute" }
    require(burstCapacity > 0) { "burstCapacity must be positive: $burstCapacity" }
    require(maxTrackedClients > 0) { "maxTrackedClients must be positive: $maxTrackedClients" }
  }

  private val refillTokensPerNano = requestsPerMinute / NANOS_PER_MINUTE

  /** バケットが満タンまで回復するのに要する時間。これ以上放置されたバケットは破棄してよい。 */
  private val staleThresholdNanos = (burstCapacity / refillTokensPerNano).toLong()

  private class Bucket(
      var tokens: Double,
      var lastRefillNanos: Long,
  )

  private val buckets = ConcurrentHashMap<String, Bucket>()

  internal val trackedClientCount: Int
    get() = buckets.size

  /** 1 リクエスト分のトークン取得を試みる。取得できなければ false（= レート超過）。 */
  fun tryAcquire(clientId: String): Boolean {
    val now = nanoTimeSource()
    evictStaleClientsIfNeeded(now)
    var acquired = false
    buckets.compute(clientId) { _, existing ->
      val bucket = existing ?: Bucket(burstCapacity.toDouble(), now)
      refill(bucket, now)
      if (bucket.tokens >= 1.0) {
        bucket.tokens -= 1.0
        acquired = true
      }
      bucket
    }
    return acquired
  }

  private fun refill(
      bucket: Bucket,
      now: Long,
  ) {
    val elapsedNanos = now - bucket.lastRefillNanos
    if (elapsedNanos > 0) {
      bucket.tokens =
          min(burstCapacity.toDouble(), bucket.tokens + elapsedNanos * refillTokensPerNano)
      bucket.lastRefillNanos = now
    }
  }

  /** メモリ枯渇防止: 追跡クライアント数が上限を超えたら、回復済みの古いバケットを破棄する。 */
  private fun evictStaleClientsIfNeeded(now: Long) {
    if (buckets.size <= maxTrackedClients) {
      return
    }
    buckets.entries.removeIf { entry -> now - entry.value.lastRefillNanos >= staleThresholdNanos }
  }
}
