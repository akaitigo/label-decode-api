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
      strings =
          [
              "アーモンド",
              "あわび",
              "いか",
              "いくら",
              "オレンジ",
              "カシューナッツ",
              "キウイフルーツ",
              "牛肉",
              "ごま",
              "さけ",
              "さば",
              "大豆",
              "鶏肉",
              "バナナ",
              "豚肉",
              "まつたけ",
              "もも",
              "やまいも",
              "りんご",
              "ゼラチン",
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
    val result = parser.detectAllergens("小麦粉、大豆（遺伝子組換えでない）、卵黄、りんご果汁")
    val mandatoryNames = result.filter { it.type == AllergenType.MANDATORY }.map { it.name }
    val recommendedNames = result.filter { it.type == AllergenType.RECOMMENDED }.map { it.name }

    assertTrue(mandatoryNames.contains("小麦"))
    assertTrue(mandatoryNames.contains("卵"))
    assertTrue(recommendedNames.contains("大豆"))
    assertTrue(recommendedNames.contains("りんご"))
  }

  @Test
  fun `false positive - 大豆油 does not trigger 大豆 allergen`() {
    val result = parser.detectAllergens("大豆油、砂糖")
    assertTrue(result.none { it.name == "大豆" })
  }

  @Test
  fun `false positive - 大豆レシチン does not trigger 大豆 allergen`() {
    val result = parser.detectAllergens("大豆レシチン、食塩")
    assertTrue(result.none { it.name == "大豆" })
  }

  @Test
  fun `false positive - ごま油 does not trigger ごま allergen`() {
    val result = parser.detectAllergens("ごま油、醤油")
    assertTrue(result.none { it.name == "ごま" })
  }

  @Test
  fun `false positive - 小麦でん粉 does not trigger 小麦 allergen`() {
    val result = parser.detectAllergens("小麦でん粉、水")
    assertTrue(result.none { it.name == "小麦" })
  }

  @Test
  fun `false positive - もも色素 does not trigger もも allergen`() {
    val result = parser.detectAllergens("もも色素、砂糖")
    assertTrue(result.none { it.name == "もも" })
  }

  @Test
  fun `false positive - 乳化剤 does not trigger 乳 allergen`() {
    val result = parser.detectAllergens("乳化剤、香料")
    assertTrue(result.none { it.name == "乳" })
  }

  @Test
  fun `real allergen detected even when false positive text also present`() {
    val result = parser.detectAllergens("大豆油、大豆（遺伝子組換えでない）")
    assertTrue(result.any { it.name == "大豆" })
  }

  @Test
  fun `multiple false positives together do not trigger allergen`() {
    val result = parser.detectAllergens("植物油脂(大豆油)、乳化剤(大豆レシチン)")
    assertTrue(result.none { it.name == "大豆" }) { "大豆油 + 大豆レシチン should not trigger 大豆 allergen" }
  }

  @Test
  fun `multiple false positives with real allergen still detects`() {
    val result = parser.detectAllergens("植物油脂(大豆油)、乳化剤(大豆レシチン)、大豆")
    assertTrue(result.any { it.name == "大豆" }) {
      "Real 大豆 should be detected even with false positive texts"
    }
  }

  @Test
  fun `false positive - 乳化剤 and 乳化 together do not trigger 乳 allergen`() {
    val result = parser.detectAllergens("乳化剤(大豆レシチン)、グリセリン脂肪酸エステル(乳化)")
    assertTrue(result.none { it.name == "乳" }) { "乳化剤 + 乳化 should not trigger 乳 allergen" }
  }

  @Test
  fun `false positive - 乳酸 does not trigger 乳 allergen`() {
    val result = parser.detectAllergens("乳酸、クエン酸")
    assertTrue(result.none { it.name == "乳" }) { "乳酸 should not trigger 乳 allergen" }
  }

  @Test
  fun `false positive - 乳酸菌 does not trigger 乳 allergen`() {
    val result = parser.detectAllergens("乳酸菌、ビタミンC")
    assertTrue(result.none { it.name == "乳" }) { "乳酸菌 should not trigger 乳 allergen" }
  }

  @Test
  fun `false positive - 乳酸Na does not trigger 乳 allergen`() {
    val result = parser.detectAllergens("乳酸Na、pH調整剤")
    assertTrue(result.none { it.name == "乳" }) { "乳酸Na should not trigger 乳 allergen" }
  }

  @Test
  fun `false positive - 乳酸Ca does not trigger 乳 allergen`() {
    val result = parser.detectAllergens("乳酸Ca、食塩")
    assertTrue(result.none { it.name == "乳" }) { "乳酸Ca should not trigger 乳 allergen" }
  }

  @Test
  fun `false positive - multiple 乳 false positives together do not trigger`() {
    val result = parser.detectAllergens("乳化剤、乳酸菌、乳酸Na")
    assertTrue(result.none { it.name == "乳" }) { "乳化剤 + 乳酸菌 + 乳酸Na should not trigger 乳 allergen" }
  }

  @Test
  fun `real 乳 detected even with 乳酸 false positive present`() {
    val result = parser.detectAllergens("乳酸菌、脱脂粉乳")
    assertTrue(result.any { it.name == "乳" }) {
      "Real 乳 (in 脱脂粉乳) should be detected even with 乳酸菌 present"
    }
  }
}
