package com.jetbrains.edu.kotlin.subtask

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.subtask.CCSubtaskTestBase
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.learning.EduConfigurator
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtSubtaskTest : CCSubtaskTestBase() {
  override val configurator: EduConfigurator<*> = KtConfigurator()
  override val taskFileName: String = KtConfigurator.TASK_KT
  override val testFileName: String = KtConfigurator.TESTS_KT
  override val language: Language = KotlinLanguage.INSTANCE
}
