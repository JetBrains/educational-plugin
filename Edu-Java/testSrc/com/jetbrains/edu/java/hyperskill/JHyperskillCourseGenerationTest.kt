package com.jetbrains.edu.java.hyperskill

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.java.hyperskill.JHyperskillConfigurator.Companion.JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME

class JHyperskillCourseGenerationTest : EduTestCase() {
  fun `test build gradle file`() {
    courseWithFiles(courseProducer = ::HyperskillCourse,
                    language = JavaLanguage.INSTANCE,
                    settings = JdkProjectSettings.emptySettings()) {}
    val actualBuildGradleContent = findFile(DEFAULT_SCRIPT_NAME).document.text
    val expectedBuildGradleContent = GeneratorUtils.getInternalTemplateText(JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME)

    assertEquals(expectedBuildGradleContent, actualBuildGradleContent)
  }
}