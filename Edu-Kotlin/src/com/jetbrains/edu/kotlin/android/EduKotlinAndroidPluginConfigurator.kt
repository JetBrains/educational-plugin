package com.jetbrains.edu.kotlin.android

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.kotlin.EduKotlinPluginConfigurator
import com.jetbrains.edu.learning.checker.StudyTaskChecker
import com.jetbrains.edu.learning.core.EduNames
import com.jetbrains.edu.learning.core.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator
import java.io.IOException

class EduKotlinAndroidPluginConfigurator : EduKotlinPluginConfigurator() {

  override fun excludeFromArchive(path: String): Boolean {
    val excluded = super.excludeFromArchive(path)
    return excluded || path.contains("build") || PathUtil.getFileName(path) in NAMES_TO_EXCLUDE
  }

  override fun getPyCharmTaskChecker(pyCharmTask: PyCharmTask, project: Project): StudyTaskChecker<PyCharmTask> =
          EduKotlinAndroidPyCharmTaskChecker(pyCharmTask, project)

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

  override fun getEduCourseProjectGenerator(course: Course): EduCourseProjectGenerator<JdkProjectSettings>? =
          KoansAndroidProjectGenerator(course)

  override fun isEnabled(): Boolean = EduUtils.isAndroidStudio()

  companion object {
    private val LOG = Logger.getInstance(EduKotlinAndroidPluginConfigurator::class.java)

    private val NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
            "gradlew", "gradlew.bat", "local.properties", "gradle.properties",
            "build.gradle", "settings.gradle", "gradle-wrapper.jar", "gradle-wrapper.properties")

    @JvmStatic
    fun initTask(task: Task) {
      val taskFile = TaskFile()
      taskFile.task = task
      taskFile.name = TASK_KT
      taskFile.text = EduUtils.getTextFromInternalTemplate(TASK_KT)
      task.addTaskFile(taskFile)
      task.testsText.put(TESTS_KT, EduUtils.getTextFromInternalTemplate(TESTS_KT))
    }
  }
}
