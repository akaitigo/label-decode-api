package com.akaitigo.labeldecode.parser

import com.akaitigo.labeldecode.repository.AdditiveLookup

class StubAdditiveLookup : AdditiveLookup {
  override fun findCategoryByName(name: String): String? = null

  override fun findCategoryByPartialName(name: String): String? = null

  override fun findCategoriesByNames(names: List<String>): Map<String, String> = emptyMap()
}
