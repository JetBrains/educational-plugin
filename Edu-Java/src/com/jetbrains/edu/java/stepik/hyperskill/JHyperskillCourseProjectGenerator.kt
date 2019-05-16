package com.jetbrains.edu.java.stepik.hyperskill

import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.java.JLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.stepik.hyperskill.HyperskillGradleCourseProjectGenerator
import com.jetbrains.edu.learning.courseFormat.Course

class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase,
                                        course: Course) : HyperskillGradleCourseProjectGenerator(builder, course) {
  override fun getJdk(settings: JdkProjectSettings): Sdk? =
    super.getJdk(settings) ?: JLanguageSettings.findSuitableJdk(myCourse, settings.model)
}
