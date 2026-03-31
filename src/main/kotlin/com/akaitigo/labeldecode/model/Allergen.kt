package com.akaitigo.labeldecode.model

data class Allergen(
    val name: String,
    val type: AllergenType,
    val sourceText: String,
)

enum class AllergenType {
  MANDATORY,
  RECOMMENDED,
}
