package com.jetbrains.edu.javascript.learning

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator


open class JsConfigurator : EduConfigurator<JsNewProjectSettings> {
  private val courseBuilder = JsCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<JsNewProjectSettings> = courseBuilder

  override fun getTestFileName() = ""

  override fun getTestDirs() = listOf("test")

  override fun getTaskCheckerProvider(): TaskCheckerProvider = TaskCheckerProvider { task, project -> JsEduTaskChecker(task, project) }

  override fun pluginRequirements() = listOf("NodeJS")

  override fun isEnabled() = !EduUtils.isAndroidStudio() && !PlatformUtils.isCommunityEdition() && !PlatformUtils.isPyCharmEducational()
}