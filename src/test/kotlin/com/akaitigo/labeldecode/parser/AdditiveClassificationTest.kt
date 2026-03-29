package com.akaitigo.labeldecode.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdditiveClassificationTest {
    private val parser = LabelParser(StubAdditiveMasterRepository())

    @Test
    fun `classifies preservatives`() {
        val result = parser.parseAdditives("ソルビン酸K（保存料）")
        assertEquals(1, result.size)
        assertEquals("ソルビン酸K", result[0].name)
        assertEquals("保存料", result[0].category)
    }

    @Test
    fun `classifies colorings`() {
        val result = parser.parseAdditives("カラメル色素（着色料）")
        assertEquals(1, result.size)
        assertEquals("着色料", result[0].category)
    }

    @Test
    fun `classifies sweeteners`() {
        val result = parser.parseAdditives("アスパルテーム（甘味料）")
        assertEquals(1, result.size)
        assertEquals("甘味料", result[0].category)
    }

    @Test
    fun `classifies antioxidants`() {
        val result = parser.parseAdditives("V.C（酸化防止剤）")
        assertEquals(1, result.size)
        assertEquals("酸化防止剤", result[0].category)
    }

    @Test
    fun `classifies emulsifiers`() {
        val result = parser.parseAdditives("レシチン（乳化剤）")
        assertEquals(1, result.size)
        assertEquals("乳化剤", result[0].category)
    }

    @Test
    fun `classifies multiple additives`() {
        val result =
            parser.parseAdditives(
                "ソルビン酸K（保存料）、アスパルテーム（甘味料）、V.C（酸化防止剤）、レシチン（乳化剤）、香料",
            )
        assertEquals(5, result.size)
        assertEquals("保存料", result[0].category)
        assertEquals("甘味料", result[1].category)
        assertEquals("酸化防止剤", result[2].category)
        assertEquals("乳化剤", result[3].category)
        assertEquals("その他", result[4].category)
    }

    @Test
    fun `handles half-width parentheses`() {
        val result = parser.parseAdditives("ソルビン酸K(保存料)")
        assertEquals(1, result.size)
        assertEquals("保存料", result[0].category)
    }

    @Test
    fun `additive without category returns その他`() {
        val result = parser.parseAdditives("カラメル色素")
        assertEquals(1, result.size)
        assertEquals("その他", result[0].category)
    }
}
