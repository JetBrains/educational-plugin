package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.junit.Test
import org.junit.Assert.assertTrue as check


var courseEducator = EduCourse()
var courseLearner = EduCourse()

class ClientTest : EduTestCase() {

  @Test
  fun `test - create course`() {
    courseEducator = sampleCourse()
    ServerConnector.createCourse(courseEducator)
  }

  @Test
  fun `test - get last course 1`() {
    val courses = ServerConnector.getAvailableCourses()
    val courseId = courses.maxBy { it.courseId }
    courseId?.let {
      courseLearner = ServerConnector.getCourseMaterials(it.courseId)
    }
    val sec1 = courseLearner.items[1] as Section
    val les2 = sec1.items[0] as Lesson
    val task5 = les2.taskList[1] as Task
    check(task5.name == "PA #5")
    val sec2 = courseLearner.items[2] as Section
    val les5 = sec2.items[1] as Lesson
    val task13 = les5.taskList[1] as Task
    check(task13.name == "PA #13")

  }

  @Test
  fun `test - update course`() {
    val sec1 = courseEducator.items[1] as Section
    val les2 = sec1.items[0] as Lesson
    val task = les2.taskList[1] as Task
    sec1.stepikChangeStatus = StepikChangeStatus.CONTENT
    les2.stepikChangeStatus = StepikChangeStatus.CONTENT
    task.stepikChangeStatus = StepikChangeStatus.INFO_AND_CONTENT
    task.name = "${task.name} updated"
    ServerConnector.updateCourse(courseEducator)
  }

  @Test
  fun `test - get last course 2`() {
    val courses = ServerConnector.getAvailableCourses()
    val courseId = courses.maxBy { it.courseId }
    courseId?.let {
      courseLearner = ServerConnector.getCourseMaterials(it.courseId)
    }
    val sec1 = courseLearner.items[1] as Section
    val les2 = sec1.items[0] as Lesson
    val task5 = les2.taskList[1] as Task
    check(task5.name == "PA #5 updated")
    val sec2 = courseLearner.items[2] as Section
    val les5 = sec2.items[1] as Lesson
    val task13 = les5.taskList[1] as Task
    check(task13.name == "PA #13")
  }

}