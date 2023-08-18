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

fun wrapWithUtm(link: String, content: String): String {
  val utmParams = "utm_source=ide&utm_medium=ide&utm_campaign=ide&utm_content=$content"

  // if there are other params, add utms as new ones
  return if (link.contains("?")) {
    "$link&$utmParams"
  }
  else {
    "$link?$utmParams"
  }
}
