package com.jetbrains.edu.ai.translation

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.core.format.domain.MarketplaceId
import com.jetbrains.educational.core.format.domain.TaskEduId
import com.jetbrains.educational.core.format.domain.UpdateVersion
import com.jetbrains.educational.translation.enum.Language

val defaultLanguage: Language
  get() = Language.ENGLISH

val EduCourse.marketplaceId: MarketplaceId
  get() = MarketplaceId(id)

val EduCourse.updateVersion: UpdateVersion
  get() = UpdateVersion(course.marketplaceCourseVersion)

val Task.taskEduId: TaskEduId
  get() = TaskEduId(id)

fun TranslationProjectSettings.Companion.getCurrentTranslationLanguage(project: Project): Language? {
  val languageCode = getCurrentTranslationLanguageCode(project) ?: return null
  return Language.findByCode(languageCode)
}

fun TranslationProjectSettings.Companion.changeCurrentTranslationLanguage(project: Project, language: Language) {
  getInstance(project).currentTranslationLanguageCode = language.code
}