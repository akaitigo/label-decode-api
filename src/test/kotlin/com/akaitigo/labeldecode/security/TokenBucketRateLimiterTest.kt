package com.akaitigo.labeldecode.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val NANOS_PER_SECOND = 1_000_000_000L

class TokenBucketRateLimiterTest {
  private var nowNanos = 0L

  private fun limiter(
      requestsPerMinute: Long = 60,
      burstCapacity: Long = 3,
      maxTrackedClients: Int = TokenBucketRateLimiter.DEFAULT_MAX_TRACKED_CLIENTS,
  ): TokenBucketRateLimiter =
      TokenBucketRateLimiter(requestsPerMinute, burstCapacity, maxTrackedClients) { nowNanos }

  @Test
  fun `allows requests up to burst capacity then denies`() {
    val limiter = limiter(burstCapacity = 3)

    repeat(3) { assertTrue(limiter.tryAcquire("client-a"), "request ${it + 1} should pass") }
    assertFalse(limiter.tryAcquire("client-a"), "request over burst capacity should be denied")
  }

  @Test
  fun `refills tokens over elapsed time`() {
    val limiter = limiter(requestsPerMinute = 60, burstCapacity = 3)
    repeat(3) { limiter.tryAcquire("client-a") }
    assertFalse(limiter.tryAcquire("client-a"))

    // 60 req/分 = 1 トークン/秒。1 秒進めると 1 リクエストだけ通る
    nowNanos += NANOS_PER_SECOND
    assertTrue(limiter.tryAcquire("client-a"))
    assertFalse(limiter.tryAcquire("client-a"))
  }

  @Test
  fun `does not accumulate tokens beyond burst capacity after long idle`() {
    val limiter = limiter(requestsPerMinute = 60, burstCapacity = 3)
    repeat(3) { limiter.tryAcquire("client-a") }

    // 1 時間放置してもバースト容量までしか回復しない
    nowNanos += 3600 * NANOS_PER_SECOND
    repeat(3) { assertTrue(limiter.tryAcquire("client-a"), "request ${it + 1} should pass") }
    assertFalse(limiter.tryAcquire("client-a"))
  }

  @Test
  fun `tracks buckets per client independently`() {
    val limiter = limiter(burstCapacity = 2)
    repeat(2) { limiter.tryAcquire("client-a") }
    assertFalse(limiter.tryAcquire("client-a"))

    assertTrue(limiter.tryAcquire("client-b"), "other client must not be affected")
  }

  @Test
  fun `evicts stale clients when tracked client count exceeds limit`() {
    val limiter = limiter(requestsPerMinute = 60, burstCapacity = 3, maxTrackedClients = 2)
    limiter.tryAcquire("client-a")
    limiter.tryAcquire("client-b")
    assertEquals(2, limiter.trackedClientCount)

    // バケットが満タンまで回復する時間（3 トークン @ 1 トークン/秒 = 3 秒）以上経過させる
    nowNanos += 10 * NANOS_PER_SECOND
    limiter.tryAcquire("client-c")
    assertTrue(limiter.tryAcquire("client-d"))
    assertTrue(
        limiter.trackedClientCount <= 2,
        "stale clients should be evicted, but count was ${limiter.trackedClientCount}",
    )
  }

  @Test
  fun `rejects non-positive configuration`() {
    assertThrows<IllegalArgumentException> { limiter(requestsPerMinute = 0) }
    assertThrows<IllegalArgumentException> { limiter(burstCapacity = 0) }
    assertThrows<IllegalArgumentException> { limiter(maxTrackedClients = 0) }
  }
}
