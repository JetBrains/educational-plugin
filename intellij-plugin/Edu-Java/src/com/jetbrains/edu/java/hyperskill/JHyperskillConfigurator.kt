package com.jetbrains.edu.java.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.ParsedJavaVersion
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.GradleHyperskillConfigurator
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseProjectGenerator

class JHyperskillConfigurator : GradleHyperskillConfigurator<JdkProjectSettings>(JConfigurator()) {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = JHyperskillCourseBuilder(JHyperskillGradleCourseBuilder())

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST, "${EduNames.TEST}/stageTest")

  private class JHyperskillGradleCourseBuilder : JCourseBuilder() {
    override fun buildGradleTemplateName(course: Course): String = JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
    override fun settingGradleTemplateName(course: Course): String = HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
    override fun getLanguageSettings(): JHyperskillLanguageSettings = JHyperskillLanguageSettings()
  }

  private class JHyperskillCourseBuilder(private val gradleCourseBuilder: GradleCourseBuilderBase) :
    HyperskillCourseBuilder<JdkProjectSettings>(gradleCourseBuilder) {

    override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings>? {
      if (course !is HyperskillCourse) return null
      val generatorBase = JHyperskillCourseProjectGenerator(gradleCourseBuilder, course)
      return HyperskillCourseProjectGenerator(generatorBase, this, course)
    }
  }

  private class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase, course: Course) :
    GradleCourseProjectGenerator(builder, course) {

    override fun getJdk(settings: JdkProjectSettings): Sdk? {
      return super.getJdk(settings) ?: JdkLanguageSettings.findSuitableJdk(
        ParsedJavaVersion.fromJavaSdkDescriptionString(course.languageVersion),
        settings.model
      )
    }
  }

  companion object {
    @VisibleForTesting
    const val JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME = "hyperskill-java-build.gradle"
  }
}
