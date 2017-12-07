package com.jetbrains.edu.kotlin.studio

import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.intellij.JdkProjectSettings

class KtConfigurator : KtConfigurator() {

  private val myCourseBuilder: KtCourseBuilder = KtCourseBuilder()

  override fun getCourseBuilder(): EduCourseBuilder<JdkProjectSettings> = myCourseBuilder

  override fun excludeFromArchive(path: String): Boolean {
    val excluded = super.excludeFromArchive(path)
    return excluded || path.contains("build") || PathUtil.getFileName(path) in NAMES_TO_EXCLUDE
  }

  override fun isEnabled(): Boolean = EduUtils.isAndroidStudio()

  companion object {

    private val NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
            "gradlew", "gradlew.bat", "local.properties", "gradle.properties",
            "build.gradle", "settings.gradle", "gradle-wrapper.jar", "gradle-wrapper.properties")
  }
}
