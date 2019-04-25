package com.jetbrains.edu.cpp

import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard
import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.fileTree

class CppCourseBuilderTest : CourseGenerationTestBase<CppProjectSettings>() {

  override val courseBuilder = CppCourseBuilder()
  override val defaultSettings = CppProjectSettings(CMakeRecognizedCPPLanguageStandard.CPP14.displayString)

  fun `test study course structure with top-level lesson`() {
    val course = course(language = OCLanguage.getInstance()) {
      lesson {
        eduTask {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }
      }
      additionalFiles {
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  fun `test study course structure with top-level section`() {
    val course = course(language = OCLanguage.getInstance()) {
      section {
        lesson {
          eduTask {
            taskFile("src/task.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }
      additionalFiles {
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section1/lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  fun `test study course structure with top-level section and lesson`() {
    val course = course(language = OCLanguage.getInstance()) {
      section {
        lesson {
          eduTask {
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
      }

      additionalFiles {
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section1/lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("CMakeLists.txt")
      }
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  fun `test study course structure with Russian names`() {
    val course = course(language = OCLanguage.getInstance()) {
      section(name = "Введение в язык C++") {
        lesson(name = "Обзор возможностей") {
          eduTask(name = "Задача 1") {
            taskFile("src/task.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }

      lesson(name ="История языка") {
        theoryTask(name = "Теоретическая задача") {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }
      }

      additionalFiles {
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section1/lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("CMakeLists.txt")
      }
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  fun `test study course structure with custom names`() {
    val course = course(language = OCLanguage.getInstance()) {
      section(name = "Introduction to C++") {
        lesson(name = "Overview") {
          eduTask(name = "First task") {
            taskFile("src/task.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }

      lesson(name ="Language history") {
        theoryTask(name = "Theory task") {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }
      }

      additionalFiles {
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section1/lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("CMakeLists.txt")
      }
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  fun `test study course structure with eduTask and theoryTask`() {
    val course = course(language = OCLanguage.getInstance()) {
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
    createCourseStructure(course)

    fileTree {
      dir("section1/lesson1") {
        dir("task1") {
          dir("src") {
            file("task.cpp")
          }
          file("CMakeLists.txt")
        }

        dir("task2") {
          dir("src") {
            file("task.cpp")
          }
          file("CMakeLists.txt")
        }
      }

      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("task.cpp")
          }
          file("CMakeLists.txt")
        }

        dir("task2") {
          dir("src") {
            file("task.cpp")
          }
          file("CMakeLists.txt")
        }
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }
}
