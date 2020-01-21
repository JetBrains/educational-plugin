package com.jetbrains.edu.java.stepik.hyperskill

import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.java.JLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillCourseBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillCourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class JHyperskillConfigurator : HyperskillConfigurator<JdkProjectSettings>(JConfigurator()) {
  private val courseBuilder = JHyperskillCourseBuilder(JCourseBuilder())

  override fun getCourseBuilder(): EduCourseBuilder<JdkProjectSettings> = courseBuilder
  override fun getTestDirs() = listOf("${EduNames.TEST}/stageTest", EduNames.TEST)

  private class JHyperskillCourseBuilder(private val gradleCourseBuilder: GradleCourseBuilderBase) :
    HyperskillCourseBuilder<JdkProjectSettings>(gradleCourseBuilder) {

    override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings>? {
      val generatorBase = JHyperskillCourseProjectGenerator(gradleCourseBuilder, course)
      val hyperskillCourse = course as? HyperskillCourse ?: return null
      return HyperskillCourseProjectGenerator(generatorBase, this, hyperskillCourse)
    }
  }

  private class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase, course: Course) :
    GradleCourseProjectGenerator(builder, course) {

    override fun getJdk(settings: JdkProjectSettings): Sdk? {
      return super.getJdk(settings) ?: JLanguageSettings.findSuitableJdk(myCourse, settings.model)
    }
  }
}
