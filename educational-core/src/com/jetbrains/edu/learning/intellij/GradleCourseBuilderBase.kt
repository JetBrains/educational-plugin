package com.jetbrains.edu.learning.intellij

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.SubtaskUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.findTestDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.projectView.CourseViewPane
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.util.*

abstract class GradleCourseBuilderBase : EduCourseBuilder<JdkProjectSettings> {

  abstract val buildGradleTemplateName: String
  abstract val subtaskTestTemplateName: String

  override fun createTaskContent(project: Project, task: Task,
                                 parentDirectory: VirtualFile, course: Course): VirtualFile? {
    val taskFolder = super.createTaskContent(project, task, parentDirectory, course)
    ExternalSystemUtil.refreshProjects(project, GradleConstants.SYSTEM_ID, true, ProgressExecutionMode.MODAL_SYNC)
    ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL) {
      ProjectView.getInstance(project).changeViewCB(CourseViewPane.ID, null)
    }
    return taskFolder
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

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)

  companion object {
    private val LOG = Logger.getInstance(GradleCourseBuilderBase::class.java)
  }
}
