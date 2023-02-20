package com.jetbrains.edu.kotlin.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.kotlin.KtConfigurator.Companion.MAIN_KT
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class KtHyperskillConfigurator : HyperskillConfigurator<JdkProjectSettings>(KtConfigurator()) {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = KtHyperskillCourseBuilder()

  override fun getMockFileName(text: String): String = MAIN_KT

  private class KtHyperskillCourseBuilder : KtCourseBuilder() {
    override val buildGradleTemplateName: String = KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
    override val settingGradleTemplateName: String = HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME

    override val buildGradleKtsTemplateName: String = KOTLIN_HYPERSKILL_BUILD_GRADLE_KOTLIN_DSL_TEMPLATE_NAME
    override val settingGradleKtsTemplateName: String = KOTLIN_HYPERSKILL_SETTINGS_GRADLE_KOTLIN_DSL_TEMPLATE_NAME
  }

  companion object {
    @VisibleForTesting
    const val KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME = "hyperskill-kotlin-build.gradle"

    const val KOTLIN_HYPERSKILL_BUILD_GRADLE_KOTLIN_DSL_TEMPLATE_NAME = "hyperskill-kotlin-build.gradle.kts"
    const val KOTLIN_HYPERSKILL_SETTINGS_GRADLE_KOTLIN_DSL_TEMPLATE_NAME = "hyperskill-settings.gradle.kts"
  }
}
