package com.jetbrains.edu.javascript.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PlatformUtils.isCommunityEdition
import com.intellij.util.PlatformUtils.isPyCharmEducational
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtils.isAndroidStudio
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions
import icons.EducationalCoreIcons
import javax.swing.Icon


open class JsConfigurator : EduConfiguratorWithSubmissions<JsNewProjectSettings>() {
  override val courseBuilder: EduCourseBuilder<JsNewProjectSettings>
    get() = JsCourseBuilder()

  override val testFileName: String
    get() = ""

  override fun getMockFileName(text: String): String = TASK_JS

  override val testDirs: List<String>
    get() = listOf("test")

  override val taskCheckerProvider: TaskCheckerProvider
    get() = JsTaskCheckerProvider()

  override val pluginRequirements: List<String>
    get() = listOf("NodeJS")

  override val isEnabled: Boolean
    get() = !isAndroidStudio() && !isCommunityEdition() && !isPyCharmEducational()

  override val logo: Icon
    get() = EducationalCoreIcons.JsLogo

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean =
    super.excludeFromArchive(project, file) || file.path.contains("node_modules") || "package-lock.json" == file.name

  companion object {
    const val TASK_JS = "task.js"
  }
}