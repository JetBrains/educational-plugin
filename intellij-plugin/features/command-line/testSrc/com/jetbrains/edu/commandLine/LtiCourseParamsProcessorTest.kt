package com.jetbrains.edu.commandLine

import com.jetbrains.edu.commandLine.processors.CourseParamsProcessor
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.marketplace.lti.LTISettingsManager
import org.junit.Test

class LtiCourseParamsProcessorTest : EduTestCase() {

  override fun createCourse() {
    courseWithFiles(courseMode = CourseMode.STUDENT) {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 2) {
          taskFile("task1.txt")
        }
        eduTask("task2", stepId = 3) {
          taskFile("task2.txt")
        }
      }
    }
  }

  @Test
  fun `test course parameters processing`() {
    val params = mapOf("study_item_id" to "3", "lti_launch_id" to "12445", "lti_lms_description" to "some info string")
    val course = project?.course
    assertNotNull(course)
    CourseParamsProcessor.applyProcessors(project, course!!, params)
    val state = LTISettingsManager.instance(project).state
    assertEquals("12445", state.launchId)
    assertEquals("some info string", state.lmsDescription)
  }
}