package com.akaitigo.labeldecode.parser

import com.akaitigo.labeldecode.model.AllergenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AllergenDetectionTest {
    private val parser = LabelParser(StubAdditiveLookup())

    @ParameterizedTest
    @ValueSource(strings = ["えび", "かに", "くるみ", "小麦", "そば", "卵", "乳", "落花生"])
    fun `detectAllergens finds all 8 mandatory allergens`(allergen: String) {
        val result = parser.detectAllergens("この製品には${allergen}が含まれます")
        assertTrue(result.any { it.name == allergen }) {
            "Expected to detect mandatory allergen: $allergen"
        }
        assertEquals(AllergenType.MANDATORY, result.first { it.name == allergen }.type)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "アーモンド", "あわび", "いか", "いくら", "オレンジ",
            "カシューナッツ", "キウイフルーツ", "牛肉", "ごま",
            "さけ", "さば", "大豆", "鶏肉", "バナナ", "豚肉",
            "まつたけ", "もも", "やまいも", "りんご", "ゼラチン",
        ],
    )
    fun `detectAllergens finds all 20 recommended allergens`(allergen: String) {
        val result = parser.detectAllergens("この製品には${allergen}が含まれます")
        assertTrue(result.any { it.name == allergen }) {
            "Expected to detect recommended allergen: $allergen"
        }
        assertEquals(AllergenType.RECOMMENDED, result.first { it.name == allergen }.type)
    }

    @Test
    fun `detectAllergens returns empty for text without allergens`() {
        val result = parser.detectAllergens("水、食塩、砂糖")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectAllergens handles mixed mandatory and recommended`() {
        val result = parser.detectAllergens("小麦粉、大豆油、卵黄、りんご果汁")
        val mandatoryNames = result.filter { it.type == AllergenType.MANDATORY }.map { it.name }
        val recommendedNames = result.filter { it.type == AllergenType.RECOMMENDED }.map { it.name }

        assertTrue(mandatoryNames.contains("小麦"))
        assertTrue(mandatoryNames.contains("卵"))
        assertTrue(recommendedNames.contains("大豆"))
        assertTrue(recommendedNames.contains("りんご"))
    }
}
