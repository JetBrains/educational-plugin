package com.jetbrains.edu.ai

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.ai.AIServiceUpdateChecker.Companion.launchUpdateChecker
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.terms.observeAndLoadCourseTerms
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.notification.EduNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIStartupActivity(private val scope: CoroutineScope) : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isStudentProject()) return

    val course = project.course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return
    project.launchUpdateChecker(course)

    scope.observeAndLoadCourseTerms(project)

    withContext(Dispatchers.EDT) {
      showAINewsNotification(project)
    }
  }

  private fun showAINewsNotification(project: Project) {
    if (PropertiesComponent.getInstance().getBoolean(EDU_AI_NEWS_NOTIFICATION_SHOWN)) return
    EduNotificationManager.showInfoNotification(
      project,
      @Suppress("DialogTitleCapitalization") // incorrect capitalization of the title is intended
      EduAIBundle.message("ai.notification.news.title"),
      EduAIBundle.message("ai.notification.news.content")
    )
    PropertiesComponent.getInstance().setValue(EDU_AI_NEWS_NOTIFICATION_SHOWN, true)
  }

  companion object {
    private const val EDU_AI_NEWS_NOTIFICATION_SHOWN = "edu.ai.news.notification.shown"
  }
}