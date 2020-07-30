package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import com.jetbrains.edu.scala.messages.EduScalaBundle
import icons.EducationalCoreIcons
import javax.swing.Icon

class ScalaSbtConfigurator : EduConfigurator<JdkProjectSettings> {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = ScalaSbtCourseBuilder()

  override val testFileName: String
    get() = TEST_SCALA

  override val isEnabled: Boolean
    get() = !EduUtils.isAndroidStudio()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = object : TaskCheckerProvider {
      override val envChecker: EnvironmentChecker
        get() = ScalaSbtEnvironmentChecker()

      override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> =
        ScalaSbtEduTaskChecker(task, envChecker, project)
    }

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_SCALA)

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val logo: Icon
    get() = EducationalCoreIcons.ScalaLogo

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
    return super.excludeFromArchive(project, file) ||
           generateSequence(file, VirtualFile::getParent).any { it.name == "target" || it.name == "project" }
  }

  private class ScalaSbtEnvironmentChecker: EnvironmentChecker() {
    override fun checkEnvironment(project: Project, task: Task): String? {
      if (ProjectRootManager.getInstance(project).projectSdk == null) {
        return EduCoreErrorBundle.message("no.sdk")
      }
      if (!project.isSbtProject) {
        return EduScalaBundle.message("error.no.sbt.project")
      }
      return null
    }
  }

  companion object {
    const val TEST_SCALA = "TestSpec.scala"
    const val TASK_SCALA = "Task.scala"
    const val MAIN_SCALA = "Main.scala"
    const val MOCK_SCALA = "Mock.scala"
  }
}
