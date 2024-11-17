package com.jetbrains.edu.learning.ai

import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.domain.TranslationVersion

/**
 * Represents the properties of a translation for a course.
 *
 * @property language The language of the translation.
 * @property structureTranslation The translation of the course structure, which includes the translations of
 * [com.jetbrains.edu.learning.courseFormat.StudyItem.presentableName] for
 * [com.jetbrains.edu.learning.courseFormat.tasks.Task],
 * [com.jetbrains.edu.learning.courseFormat.Lesson], and
 * [com.jetbrains.edu.learning.courseFormat.Section].
 * @property version The version of the translation.
 */
data class TranslationProperties(
  val language: TranslationLanguage,
  val structureTranslation: Map<String, String>,
  val version: TranslationVersion,
)