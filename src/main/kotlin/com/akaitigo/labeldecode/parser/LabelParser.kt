package com.akaitigo.labeldecode.parser

import com.akaitigo.labeldecode.model.Additive
import com.akaitigo.labeldecode.model.Allergen
import com.akaitigo.labeldecode.model.AllergenType
import com.akaitigo.labeldecode.model.Ingredient
import com.akaitigo.labeldecode.model.ParsedLabel
import com.akaitigo.labeldecode.repository.AdditiveLookup
import jakarta.enterprise.context.ApplicationScoped

private val SLASH_PATTERN = Regex("[/／]")

private val ADDITIVE_CATEGORY_PATTERN =
    Regex(
        "[（(](保存料|着色料|甘味料|酸化防止剤|乳化剤|増粘剤|膨張剤|調味料|酸味料|" + "pH調整剤|香料|発色剤|漂白剤|防かび剤|栄養強化剤|安定剤|ゲル化剤)[）)]",
    )

private val MANDATORY_ALLERGENS = setOf("えび", "かに", "くるみ", "小麦", "そば", "卵", "乳", "落花生")

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

private val ALLERGEN_FALSE_POSITIVES =
    mapOf(
        "もも" to setOf("もも色素"),
        "乳" to setOf("乳化剤", "乳化", "乳酸菌", "乳酸Na", "乳酸Ca", "乳酸"),
        "卵" to setOf("卵殻Ca"),
        "大豆" to setOf("大豆油", "大豆レシチン"),
        "ごま" to setOf("ごま油"),
        "小麦" to setOf("小麦でん粉", "小麦胚芽油"),
    )

@ApplicationScoped
class LabelParser(
    private val additiveLookup: AdditiveLookup,
) {
  fun parse(rawText: String): ParsedLabel {
    val parts = SLASH_PATTERN.split(rawText, limit = 2)
    val ingredientsPart = parts[0].trim()
    val additivesPart =
        if (parts.size > 1) {
          parts[1].trim()
        } else {
          ""
        }

    val ingredients = parseIngredients(ingredientsPart)
    val additives = parseAdditives(additivesPart)
    val allergens = findAllAllergens(rawText)

    return ParsedLabel(
        ingredients = ingredients,
        additives = additives,
        allergens = allergens,
        originalText = rawText,
    )
  }

  fun parseIngredients(text: String): List<Ingredient> {
    if (text.isBlank()) {
      return emptyList()
    }
    return splitItems(text).map { item ->
      val allergenSources =
          extractParenContent(item).flatMap { content -> findAllAllergens(content) }.map { it.name }
      val name = removeTopLevelParens(item).trim()
      Ingredient(name = name, allergenSources = allergenSources)
    }
  }

  fun parseAdditives(text: String): List<Additive> {
    if (text.isBlank()) {
      return emptyList()
    }
    return splitItems(text).map { item ->
      val name = item.replace(ADDITIVE_CATEGORY_PATTERN, "").trim()
      val category = resolveAdditiveCategory(item, name)
      Additive(name = name, category = category)
    }
  }

  fun detectAllergens(text: String): List<Allergen> = findAllAllergens(text)

  fun parseAdditivesFromFullText(rawText: String): List<Additive> {
    val parts = SLASH_PATTERN.split(rawText, limit = 2)
    val additivesPart =
        if (parts.size > 1) {
          parts[1].trim()
        } else {
          ""
        }
    return parseAdditives(additivesPart)
  }

  private fun resolveAdditiveCategory(
      rawItem: String,
      name: String,
  ): String =
      ADDITIVE_CATEGORY_PATTERN.find(rawItem)?.groupValues?.get(1)
          ?: additiveLookup.findCategoryByName(name)
          ?: additiveLookup.findCategoryByPartialName(name)
          ?: "その他"

  private fun findAllAllergens(text: String): List<Allergen> {
    val found = mutableListOf<Allergen>()
    for (allergen in MANDATORY_ALLERGENS) {
      if (matchesAllergen(text, allergen)) {
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
      if (matchesAllergen(text, allergen)) {
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

  private fun matchesAllergen(
      text: String,
      allergen: String,
  ): Boolean {
    if (!text.contains(allergen)) {
      return false
    }
    val cleaned = removeFalsePositives(text, allergen)
    return cleaned.contains(allergen)
  }

  private fun removeFalsePositives(
      text: String,
      allergen: String,
  ): String {
    val falsePositives = ALLERGEN_FALSE_POSITIVES[allergen] ?: return text
    val sorted = falsePositives.sortedByDescending { it.length }
    return sorted.fold(text) { acc, fp -> acc.replace(fp, "") }
  }
}

private val OPEN_PARENS = setOf('（', '(')
private val CLOSE_PARENS = setOf('）', ')')
private val SEPARATORS = setOf('、', ',', '，')

private fun splitItems(text: String): List<String> {
  val items = mutableListOf<String>()
  val current = StringBuilder()
  var depth = 0
  for (ch in text) {
    depth = updateDepthAndAppend(ch, depth, current, items)
  }
  addRemainder(current, items)
  return items
}

private fun updateDepthAndAppend(
    ch: Char,
    depth: Int,
    current: StringBuilder,
    items: MutableList<String>,
): Int =
    when {
      ch in OPEN_PARENS -> {
        current.append(ch)
        depth + 1
      }

      ch in CLOSE_PARENS -> {
        current.append(ch)
        if (depth > 0) {
          depth - 1
        } else {
          depth
        }
      }

      ch in SEPARATORS && depth == 0 -> {
        flushItem(current, items)
        depth
      }

      else -> {
        current.append(ch)
        depth
      }
    }

private fun flushItem(
    current: StringBuilder,
    items: MutableList<String>,
) {
  val item = current.toString().trim()
  if (item.isNotEmpty()) {
    items.add(item)
  }
  current.clear()
}

private fun addRemainder(
    current: StringBuilder,
    items: MutableList<String>,
) {
  val last = current.toString().trim()
  if (last.isNotEmpty()) {
    items.add(last)
  }
}

/** ネスト対応でトップレベルの括弧とその中身を除去する。 */
private fun removeTopLevelParens(text: String): String {
  val result = StringBuilder()
  var depth = 0
  for (ch in text) {
    depth = processParenChar(ch, depth)
    if (depth == 0 && ch !in CLOSE_PARENS) {
      result.append(ch)
    }
  }
  return result.toString()
}

/** 括弧文字に対する depth の変化を計算する。 */
private fun processParenChar(
    ch: Char,
    depth: Int,
): Int =
    when {
      ch in OPEN_PARENS -> depth + 1
      ch in CLOSE_PARENS && depth > 0 -> depth - 1
      else -> depth
    }

/** ネスト対応の括弧内テキスト抽出。トップレベルの括弧の中身を返す。 */
private fun extractParenContent(text: String): List<String> {
  val results = mutableListOf<String>()
  val current = StringBuilder()
  var depth = 0
  for (ch in text) {
    depth = handleExtractChar(ch, depth, current, results)
  }
  return results
}

private fun handleExtractChar(
    ch: Char,
    depth: Int,
    current: StringBuilder,
    results: MutableList<String>,
): Int =
    when {
      ch in OPEN_PARENS -> {
        if (depth > 0) {
          current.append(ch)
        }
        depth + 1
      }

      ch in CLOSE_PARENS -> {
        handleExtractClose(depth, current, results)
      }

      else -> {
        if (depth > 0) {
          current.append(ch)
        }
        depth
      }
    }

private fun handleExtractClose(
    depth: Int,
    current: StringBuilder,
    results: MutableList<String>,
): Int {
  val newDepth =
      if (depth > 0) {
        depth - 1
      } else {
        depth
      }
  if (newDepth == 0) {
    val content = current.toString().trim()
    if (content.isNotEmpty()) {
      results.add(content)
    }
    current.clear()
  } else if (newDepth > 0) {
    current.append('）')
  }
  return newDepth
}
