package com.jetbrains.edu.kotlin

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.edu.kotlin.KtConfigurator.Companion.SUBTASK_TESTS_KT
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.SubtaskUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator
import java.util.*

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
  override val taskTemplateName: String = KtConfigurator.TASK_KT
  override val testTemplateName: String = KtConfigurator.TESTS_KT

  override fun createTestsForNewSubtask(project: Project, task: TaskWithSubtasks) {
    val taskDir = task.getTaskDir(project) ?: return
    val prevSubtaskIndex = task.lastSubtaskIndex
    val taskPsiDir = PsiManager.getInstance(project).findDirectory(taskDir) ?: return
    val nextSubtaskIndex = prevSubtaskIndex + 1
    val nextSubtaskFileName = SubtaskUtils.getTestFileName(project, nextSubtaskIndex)

    runWriteAction {
      try {
        val testsTemplate = FileTemplateManager.getInstance(project).getInternalTemplate(SUBTASK_TESTS_KT)
        if (testsTemplate == null) return@runWriteAction
        val properties = Properties()
        properties.setProperty("TEST_CLASS_NAME", "Test" + EduNames.SUBTASK_MARKER + nextSubtaskIndex)
        FileTemplateUtil.createFromTemplate(testsTemplate, nextSubtaskFileName, properties, taskPsiDir)
      } catch (e: Exception) {
        LOG.error(e)
      }
    }
  }

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)

  companion object {
    private val LOG = Logger.getInstance(KtCourseBuilder::class.java)

    private const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"
  }
}
