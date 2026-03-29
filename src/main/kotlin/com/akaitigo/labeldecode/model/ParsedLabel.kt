package com.akaitigo.labeldecode.model

data class ParsedLabel(
    val ingredients: List<Ingredient>,
    val additives: List<Additive>,
    val allergens: List<Allergen>,
    val originalText: String,
)
