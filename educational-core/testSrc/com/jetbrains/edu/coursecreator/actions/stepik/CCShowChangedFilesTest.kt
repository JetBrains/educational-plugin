package com.jetbrains.edu.coursecreator.actions.stepik

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import junit.framework.TestCase

class CCShowChangedFilesTest : EduActionTestCase() {

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

    val remoteCourse = course(courseMode = CCUtils.COURSE_MODE) {
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

    checkMessage(course, remoteCourse, "No changes")
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
      section("section3") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }.asRemote()

    val remoteCourse = course(courseMode = CCUtils.COURSE_MODE) {
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

    course.getSection("section3")!!.id = 0
    checkMessage(course, remoteCourse, "section3 New\n")
  }

  fun `test course lesson moved`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson()
      lesson("lesson2")
      section()
    }.asRemote()

    val remoteCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson2")
      lesson()
      section()
    }.asRemote()

    course.sectionIds = listOf(123)
    remoteCourse.sectionIds = listOf(123)
    checkMessage(course, remoteCourse,
                 "lesson1 Info Changed\n")
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

    val remoteCourse = course(courseMode = CCUtils.COURSE_MODE) {
      section("section2") {
        lesson("lesson1") {
          eduTask {
            taskFile("fizz.kt")
          }
        }
      }
    }.asRemote()

    val changedSection = course.sections[0]
    checkMessage(course, remoteCourse, "${changedSection.name} Info Changed\n")
  }

  fun `test lesson added into section`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson {
          eduTask()
        }
        lesson {
          eduTask()
        }
        lesson {
          eduTask()
        }
      }
    }.asRemote()

    val remoteCourse = course(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson {
          eduTask()
        }
        lesson {
          eduTask()
        }
      }
    }.asRemote()

    course.sections[0].lessons[2].id = 0
    checkMessage(course, remoteCourse, "section1/lesson3 New\n")
  }

  fun `test lesson moved between sections`() {
    val remoteCourse = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson11")
        lesson("lesson12")
      }
      section("section2") {
        lesson("lesson22")
      }
    }.asRemote()
    val course = course(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson11")
      }
      section("section2") {
        lesson("lesson12")
        lesson("lesson22")
      }
    }.asRemote()

    course.sections[1].getLesson("lesson12")!!.id = 21
    remoteCourse.sections[0].getLesson("lesson12")!!.id = 12

    course.sections[1].getLesson("lesson22")!!.id = 22
    remoteCourse.sections[1].getLesson("lesson22")!!.id = 22

    checkMessage(course, remoteCourse, "section2/lesson12 New\n" +
                                       "section1/lesson12 Removed\n" +
                                       "section2/lesson22 Info Changed\n")
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
    val remoteCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1")
        eduTask("task3")
      }
      lesson("lesson2") {
        eduTask("task1")
        eduTask("task2")
      }
    }.asRemote()
    course.sectionIds = listOf(123)
    remoteCourse.sectionIds = listOf(123)

    checkMessage(course, remoteCourse, "lesson1/task2 Info Changed\nlesson2/task3 Info Changed\n")
  }


  fun `test task moved inside lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1")
        eduTask("task2")
        eduTask("task3")
      }
    }.asRemote()
    val remoteCourse = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task2")
        eduTask("task1")
        eduTask("task3")
      }
    }.asRemote()

    course.sectionIds = listOf(123)
    remoteCourse.sectionIds = listOf(123)
    checkMessage(course, remoteCourse, "lesson1/task1 Info Changed\nlesson1/task2 Info Changed\n")
  }

  private fun checkMessage(course: EduCourse, remoteCourse: EduCourse, expectedMessage: String) {
    TestCase.assertEquals(expectedMessage, CCShowChangedFiles.buildChangeMessage(course, remoteCourse, myFixture.project))
  }
}

