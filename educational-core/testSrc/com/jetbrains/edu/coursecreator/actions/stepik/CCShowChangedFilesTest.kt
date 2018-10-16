package com.jetbrains.edu.coursecreator.actions.stepik

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.stepik.actions.CCShowChangedFiles
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.remote.LocalInfo
import com.jetbrains.edu.learning.stepik.courseFormat.StepikChangeStatus.*
import junit.framework.TestCase

class CCShowChangedFilesTest: EduActionTestCase() {

  fun `test course up to date`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
      section("section2") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }.asRemote()

    checkMessage(course, "No changes")
  }

  fun `test course content changed`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
      section("section2") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }.asRemote()

    val changedSection = course.getSection("section2")!!
    changedSection.stepikChangeStatus = CONTENT
    changedSection.remoteInfo = LocalInfo()
    changedSection.lessons[0].remoteInfo = LocalInfo()

    checkMessage(course,  "section2 $CONTENT\nsection2 New\nsection2/lesson1 New\n")
  }

  fun `test course lesson moved`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson("lesson2")
      section()
    }.asRemote()

    course.stepikChangeStatus = CONTENT
    course.sections[0].stepikChangeStatus = INFO_AND_CONTENT
    course.getLesson("lesson2")!!.stepikChangeStatus = INFO
    val changedSection = course.sections[0]!!
    val changedLesson = course.getLesson("lesson2")!!

    checkMessage(course,
                 "${course.name} ${CONTENT}\n${changedLesson.name} ${INFO}\n${changedSection.name} ${INFO_AND_CONTENT}\n")
  }

  fun `test section renamed`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }.asRemote()

    val changedSection = course.sections[0]
    changedSection.stepikChangeStatus = INFO
    checkMessage(course, "${changedSection.name} $INFO\n")
  }

  fun `test lesson added into section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1"){
        lesson {
          eduTask()
        }
        lesson {
          eduTask()
        }
      }
    }.asRemote()

    val changedSection = course.sections[0]
    changedSection.stepikChangeStatus = CONTENT
    changedSection.lessons[1].remoteInfo = LocalInfo()
    checkMessage(course, "${changedSection.name} ${CONTENT}\nsection1/lesson2 New\n")
  }

  fun `test lesson moved between sections`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1")
      }
      section("section2") {
        lesson("lesson1")
        lesson("lesson2")
      }
    }.asRemote()

    course.sections.forEach {
      it.stepikChangeStatus = CONTENT
    }

    val expected = course.sections.joinToString(separator = "") { "${it.name} ${CONTENT}\n" }
    checkMessage(course, expected)
  }

  fun `test task moved between lessons`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1")
        eduTask("task2")
      }

      lesson("lesson2") {
        eduTask("task1")
        eduTask("task3")
      }
    }.asRemote()

    course.lessons.forEach {
      it.stepikChangeStatus = CONTENT
    }
    val expected = course.lessons.joinToString(separator = "") { "${it.name} ${CONTENT}\n" }
    checkMessage(course, expected)
  }

  fun `test task moved inside lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1")
        eduTask("task2")
        eduTask("task3")
      }
    }.asRemote()

    val changedTask2 = course.lessons[0].getTask("task2")!!
    changedTask2.stepikChangeStatus = INFO_AND_CONTENT

    val changedTask3 = course.lessons[0].getTask("task3")!!
    changedTask3.stepikChangeStatus = INFO_AND_CONTENT

    checkMessage(course, "lesson1/${changedTask2.name} ${INFO_AND_CONTENT}\nlesson1/${changedTask3.name} ${INFO_AND_CONTENT}\n")
  }

  private fun checkMessage(course: Course, expectedMessage: String) {
    TestCase.assertEquals(expectedMessage, CCShowChangedFiles.buildChangeMessage(course))
  }
}

