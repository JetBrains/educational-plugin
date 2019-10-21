package com.jetbrains.edu.cpp

import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.learning.FindTaskFileTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course

class CppFindTaskTest : FindTaskFileTestBase<CppProjectSettings>() {

  override val defaultSettings = CppProjectSettings()

  fun `test get task dir from top-level lesson`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskDir(
      course = courseForTests,
      filePath = "./lesson1/task2/src/task.cpp",
      taskDirPath = "./lesson1/task2"
    )
  }

  fun `test get task dir from top-level section`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskDir(
      course = courseForTests,
      filePath = "./section1/lesson1/task2/src/task.cpp",
      taskDirPath = "./section1/lesson1/task2"
    )
  }

  fun `test get task for file from top-level lesson`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskForFile(
      course = courseForTests,
      filePath = "./lesson1/task2/src/task.cpp"
    ) { it.lessons[0].taskList[1] }
  }

  fun `test get task for file from top-level section`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskForFile(
      course = courseForTests,
      filePath = "./section1/lesson1/task2/src/task.cpp"
    ) { it.sections[0].lessons[0].taskList[1] }
  }

  fun `test get task file from top-level lesson`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskFile(
      course = courseForTests,
      filePath = "./lesson1/task2/src/task.cpp"
    ) { it.lessons[0].taskList[1].taskFiles["src/task.cpp"]!! }
  }

  fun `test get task file from top-level section`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskFile(
      course = courseForTests,
      filePath = "./section1/lesson1/task2/src/task.cpp"
    ) { it.sections[0].lessons[0].taskList[1].taskFiles["src/task.cpp"]!! }
  }

  private fun getCourseForTests(): Course {
    return course(language = OCLanguage.getInstance(), environment = "GoogleTest") {
      section {
        lesson {
          eduTask {
            taskFile("src/task.cpp")
            taskFile("CMakeLists.txt")
          }

          theoryTask {
            taskFile("src/task.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }

      lesson {
        eduTask {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }

        theoryTask {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }
      }

      additionalFiles {
        taskFile("CMakeLists.txt")
      }
    }
  }
}
