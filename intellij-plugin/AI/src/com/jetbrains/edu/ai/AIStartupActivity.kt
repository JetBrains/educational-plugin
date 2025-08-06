package com.jetbrains.edu.ai

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.ai.AIServiceUpdateChecker.Companion.launchUpdateChecker
import com.jetbrains.edu.ai.terms.observeAndLoadCourseTerms
import com.jetbrains.edu.ai.terms.ui.TermsGotItTooltipService
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.isFromCourseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIStartupActivity(private val scope: CoroutineScope) : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isStudentProject()) return

    val course = project.course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return
    if (course.isFromCourseStorage()) return
    project.launchUpdateChecker(course)

    scope.observeAndLoadCourseTerms(project)

    withContext(Dispatchers.EDT) {
      TermsGotItTooltipService.getInstance(project).showTermsGotItTooltip()
    }
  }
}