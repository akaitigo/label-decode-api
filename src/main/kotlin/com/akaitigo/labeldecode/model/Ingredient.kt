package com.akaitigo.labeldecode.model

data class Ingredient(
    val name: String,
    val allergenSources: List<String> = emptyList(),
)
