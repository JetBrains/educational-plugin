package com.jetbrains.edu.learning.ai.completion

import com.intellij.ml.inline.completion.impl.configuration.MLCompletionPerProjectSuppressor
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.featureManagement.EduManagedFeature
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test

class AiCompletionProjectActivityTest : CourseGenerationTestBase<EmptyProjectSettings>() {

  override val defaultSettings: EmptyProjectSettings
    get() = EmptyProjectSettings

  @Test
  fun `test ai completion disabled on project startup`() {
    val course = course(courseMode = CourseMode.STUDENT) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }.apply {
      disabledFeatures = listOf(EduManagedFeature.AI_COMPLETION.featureKey)
    }

    createCourseStructure(course)
    assertTrue(MLCompletionPerProjectSuppressor.getInstance(project).isSuppressed())
  }
}
