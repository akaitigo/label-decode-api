package com.akaitigo.labeldecode.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DestructiveTest {
  private val parser = LabelParser(StubAdditiveLookup())

  @Test
  fun `empty string returns empty results`() {
    val result = parser.parse("")
    assertTrue(result.ingredients.isEmpty())
    assertTrue(result.additives.isEmpty())
    assertTrue(result.allergens.isEmpty())
  }

  @Test
  fun `slash only returns empty ingredients and additives`() {
    val result = parser.parse("/")
    assertTrue(result.ingredients.isEmpty())
    assertTrue(result.additives.isEmpty())
  }

  @Test
  fun `very long input does not crash`() {
    val longText = "小麦粉、".repeat(10000) + "砂糖/ソルビン酸K（保存料）"
    val result = parser.parse(longText)
    assertTrue(result.ingredients.size > 1)
    assertEquals(1, result.additives.size)
  }

  @Test
  fun `special characters in input do not cause errors`() {
    val result = parser.parse("原材料&lt;script&gt;、砂糖/添加物")
    assertEquals(2, result.ingredients.size)
    assertEquals(1, result.additives.size)
  }

  @Test
  fun `SQL injection in input does not cause errors`() {
    val result = parser.parse("原材料'; DROP TABLE additive_master; --、砂糖")
    assertEquals(2, result.ingredients.size)
  }

  @Test
  fun `unicode edge cases`() {
    val result = parser.parse("𠮷野家の牛丼/調味料（アミノ酸等）")
    assertEquals(1, result.ingredients.size)
    assertEquals(1, result.additives.size)
  }

  @Test
  fun `multiple consecutive slashes`() {
    val result = parser.parse("小麦粉//ソルビン酸K（保存料）")
    assertTrue(result.ingredients.isNotEmpty())
  }

  @Test
  fun `only allergens no ingredients`() {
    val allergens = parser.detectAllergens("")
    assertTrue(allergens.isEmpty())
  }
}
