package com.jetbrains.edu.javascript.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions
import icons.EducationalCoreIcons
import javax.swing.Icon


open class JsConfigurator : EduConfiguratorWithSubmissions<JsNewProjectSettings>() {
  private val courseBuilder = JsCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<JsNewProjectSettings> = courseBuilder

  override fun getTestFileName() = ""

  override fun getMockFileName(text: String): String = "task.js"

  override fun getTestDirs() = listOf("test")

  override fun getTaskCheckerProvider(): TaskCheckerProvider = TaskCheckerProvider { task, project -> JsEduTaskChecker(task, project) }

  override fun pluginRequirements() = listOf("NodeJS")

  override fun isEnabled() = !EduUtils.isAndroidStudio() && !PlatformUtils.isCommunityEdition() && !PlatformUtils.isPyCharmEducational()

  override fun getLogo(): Icon = EducationalCoreIcons.JsLogo

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
    return super.excludeFromArchive(project, file) || file.path.contains("node_modules") || "package-lock.json" == file.name
  }
}