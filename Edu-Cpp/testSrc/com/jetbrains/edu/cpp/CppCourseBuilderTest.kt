package com.jetbrains.edu.cpp

import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.course.StepikCourse

class CppCourseBuilderTest : CourseGenerationTestBase<CppProjectSettings>() {

  override val defaultSettings = CppProjectSettings()

  fun `test create new cc edu course`() {
    val course = course(
      language = OCLanguage.getInstance(),
      courseMode = CCUtils.COURSE_MODE,
      environment = "GoogleTest"
    ) { }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.html")
        file("CMakeLists.txt",
             getExpectedTaskCMakeText(course, defaultSettings, "global-lesson1-task1"))
      }
      dir("cmake") {
        file("utils.cmake")
        file("googletest.cmake")
        file("googletest-download.cmake")
      }
      file("CMakeLists.txt")
      file("run.cpp")
    }.assertEquals(rootDir)
  }

  fun `test study edu course structure with top-level lesson`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      lesson("lesson") {
        eduTask("task") {
          taskFile("src/task.cpp")
          taskFile("test/test.cpp")
          taskFile("CMakeLists.txt")
        }
      }
      additionalFiles {
        taskFile("CMakeLists.txt.in")
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.html")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  fun `test study edu course structure with section`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      section("section") {
        lesson("lesson") {
          eduTask("task") {
            taskFile("src/task.cpp")
            taskFile("test/test.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }
      additionalFiles {
        taskFile("CMakeLists.txt.in")
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section/lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.html")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  fun `test study course structure with top-level section and lesson`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      section("section") {
        lesson("lesson") {
          eduTask("task") {
            taskFile("src/task.cpp")
            taskFile("test/test.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }
      lesson("top_level_lesson") {
        eduTask("task") {
          taskFile("src/task.cpp")
          taskFile("test/test.cpp")
          taskFile("CMakeLists.txt")
        }
      }
      additionalFiles {
        taskFile("CMakeLists.txt.in")
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section/lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.html")
        file("CMakeLists.txt")
      }
      dir("top_level_lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.html")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  fun `test study edu course structure with different tasks`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      lesson("lesson") {
        eduTask("edu") {
          taskFile("src/task.cpp")
          taskFile("test/test.cpp")
          taskFile("CMakeLists.txt")
        }
        outputTask("output") {
          taskFile("src/task.cpp")
          taskFile("output.txt")
          taskFile("CMakeLists.txt")
        }
        theoryTask("theory") {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }
      }

      additionalFiles {
        taskFile("CMakeLists.txt.in")
        taskFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson") {
        dir("edu") {
          dir("src") {
            file("task.cpp")
          }
          dir("test") {
            file("test.cpp")
          }
          file("task.html")
          file("CMakeLists.txt")
        }
        dir("output") {
          dir("src") {
            file("task.cpp")
          }
          file("output.txt")
          file("task.html")
          file("CMakeLists.txt")
        }
        dir("theory") {
          dir("src") {
            file("task.cpp")
          }
          file("task.html")
          file("CMakeLists.txt")
        }
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }


  fun `test study Stepik course structure with Russian names`() {
    val course = course(
      language = OCLanguage.getInstance(),
      courseProducer = ::StepikCourse
    ) {
      section(name = "Введение в язык C++") {
        lesson(name = "Обзор возможностей") {
          eduTask(name = "Задача 1") {
            taskFile("src/task.cpp")
          }
        }
      }

      lesson(name = "История языка") {
        theoryTask(name = "Теоретическая задача") {
          taskFile("src/task.cpp")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section1/lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("task.html")
        file("CMakeLists.txt",
             getExpectedTaskCMakeText(course, defaultSettings, "section1-lesson1-task1"))
      }
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("task.html")
        file("CMakeLists.txt",
             getExpectedTaskCMakeText(course, defaultSettings, "global-lesson2-task1"))
      }
      dir("cmake") {
        file("utils.cmake")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  fun `test study Stepik course structure with custom names`() {
    val course = course(
      language = OCLanguage.getInstance(),
      courseProducer = ::StepikCourse
    ) {
      section(name = "Introduction to C++") {
        lesson(name = "Overview") {
          eduTask(name = "First task") {
            taskFile("src/task.cpp")
          }
        }
      }

      lesson(name = "Language history") {
        theoryTask(name = "Theory task") {
          taskFile("src/task.cpp")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section1/lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("task.html")
        file("CMakeLists.txt",
             getExpectedTaskCMakeText(course, defaultSettings, "section1-lesson1-task1"))
      }
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        file("task.html")
        file("CMakeLists.txt",
             getExpectedTaskCMakeText(course, defaultSettings, "global-lesson2-task1"))
      }
      dir("cmake") {
        file("utils.cmake")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }
}
