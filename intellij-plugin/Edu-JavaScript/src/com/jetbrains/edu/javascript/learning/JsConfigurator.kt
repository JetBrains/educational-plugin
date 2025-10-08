package com.jetbrains.edu.javascript.learning

import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.javascript.learning.checker.JsTaskCheckerProvider
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.courseFormat.Course
import javax.swing.Icon


open class JsConfigurator : EduConfigurator<JsNewProjectSettings> {
  override val courseBuilder: EduCourseBuilder<JsNewProjectSettings>
    get() = JsCourseBuilder()

  override val testFileName: String
    get() = ""

  override fun getMockFileName(course: Course, text: String): String = TASK_JS

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val taskCheckerProvider: TaskCheckerProvider
    get() = JsTaskCheckerProvider()

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("NodeJS"))

  override val logo: Icon
    get() = EducationalCoreIcons.Language.JavaScript

  override val isEnabled: Boolean
    get() = !PlatformUtils.isRider()

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    file(NodeModuleNamesUtil.PACKAGE_JSON) {
      archiveInclusionPolicy(ArchiveInclusionPolicy.SHOULD_BE_INCLUDED)
    }

    dirAndChildren(NodeModuleNamesUtil.MODULES) {
      @Suppress("DEPRECATION")
      legacyExcludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    file("package-lock.json") {
      @Suppress("DEPRECATION")
      legacyExcludeFromArchive()
    }
  }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val MAIN_JS = "main.js"
    const val TASK_JS = "task.js"
    const val TEST_JS = "test.js"
  }
}
