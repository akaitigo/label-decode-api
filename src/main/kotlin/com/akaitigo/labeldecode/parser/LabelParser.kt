package com.akaitigo.labeldecode.parser

import com.akaitigo.labeldecode.model.Additive
import com.akaitigo.labeldecode.model.Allergen
import com.akaitigo.labeldecode.model.AllergenType
import com.akaitigo.labeldecode.model.Ingredient
import com.akaitigo.labeldecode.model.ParsedLabel
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class LabelParser {
    companion object {
        private val SLASH_PATTERN = Regex("[/／]")
        private val PAREN_PATTERN = Regex("[（(]([^）)]+)[）)]")
        private val ITEM_SEPARATOR = Regex("[、,，]")

        private val MANDATORY_ALLERGENS =
            setOf(
                "えび",
                "かに",
                "くるみ",
                "小麦",
                "そば",
                "卵",
                "乳",
                "落花生",
            )

        private val RECOMMENDED_ALLERGENS =
            setOf(
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
            )

        private val ADDITIVE_CATEGORY_PATTERN =
            Regex(
                "[（(](保存料|着色料|甘味料|酸化防止剤|乳化剤|増粘剤|膨張剤|調味料|酸味料|" +
                    "pH調整剤|香料|発色剤|漂白剤|防かび剤|栄養強化剤|安定剤|ゲル化剤)[）)]",
            )
    }

    fun parse(rawText: String): ParsedLabel {
        val parts = SLASH_PATTERN.split(rawText, limit = 2)
        val ingredientsPart = parts[0].trim()
        val additivesPart = if (parts.size > 1) parts[1].trim() else ""

        val ingredients = parseIngredients(ingredientsPart)
        val additives = parseAdditives(additivesPart)
        val allergens = detectAllergens(rawText)

        return ParsedLabel(
            ingredients = ingredients,
            additives = additives,
            allergens = allergens,
            originalText = rawText,
        )
    }

    fun parseIngredients(text: String): List<Ingredient> {
        if (text.isBlank()) return emptyList()
        return splitItems(text).map { item ->
            val allergenSources =
                extractParenContent(item)
                    .flatMap { content -> findAllergens(content) }
                    .map { it.name }
            val name = item.replace(PAREN_PATTERN, "").trim()
            Ingredient(name = name, allergenSources = allergenSources)
        }
    }

    fun parseAdditives(text: String): List<Additive> {
        if (text.isBlank()) return emptyList()
        return splitItems(text).map { item ->
            val categoryMatch = ADDITIVE_CATEGORY_PATTERN.find(item)
            val category = categoryMatch?.groupValues?.get(1) ?: "その他"
            val name = item.replace(ADDITIVE_CATEGORY_PATTERN, "").trim()
            Additive(name = name, category = category)
        }
    }

    fun detectAllergens(text: String): List<Allergen> {
        val found = mutableListOf<Allergen>()
        for (allergen in MANDATORY_ALLERGENS) {
            if (text.contains(allergen)) {
                found.add(
                    Allergen(
                        name = allergen,
                        type = AllergenType.MANDATORY,
                        sourceText = allergen,
                    ),
                )
            }
        }
        for (allergen in RECOMMENDED_ALLERGENS) {
            if (text.contains(allergen)) {
                found.add(
                    Allergen(
                        name = allergen,
                        type = AllergenType.RECOMMENDED,
                        sourceText = allergen,
                    ),
                )
            }
        }
        return found
    }

    private fun splitItems(text: String): List<String> =
        ITEM_SEPARATOR
            .split(text)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun extractParenContent(text: String): List<String> = PAREN_PATTERN.findAll(text).map { it.groupValues[1] }.toList()

    private fun findAllergens(text: String): List<Allergen> {
        val found = mutableListOf<Allergen>()
        for (allergen in MANDATORY_ALLERGENS) {
            if (text.contains(allergen)) {
                found.add(Allergen(allergen, AllergenType.MANDATORY, text))
            }
        }
        for (allergen in RECOMMENDED_ALLERGENS) {
            if (text.contains(allergen)) {
                found.add(Allergen(allergen, AllergenType.RECOMMENDED, text))
            }
        }
        return found
    }
}
