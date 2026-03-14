package com.jetbrains.edu.learning.ai.completion

import com.intellij.ml.inline.completion.impl.configuration.MLCompletionPerProjectSuppressor
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.featureManagement.EduManagedFeature
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.yaml.YamlTestCase
import io.mockk.verify
import org.junit.Test

class AiCompletionTest : YamlTestCase() {

  private lateinit var suppressor: MLCompletionPerProjectSuppressor

  override fun setUp() {
    super.setUp()
    suppressor = mockService<MLCompletionPerProjectSuppressor>(project)
  }

  @Test
  fun `test ai completion disabled`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }
    course.disabledFeatures = listOf(EduManagedFeature.AI_COMPLETION.featureKey)
    updateAiCompletion(project, course)

    verify (exactly = 1) { suppressor.suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy")) }
  }

  @Test
  fun `test ai completion disabled via yaml`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }

    val yamlContent = """
      |title: Test Course
      |language: English
      |summary: Some summary
      |programming_language: Plain text
      |disabled_features:
      |- ai-completion
    """.trimMargin()

    loadItemFromConfig(course, yamlContent)

    verify (exactly = 1) { suppressor.suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy")) }
  }

  @Test
  fun `test ai completion is not disabled twice`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }

    val yamlContent = """
      |title: Test Course
      |language: English
      |summary: Some summary
      |programming_language: Plain text
      |disabled_features:
      |- ai-completion
    """.trimMargin()

    loadItemFromConfig(course, yamlContent)
    course.disabledFeatures = listOf(EduManagedFeature.AI_COMPLETION.featureKey)
    updateAiCompletion(project, course)

    verify(exactly = 1) { suppressor.suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy")) }
  }

  @Test
  fun `test ai completion is not changed when yaml is modified`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }

    val yamlContent = """
      |title: Test Course
      |language: English
      |summary: Some summary
      |programming_language: Plain text
      |disabled_features:
      |- ai-completion
    """.trimMargin()

    loadItemFromConfig(course, yamlContent)

    val newYamlContent = """
      |title: Test Course
      |language: English
      |summary: Some new summary
      |programming_language: Plain text
      |disabled_features:
      |- ai-completion
    """.trimMargin()

    loadItemFromConfig(course, newYamlContent)

    verify(exactly = 1) { suppressor.suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy")) }
  }
}
