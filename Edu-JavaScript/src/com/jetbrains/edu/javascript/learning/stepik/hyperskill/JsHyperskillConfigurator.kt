package com.jetbrains.edu.javascript.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.javascript.learning.JsConfigurator
import com.jetbrains.edu.javascript.learning.JsConfigurator.Companion.TASK_JS
import com.jetbrains.edu.javascript.learning.JsCourseBuilder
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator.Companion.TEST

class JsHyperskillConfigurator : HyperskillConfigurator<JsNewProjectSettings> {
  private val courseBuilder: JsCourseBuilder = JsCourseBuilder()
  override fun getCourseBuilder(): EduCourseBuilder<JsNewProjectSettings> = courseBuilder
  override fun getTaskCheckerProvider() = JsHyperskillTaskCheckerProvider()
  override fun getMockFileName(text: String): String = TASK_JS
  override fun getTestDirs(): MutableList<String> = mutableListOf(TEST)
  override fun isEnabled() = !EduUtils.isAndroidStudio() && !PlatformUtils.isCommunityEdition() && !PlatformUtils.isPyCharmEducational()

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
    return super.excludeFromArchive(project, file) || JsConfigurator.excludeFromArchive(file)
  }
}
