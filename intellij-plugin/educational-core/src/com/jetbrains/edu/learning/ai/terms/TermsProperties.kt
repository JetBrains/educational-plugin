package com.jetbrains.edu.learning.ai.terms

import com.jetbrains.educational.terms.format.Term
import com.jetbrains.educational.terms.format.domain.TermsVersion

/**
 * Represents properties related to terms in a project, including the language, terms, and version information.
 *
 * @property languageCode The code of the language this set of terms applies to.
 * @property terms A map where each key is a task ID, and the value is a list of terms associated with that task.
 * @property version The version metadata for the terms.
 */
data class TermsProperties(
  val languageCode: String,
  val terms: Map<Int, List<Term>>,
  val version: TermsVersion
)