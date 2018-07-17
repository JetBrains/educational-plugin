package com.jetbrains.edu.coursecreator.actions.stepik

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus.*
import junit.framework.TestCase

class CCShowChangedFilesTest: CCActionTestCase() {

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
    }

    checkMessage(course, "")
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
    }

    course.getSection("section2")!!.stepikChangeStatus = CONTENT

    checkMessage(course,  "section2 $CONTENT\n" )
  }

  fun `test course lesson moved`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson("lesson2")
      section()
    }

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
    }

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
    }

    val changedSection = course.sections[0]
    changedSection.stepikChangeStatus = CONTENT
    checkMessage(course, "${changedSection.name} ${CONTENT}\n")
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
    }

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
    }

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
    }

    val changedTask2 = course.lessons[0].getTask("task2")!!
    changedTask2.stepikChangeStatus = INFO_AND_CONTENT

    val changedTask3 = course.lessons[0].getTask("task3")!!
    changedTask3.stepikChangeStatus = INFO_AND_CONTENT

    checkMessage(course, "lesson1/${changedTask2.name} ${INFO_AND_CONTENT}\nlesson1/${changedTask3.name} ${INFO_AND_CONTENT}\n")
  }

  private fun checkMessage(course: Course, expectedMessage: String) {
    TestCase.assertEquals(expectedMessage, CCShowChangedFiles.buildChangeMessage(course).toString())
  }
}

