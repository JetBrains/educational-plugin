package com.jetbrains.edu.kotlin.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class KtHyperskillConfigurator : HyperskillConfigurator<JdkProjectSettings>(object : KtConfigurator() {
  override fun getMockFileName(text: String): String = MAIN_KT
}) {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = KtHyperskillCourseBuilder()

  private class KtHyperskillCourseBuilder : KtCourseBuilder() {
    override val buildGradleTemplateName: String = KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
  }

  companion object {
    @VisibleForTesting
    const val KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME = "hyperskill-kotlin-build.gradle"
  }
}
