package com.jetbrains.edu.learning

import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.featureManagement.EduFeatureManager
import com.jetbrains.edu.learning.featureManagement.EduManagedFeature
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test

class DisabledFeaturesTest : CourseGenerationTestBase<EmptyProjectSettings>() {

  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  @Test
  fun `test service state`() {
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
        }
      }
    }
    course.disabledFeatures = listOf("ai-hints")
    createCourseStructure(course)

    val featureManager = project.service<EduFeatureManager>()
    assertTrue(featureManager.checkDisabled(EduManagedFeature.AI_HINTS))
  }
}