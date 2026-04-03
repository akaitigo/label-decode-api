package com.akaitigo.labeldecode.repository

interface AdditiveLookup {
  fun findCategoryByName(name: String): String?

  fun findCategoryByPartialName(name: String): String?

  /** Batch lookup: returns a map of name -> category for all matched names. */
  fun findCategoriesByNames(names: List<String>): Map<String, String>
}
