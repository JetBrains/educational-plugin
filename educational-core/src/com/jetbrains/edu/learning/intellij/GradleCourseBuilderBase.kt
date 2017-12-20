package com.jetbrains.edu.learning.intellij

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator
import org.jetbrains.plugins.gradle.util.GradleConstants

import java.io.IOException

abstract class GradleCourseBuilderBase : EduCourseBuilder<JdkProjectSettings> {

  abstract val buildGradleTemplateName: String
  abstract val taskTemplateName: String
  abstract val testTemplateName: String

  override fun createTaskContent(project: Project, task: Task,
                                 parentDirectory: VirtualFile, course: Course): VirtualFile? {
    initNewTask(task)
    runWriteAction {
      try {
        EduGradleModuleGenerator.createTaskModule(parentDirectory, task)
      } catch (e: IOException) {
        EduCourseBuilder.LOG.error("Failed to create task", e)
      }
    }

    ExternalSystemUtil.refreshProjects(project, GradleConstants.SYSTEM_ID, true, ProgressExecutionMode.MODAL_SYNC)
    return parentDirectory.findChild(EduNames.TASK + task.index)
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

  abstract override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator?
}
