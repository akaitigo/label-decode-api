package com.akaitigo.labeldecode.grpc

import akaitigo.labeldecode.v1.LabelDecodeServiceGrpc
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass.ClassifyAdditivesRequest
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass.DetectAllergensRequest
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass.ParseLabelRequest
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcClient
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@QuarkusTest
class LabelDecodeGrpcServiceTest {
    @GrpcClient("labeldecode")
    lateinit var client: LabelDecodeServiceGrpc.LabelDecodeServiceBlockingStub

    @Test
    fun `ParseLabel returns structured data for valid input`() {
        val request =
            ParseLabelRequest
                .newBuilder()
                .setRawText("小麦粉、砂糖、バター/ソルビン酸K（保存料）、カラメル色素")
                .build()

        val response = client.parseLabel(request)

        assertNotNull(response.label)
        assertEquals(3, response.label.ingredientsList.size)
        assertEquals("小麦粉", response.label.ingredientsList[0].name)
        assertEquals(2, response.label.additivesList.size)
        assertEquals("ソルビン酸K", response.label.additivesList[0].name)
        assertEquals("保存料", response.label.additivesList[0].category)
        assertTrue(response.label.allergensList.any { it.name == "小麦" })
    }

    @Test
    fun `ParseLabel returns INVALID_ARGUMENT for empty input`() {
        val request =
            ParseLabelRequest
                .newBuilder()
                .setRawText("")
                .build()

        val exception =
            assertThrows<StatusRuntimeException> {
                client.parseLabel(request)
            }

        assertEquals("INVALID_ARGUMENT", exception.status.code.name)
    }

    @Test
    fun `DetectAllergens returns allergens for valid input`() {
        val request =
            DetectAllergensRequest
                .newBuilder()
                .setRawText("小麦粉（小麦を含む）、卵、乳製品、大豆油")
                .build()

        val response = client.detectAllergens(request)

        val names = response.allergensList.map { it.name }
        assertTrue(names.contains("小麦"))
        assertTrue(names.contains("卵"))
        assertTrue(names.contains("乳"))
        assertTrue(names.contains("大豆"))
    }

    @Test
    fun `DetectAllergens returns INVALID_ARGUMENT for empty input`() {
        val request =
            DetectAllergensRequest
                .newBuilder()
                .setRawText("")
                .build()

        val exception =
            assertThrows<StatusRuntimeException> {
                client.detectAllergens(request)
            }

        assertEquals("INVALID_ARGUMENT", exception.status.code.name)
    }

    @Test
    fun `ClassifyAdditives returns classified additives`() {
        val request =
            ClassifyAdditivesRequest
                .newBuilder()
                .setRawText("砂糖/ソルビン酸K（保存料）、アスパルテーム（甘味料）")
                .build()

        val response = client.classifyAdditives(request)

        assertEquals(2, response.additivesList.size)
        assertEquals("保存料", response.additivesList[0].category)
        assertEquals("甘味料", response.additivesList[1].category)
    }

    @Test
    fun `ClassifyAdditives returns INVALID_ARGUMENT for empty input`() {
        val request =
            ClassifyAdditivesRequest
                .newBuilder()
                .setRawText("")
                .build()

        val exception =
            assertThrows<StatusRuntimeException> {
                client.classifyAdditives(request)
            }

        assertEquals("INVALID_ARGUMENT", exception.status.code.name)
    }
}
