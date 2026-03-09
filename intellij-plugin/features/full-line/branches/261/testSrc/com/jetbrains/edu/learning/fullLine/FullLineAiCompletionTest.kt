package com.jetbrains.edu.learning.fullLine

import com.intellij.ml.inline.completion.impl.configuration.MLCompletionPerProjectSuppressor
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.featureManagement.EduManagedFeature
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.yaml.YamlTestCase
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FullLineAiCompletionTest : YamlTestCase() {

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

    verify { suppressor.suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy")) }
  }

  @Test
  fun `test ai completion disabled on project startup`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }
    course.disabledFeatures = listOf(EduManagedFeature.AI_COMPLETION.featureKey)
    StudyTaskManager.getInstance(project).course = course

    runBlocking {
      FullLineProjectActivity().execute(project)
    }

    verify { suppressor.suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy")) }
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

    verify { suppressor.suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy")) }
  }
}
