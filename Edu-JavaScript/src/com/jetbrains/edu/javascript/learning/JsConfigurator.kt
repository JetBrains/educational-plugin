package com.jetbrains.edu.javascript.learning

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PlatformUtils.isCommunityEdition
import com.intellij.util.PlatformUtils.isPyCharmEducational
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt.isAndroidStudio
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import javax.swing.Icon


open class JsConfigurator : EduConfigurator<JsNewProjectSettings> {
  override val courseBuilder: EduCourseBuilder<JsNewProjectSettings>
    get() = JsCourseBuilder()

  override val testFileName: String
    get() = ""

  override fun getMockFileName(text: String): String = TASK_JS

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val taskCheckerProvider: TaskCheckerProvider
    get() = JsTaskCheckerProvider()

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("NodeJS"))

  override val isEnabled: Boolean
    get() = !isAndroidStudio() && !isCommunityEdition() && !isPyCharmEducational()

  override val logo: Icon
    get() = EducationalCoreIcons.JsLogo

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean =
    super.excludeFromArchive(project, file) || file.path.contains("node_modules") || "package-lock.json" == file.name

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val MAIN_JS = "main.js"
    const val TASK_JS = "task.js"
    const val TEST_JS = "test.js"
  }
}