package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import org.junit.Test
import org.junit.Assert.assertTrue as check


var courseEducator = EduCourse()
var courseLearner = EduCourse()

class ClientTest : EduTestCase() {

  @Test
  fun `test - create course`() {
    courseEducator = sampleCourse()
    ServerClient.createCourse(courseEducator)
  }

  @Test
  fun `test - get last course`() {
    val courses = ServerClient.getAvailableCourses()
    val courseId = courses.maxBy { it.courseId }
    courseId?.let {
      courseLearner = ServerClient.getCourseMaterials(it.courseId)
    }
  }

  @Test
  fun `test - update course`() {
    val sec1 = courseEducator.items[1] as Section
    val les2 = sec1.items[0] as Lesson
    val task = les2.taskList[1] as OutputTask
    sec1.isUploaded = false
    les2.isUploaded = false
    task.isUploaded = false
    task.name = "${task.name} updated"
    ServerClient.updateCourse(courseEducator)

    /* Issue: PUT request
     *
     * `com.fasterxml.jackson.databind.exc.InvalidTypeIdException`
     * Missing type id when trying to resolve subtype of ...
     *
     * Server doesn't return type fields, for all objects,
     * with is necessary for deserialization.
     *
     * UPD 18/08/22: 500 Internal server error
     *
     */
  }


}