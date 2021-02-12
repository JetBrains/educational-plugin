package com.jetbrains.edu.scala.hyperskill

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.scala.hyperskill.ScalaHyperskillConfigurator.Companion.SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaHyperskillCourseGenerationTest : EduTestCase() {
  fun `test build gradle file`() {
    courseWithFiles(courseProducer = ::HyperskillCourse,
                    language = ScalaLanguage.INSTANCE,
                    settings = JdkProjectSettings.emptySettings()) {}
    val actualBuildGradleContent = findFile(DEFAULT_SCRIPT_NAME).document.text
    val expectedBuildGradleContent = GeneratorUtils.getInternalTemplateText(SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME)

    assertEquals(expectedBuildGradleContent, actualBuildGradleContent)
  }
}