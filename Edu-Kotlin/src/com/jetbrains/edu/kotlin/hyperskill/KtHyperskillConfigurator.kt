package com.jetbrains.edu.kotlin.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.kotlin.KtConfigurator.Companion.MAIN_KT
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class KtHyperskillConfigurator : HyperskillConfigurator<JdkProjectSettings>(KtConfigurator()) {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = KtHyperskillCourseBuilder()

  override fun getMockFileName(course: Course, text: String): String = MAIN_KT

  private class KtHyperskillCourseBuilder : KtCourseBuilder() {
    override fun buildGradleTemplateName(course: Course): String = KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
    override fun settingGradleTemplateName(course: Course): String = HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
  }

  companion object {
    @VisibleForTesting
    const val KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME = "hyperskill-kotlin-build.gradle"
  }
}
