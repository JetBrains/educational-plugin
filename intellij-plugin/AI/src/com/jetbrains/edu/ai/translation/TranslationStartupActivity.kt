package com.jetbrains.edu.ai.translation

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.application
import com.jetbrains.edu.ai.translation.settings.translationSettings
import com.jetbrains.edu.ai.translation.updater.TranslationUpdateChecker.Companion.launchTranslationUpdateChecker
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse

class TranslationStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isStudentProject()) return

    val course = project.course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return

    val settings = application.translationSettings()
    val language = settings.preferableLanguage
    if (!TranslationProjectSettings.isCourseTranslated(project) && language != null) {
      TranslationLoader.getInstance(project).fetchAndApplyTranslation(course, language)
    }
    project.launchTranslationUpdateChecker(course)
  }
}