package com.jetbrains.edu.learning.intellij

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.SubtaskUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.findTestDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator
import org.jetbrains.plugins.gradle.util.GradleConstants

import java.io.IOException
import java.util.*

abstract class GradleCourseBuilderBase : EduCourseBuilder<JdkProjectSettings> {

  abstract val buildGradleTemplateName: String
  abstract val taskTemplateName: String
  abstract val testTemplateName: String
  abstract val subtaskTestTemplateName: String

  override fun createTaskContent(project: Project, task: Task,
                                 parentDirectory: VirtualFile, course: Course): VirtualFile? {
    initNewTask(task)
    runWriteAction {
      try {
        GeneratorUtils.createTask(task, parentDirectory)
      } catch (e: IOException) {
        LOG.error("Failed to create task", e)
      }
    }

    ExternalSystemUtil.refreshProjects(project, GradleConstants.SYSTEM_ID, true, ProgressExecutionMode.MODAL_SYNC)
    return parentDirectory.findChild(EduNames.TASK + task.index)
  }

  override fun createTestsForNewSubtask(project: Project, task: TaskWithSubtasks) {
    val taskDir = task.getTaskDir(project) ?: return
    val testDir = task.findTestDir(taskDir) ?: return
    val prevSubtaskIndex = task.lastSubtaskIndex
    val taskPsiDir = PsiManager.getInstance(project).findDirectory(testDir) ?: return
    val nextSubtaskIndex = prevSubtaskIndex + 1
    val nextSubtaskFileName = SubtaskUtils.getTestFileName(project, nextSubtaskIndex)

    runWriteAction {
      try {
        val testsTemplate = FileTemplateManager.getInstance(project).getInternalTemplate(subtaskTestTemplateName)
        if (testsTemplate == null) return@runWriteAction
        val properties = Properties()
        properties.setProperty("TEST_CLASS_NAME", "Test" + EduNames.SUBTASK_MARKER + nextSubtaskIndex)
        FileTemplateUtil.createFromTemplate(testsTemplate, nextSubtaskFileName, properties, taskPsiDir)
      } catch (e: Exception) {
        LOG.error(e)
      }
    }
  }

  override fun getLanguageSettings(): EduCourseBuilder.LanguageSettings<JdkProjectSettings> = JdkLanguageSettings()

  open fun initNewTask(task: Task) {
    val taskFile = TaskFile()
    taskFile.task = task
    taskFile.name = taskTemplateName
    taskFile.text = EduUtils.getTextFromInternalTemplate(taskTemplateName)
    task.addTaskFile(taskFile)
    task.testsText.put(testTemplateName, EduUtils.getTextFromInternalTemplate(testTemplateName))
  }

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)

  companion object {
    private val LOG = Logger.getInstance(GradleCourseBuilderBase::class.java)
  }
}
