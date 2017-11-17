package com.jetbrains.edu.kotlin.studio

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.IOException

class KtCourseBuilder : KtCourseBuilder() {

  override fun createTaskContent(project: Project, task: Task,
                                 parentDirectory: VirtualFile, course: Course): VirtualFile? {
    initTask(task)
    ApplicationManager.getApplication().runWriteAction {
      try {
        EduGradleModuleGenerator.createTaskModule(parentDirectory, task)
      } catch (e: IOException) {
        LOG.error("Failed to create task")
      }
    }

    ExternalSystemUtil.refreshProjects(project, ProjectSystemId("GRADLE"), true, ProgressExecutionMode.MODAL_SYNC)
    return parentDirectory.findChild(EduNames.TASK + task.index)
  }

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings>? =
          KtProjectGenerator(course)

  companion object {
    private val LOG = Logger.getInstance(KtCourseBuilder::class.java)

    @JvmStatic
    fun initTask(task: Task) {
      val taskFile = TaskFile()
      taskFile.task = task
      taskFile.name = KtConfigurator.TASK_KT
      taskFile.text = EduUtils.getTextFromInternalTemplate(KtConfigurator.TASK_KT)
      task.addTaskFile(taskFile)
      task.testsText.put(KtConfigurator.TESTS_KT, EduUtils.getTextFromInternalTemplate(KtConfigurator.TESTS_KT))
    }
  }
}
