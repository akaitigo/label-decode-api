package com.akaitigo.labeldecode.repository

interface AdditiveLookup {
  fun findCategoryByName(name: String): String?

  fun findCategoryByPartialName(name: String): String?
}
