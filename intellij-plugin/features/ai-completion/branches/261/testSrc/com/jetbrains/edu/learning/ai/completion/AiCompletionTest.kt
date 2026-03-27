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
  fun `test ai completion is not disabled twice`() {
    val course = courseWithFiles(courseMode = CourseMode.STUDENT) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }

    course.disabledFeatures = listOf(EduManagedFeature.AI_COMPLETION.featureKey)
    disableAiCompletion(project, course)
    disableAiCompletion(project, course)

    verify(exactly = 1) { suppressor.suppress(MLCompletionPerProjectSuppressor.Token("JetBrains Academy")) }
  }
}
