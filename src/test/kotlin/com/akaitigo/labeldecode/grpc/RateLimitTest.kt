package com.akaitigo.labeldecode.grpc

import akaitigo.labeldecode.v1.LabelDecodeServiceGrpc
import akaitigo.labeldecode.v1.ParseLabelRequest
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcClient
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val BURST_CAPACITY = 3

@QuarkusTest
@TestProfile(RateLimitTest.RateLimitEnabledProfile::class)
class RateLimitTest {
  class RateLimitEnabledProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> =
        mapOf(
            "labeldecode.security.rate-limit.enabled" to "true",
            // 補充を毎分 1 リクエストに抑え、テスト実行中の補充による揺らぎを防ぐ
            "labeldecode.security.rate-limit.requests-per-minute" to "1",
            "labeldecode.security.rate-limit.burst-capacity" to "$BURST_CAPACITY",
        )
  }

  @GrpcClient("labeldecode")
  lateinit var client: LabelDecodeServiceGrpc.LabelDecodeServiceBlockingStub

  @Test
  fun `requests within burst capacity succeed and excess is rejected with RESOURCE_EXHAUSTED`() {
    val request = ParseLabelRequest.newBuilder().setRawText("小麦粉、砂糖").build()

    repeat(BURST_CAPACITY) {
      val response = client.parseLabel(request)
      assertEquals(2, response.label.ingredientsList.size, "request ${it + 1} should succeed")
    }

    val exception = assertThrows<StatusRuntimeException> { client.parseLabel(request) }

    assertEquals("RESOURCE_EXHAUSTED", exception.status.code.name)
  }
}
