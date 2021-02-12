package com.jetbrains.edu.java.hyperskill

import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.java.JLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.GradleHyperskillConfigurator
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseProjectGenerator

class JHyperskillConfigurator : GradleHyperskillConfigurator<JdkProjectSettings>(JConfigurator()) {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = JHyperskillCourseBuilder(JCourseBuilder())

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST, "${EduNames.TEST}/stageTest")

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
      return super.getJdk(settings) ?: JLanguageSettings.findSuitableJdk(myCourse, settings.model)
    }
  }
}
