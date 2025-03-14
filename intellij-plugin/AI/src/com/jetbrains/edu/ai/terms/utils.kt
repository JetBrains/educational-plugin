package com.jetbrains.edu.ai.terms

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.terms.updater.TermsUpdateChecker
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
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
      val course = project.course as? EduCourse ?: return@collectLatest
      TermsUpdateChecker.getInstance(project).checkUpdate(course, theoryLookupProperties, translationProperties)
    }
  }
}