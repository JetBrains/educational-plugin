package com.jetbrains.edu.learning

import java.util.*

private fun String.capitalize(locale: Locale): String {
  return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

fun String.capitalize(): String {
  return capitalize(Locale.getDefault())
}

fun String.decapitalize(): String {
  return replaceFirstChar { it.lowercase(Locale.getDefault()) }
}