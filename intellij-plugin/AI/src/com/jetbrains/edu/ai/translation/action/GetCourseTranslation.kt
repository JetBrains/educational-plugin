package com.jetbrains.edu.ai.translation.action

import com.google.gson.GsonBuilder
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.ai.translation.dialog.GetCourseTranslationDialog
import com.jetbrains.edu.ai.translation.marketplaceId
import com.jetbrains.edu.ai.translation.updateVersion
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.core.format.domain.MarketplaceId
import com.jetbrains.educational.core.format.domain.UpdateVersion
import com.jetbrains.educational.translation.enum.Language
import org.jetbrains.annotations.NonNls

class GetCourseTranslation : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val course = project.service<StudyTaskManager>().course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return

    val selectedLanguage = GetCourseTranslationDialog(course).getLanguage() ?: return
    printTranslatedCourse(project, course.marketplaceId, course.updateVersion, selectedLanguage)
  }

  override fun update(e: AnActionEvent) {
    val course = e.project?.course as? EduCourse
    e.presentation.isEnabledAndVisible = course?.isMarketplaceRemote == true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  private fun printTranslatedCourse(project: Project, marketplaceId: MarketplaceId, updateVersion: UpdateVersion, language: Language) {
    val translatedCourse = runWithModalProgressBlocking(project, EduAIBundle.message("ai.service.getting.course.translation")) {
      TranslationServiceConnector.getInstance().getTranslatedCourse(marketplaceId, updateVersion, language)
    }.onError {
      LOG.error(it)
      return
    }

    GsonBuilder()
      .setPrettyPrinting()
      .create()
      .toJson(translatedCourse)
      .let { LOG.info(it) }
  }

  companion object {
    private val LOG: Logger = thisLogger()

    @Suppress("unused")
    @NonNls
    const val ACTION_ID = "Educational.GetCourseTranslation"
  }
}