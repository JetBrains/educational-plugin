package com.jetbrains.edu.ai.terms

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.translation.isSameLanguage
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TheoryLookupSettings
import com.jetbrains.edu.learning.combineStateFlow
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val TERMS_NOTIFICATION_ID = "terms"

// TODO(implement tests)
fun CoroutineScope.observeAndLoadCourseTerms(project: Project) {
  launch {
    val combinedStateFlow = combineStateFlow(
      TheoryLookupSettings.getInstance().theoryLookupProperties,
      TranslationProjectSettings.getInstance(project).translationProperties
    )
    combinedStateFlow.collectLatest { (theoryLookupProperties, translationProperties) ->
      if (theoryLookupProperties?.isEnabled == false) return@collectLatest

      val course = project.course as? EduCourse ?: return@collectLatest

      val translationLanguage = translationProperties?.language
      if (translationLanguage != null && !translationLanguage.isSameLanguage(course)) return@collectLatest  // TODO(support other languages)
      val languageCode = course.languageCode

      if (TermsProjectSettings.areCourseTermsLoaded(project, languageCode)) return@collectLatest
      TermsLoader.getInstance(project).fetchAndApplyTerms(course, languageCode)
    }
  }
}