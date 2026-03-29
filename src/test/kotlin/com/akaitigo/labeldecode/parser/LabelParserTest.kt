package com.akaitigo.labeldecode.parser

import com.akaitigo.labeldecode.model.AllergenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LabelParserTest {
    private val parser = LabelParser()

    @Test
    fun `parse splits ingredients and additives by slash`() {
        val result = parser.parse("小麦粉、砂糖、バター/ソルビン酸K（保存料）、カラメル色素")

        assertEquals(3, result.ingredients.size)
        assertEquals("小麦粉", result.ingredients[0].name)
        assertEquals("砂糖", result.ingredients[1].name)
        assertEquals("バター", result.ingredients[2].name)

        assertEquals(2, result.additives.size)
        assertEquals("ソルビン酸K", result.additives[0].name)
        assertEquals("保存料", result.additives[0].category)
        assertEquals("カラメル色素", result.additives[1].name)
    }

    @Test
    fun `parse handles full-width slash`() {
        val result = parser.parse("小麦粉、砂糖／ソルビン酸K（保存料）")

        assertEquals(2, result.ingredients.size)
        assertEquals(1, result.additives.size)
    }

    @Test
    fun `parse with no additives section`() {
        val result = parser.parse("小麦粉、砂糖、バター")

        assertEquals(3, result.ingredients.size)
        assertTrue(result.additives.isEmpty())
    }

    @Test
    fun `detectAllergens finds mandatory allergens`() {
        val result = parser.detectAllergens("小麦粉（小麦を含む）、卵、乳製品")

        val names = result.map { it.name }
        assertTrue(names.contains("小麦"))
        assertTrue(names.contains("卵"))
        assertTrue(names.contains("乳"))

        val mandatoryCount = result.count { it.type == AllergenType.MANDATORY }
        assertEquals(3, mandatoryCount)
    }

    @Test
    fun `detectAllergens finds recommended allergens`() {
        val result = parser.detectAllergens("大豆、りんご果汁、ゼラチン")

        val names = result.map { it.name }
        assertTrue(names.contains("大豆"))
        assertTrue(names.contains("りんご"))
        assertTrue(names.contains("ゼラチン"))

        val recommendedCount = result.count { it.type == AllergenType.RECOMMENDED }
        assertEquals(3, recommendedCount)
    }

    @Test
    fun `parseIngredients extracts allergen sources from parentheses`() {
        val result = parser.parseIngredients("しょうゆ（小麦・大豆を含む）、マヨネーズ（卵を含む）")

        assertEquals(2, result.size)
        assertEquals("しょうゆ", result[0].name)
        assertTrue(result[0].allergenSources.contains("小麦"))
        assertTrue(result[0].allergenSources.contains("大豆"))
        assertEquals("マヨネーズ", result[1].name)
        assertTrue(result[1].allergenSources.contains("卵"))
    }

    @Test
    fun `parseAdditives classifies by category in parentheses`() {
        val result = parser.parseAdditives("ソルビン酸K（保存料）、アスパルテーム（甘味料）、V.C（酸化防止剤）")

        assertEquals(3, result.size)
        assertEquals("保存料", result[0].category)
        assertEquals("甘味料", result[1].category)
        assertEquals("酸化防止剤", result[2].category)
    }

    @Test
    fun `parseAdditives without category defaults to その他`() {
        val result = parser.parseAdditives("カラメル色素、pH調整剤")

        assertEquals(2, result.size)
        assertEquals("その他", result[0].category)
    }

    @Test
    fun `parse complex real-world label`() {
        val text =
            "鶏肉（国産）、玉ねぎ、にんじん、じゃがいも、小麦粉（小麦を含む）、" +
                "カレー粉、食塩、砂糖/調味料(アミノ酸等)、カラメル色素、酸味料、香料"
        val result = parser.parse(text)

        assertEquals(8, result.ingredients.size)
        assertEquals(4, result.additives.size)
        assertTrue(result.allergens.any { it.name == "小麦" })
        assertTrue(result.allergens.any { it.name == "鶏肉" })
    }

    @Test
    fun `parse preserves original text`() {
        val text = "小麦粉、砂糖/ソルビン酸K（保存料）"
        val result = parser.parse(text)
        assertEquals(text, result.originalText)
    }
}
