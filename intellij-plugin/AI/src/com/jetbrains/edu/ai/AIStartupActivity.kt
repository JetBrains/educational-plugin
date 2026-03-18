package com.jetbrains.edu.ai

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.ai.AIServiceUpdateChecker.Companion.launchUpdateChecker
import com.jetbrains.edu.ai.terms.CourseTermsObserverService
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.isFromCourseStorage

class AIStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isStudentProject()) return

    val course = project.course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return
    if (course.isFromCourseStorage()) return
    project.launchUpdateChecker(course)

    CourseTermsObserverService.getInstance(project).observeAndLoadCourseTerms()
  }
}