package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import org.junit.Test


open class StepikIntegrationTest : StepikTestCase() {

  @Test
  fun testUploadCourse() {
    CCStepikConnector.postCourseWithProgress(project, StudyTaskManager.getInstance(project).course!!)
    checkCourseUploaded(StudyTaskManager.getInstance(project).course as RemoteCourse)
  }
}