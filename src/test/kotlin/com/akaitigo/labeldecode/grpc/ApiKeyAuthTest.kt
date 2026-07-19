package com.akaitigo.labeldecode.grpc

import akaitigo.labeldecode.v1.ClassifyAdditivesRequest
import akaitigo.labeldecode.v1.DetectAllergensRequest
import akaitigo.labeldecode.v1.LabelDecodeServiceGrpc
import akaitigo.labeldecode.v1.ParseLabelRequest
import com.akaitigo.labeldecode.security.ApiKeyAuthInterceptor
import io.grpc.Metadata
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import io.quarkus.grpc.GrpcClient
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@QuarkusTest
@TestProfile(ApiKeyAuthTest.AuthEnabledProfile::class)
class ApiKeyAuthTest {
  class AuthEnabledProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> =
        mapOf(
            "labeldecode.security.auth.enabled" to "true",
            "labeldecode.security.auth.api-keys" to "test-key-1,test-key-2",
        )
  }

  @GrpcClient("labeldecode")
  lateinit var client: LabelDecodeServiceGrpc.LabelDecodeServiceBlockingStub

  private fun clientWithKey(apiKey: String): LabelDecodeServiceGrpc.LabelDecodeServiceBlockingStub {
    val headers = Metadata()
    headers.put(ApiKeyAuthInterceptor.API_KEY_METADATA_KEY, apiKey)
    return client.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
  }

  @Test
  fun `request without api key is rejected with UNAUTHENTICATED`() {
    val request = ParseLabelRequest.newBuilder().setRawText("小麦粉、砂糖").build()

    val exception = assertThrows<StatusRuntimeException> { client.parseLabel(request) }

    assertEquals("UNAUTHENTICATED", exception.status.code.name)
  }

  @Test
  fun `request with invalid api key is rejected with UNAUTHENTICATED`() {
    val request = ParseLabelRequest.newBuilder().setRawText("小麦粉、砂糖").build()

    val exception =
        assertThrows<StatusRuntimeException> { clientWithKey("wrong-key").parseLabel(request) }

    assertEquals("UNAUTHENTICATED", exception.status.code.name)
  }

  @Test
  fun `request with valid api key succeeds`() {
    val request = ParseLabelRequest.newBuilder().setRawText("小麦粉、砂糖、バター").build()

    val response = clientWithKey("test-key-1").parseLabel(request)

    assertEquals(3, response.label.ingredientsList.size)
  }

  @Test
  fun `all configured api keys are accepted`() {
    val request = DetectAllergensRequest.newBuilder().setRawText("小麦粉、卵").build()

    val response = clientWithKey("test-key-2").detectAllergens(request)

    assertTrue(response.allergensList.any { it.name == "小麦" })
  }

  @Test
  fun `all rpc methods are guarded by authentication`() {
    val parseError =
        assertThrows<StatusRuntimeException> {
          client.parseLabel(ParseLabelRequest.newBuilder().setRawText("小麦粉").build())
        }
    val detectError =
        assertThrows<StatusRuntimeException> {
          client.detectAllergens(DetectAllergensRequest.newBuilder().setRawText("小麦粉").build())
        }
    val classifyError =
        assertThrows<StatusRuntimeException> {
          client.classifyAdditives(ClassifyAdditivesRequest.newBuilder().setRawText("小麦粉").build())
        }

    assertEquals("UNAUTHENTICATED", parseError.status.code.name)
    assertEquals("UNAUTHENTICATED", detectError.status.code.name)
    assertEquals("UNAUTHENTICATED", classifyError.status.code.name)
  }
}
