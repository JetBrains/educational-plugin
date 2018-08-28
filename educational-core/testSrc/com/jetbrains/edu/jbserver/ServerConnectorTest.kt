package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.junit.Assert.assertTrue as check


var courseEducator = EduCourse()
var courseLearner = EduCourse()


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ServerConnectorTest : EduTestCase() {

  @Test
  fun `test 1 cc create course`() {
    courseEducator = sampleCourse()
    ServerConnector.createCourse(courseEducator)
    println(courseEducator.info())
  }

  @Test
  fun `test 2 learner get course`() {
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
    println(courseLearner.info())
  }

  @Test
  fun `test 3 cc update course`() {
    val sec1 = courseEducator.items[1] as Section
    val les2 = sec1.items[0] as Lesson
    val task = les2.taskList[1] as Task
    sec1.stepikChangeStatus = StepikChangeStatus.CONTENT
    les2.stepikChangeStatus = StepikChangeStatus.CONTENT
    task.stepikChangeStatus = StepikChangeStatus.INFO_AND_CONTENT
    task.name = "${task.name} updated"
    ServerConnector.updateCourse(courseEducator)
    println(courseEducator.info())
  }

  @Test
  fun `test 4 learner update course`() {
    check(ServerConnector.isCourseUpdated(courseLearner))
    courseLearner = ServerConnector.getCourseUpdate(courseLearner)
    println("\n${courseLearner.info()}")

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