package com.jetbrains.edu.java

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.stepik.StepikCourseUpdater
import junit.framework.TestCase

class StudentCourseUpdateTest : CourseGenerationTestBase<JdkProjectSettings>() {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = JCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test lesson added`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_added")
  }

  fun `test lesson moved`() {
    val expectedFileTree = fileTree {
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lessons_moved")
  }

  fun `test lesson renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1_renamed") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_renamed")
  }

  fun `test task added`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
        dir("task2") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_added")
  }

  fun `test task in section added`() {
    val expectedFileTree = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java")
            }
            dir("test") {
              file("Test.java")
            }
          }
          dir("task2") {
            dir("src") {
              file("Task.java")
            }
            dir("test") {
              file("Test.java")
            }
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_in_section_added")
  }


  fun `test task renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1_renamed") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_renamed")
  }

  fun `test task text changed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_text_changed")
  }

  fun `test task file renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task_renamed.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_file_renamed")
  }

  fun `test task file text changed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java",  "class Task {\n  //Changed put your task here\n}")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_file_text_changed")
  }

  fun `test task file in section text changed`() {
    val expectedFileTree = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java", "class Task {\n  //Changed put your task here\n}")
            }
            dir("test") {
              file("Test.java")
            }
          }
        }
        file("build.gradle")
        file("settings.gradle")
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_file_in_section_text_changed")
  }


  fun `test section added`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java",  "class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java",  "class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Test.java")
            }
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/section_added")
  }

  fun `test lesson added into section`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java",  "class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java",  "class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Test.java")
            }
          }
        }
        dir("lesson2") {
          dir("task1") {
            dir("src") {
              file("Task.java",  "class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Test.java")
            }
          }
        }

      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_added_into_section")
  }

  fun `test section renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java",  "class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      dir("section1_renamed") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java",  "class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Test.java")
            }
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/section_renamed")
  }

  fun `test lesson in section renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java",  "class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      dir("section1") {
        dir("lesson1_renamed") {
          dir("task1") {
            dir("src") {
              file("Task.java",  "class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Test.java")
            }
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_in_section_renamed")
  }

  private fun doTest(expectedFileTree: FileTree, testPath: String) {
    val course = createRemoteCourseFromJson("$testPath/course.json", CourseType.STUDENT)
    createCourseStructure(courseBuilder, course, defaultSettings)
    val courseFromServer = createRemoteCourseFromJson("$testPath/updated_course.json", CourseType.STUDENT)

    StepikCourseUpdater(course, project).doUpdate(courseFromServer)
    TestCase.assertEquals("Lessons number mismatch. Expected: ${courseFromServer.lessons.size}. Actual: ${course.lessons.size}",
                          courseFromServer.lessons.size, course.lessons.size)
    for ((lesson, newLesson) in course.lessons.zip(courseFromServer.lessons)) {
      TestCase.assertTrue("Tasks number mismatch.\n" +
                          "Lesson \"${lesson.name}\". \n" +
                          "Expected task number: ${newLesson.taskList.size}. Actual: ${lesson.taskList.size}",
                          lesson.taskList.size == newLesson.taskList.size)

      TestCase.assertTrue("Lesson name mismatch. Expected: ${newLesson.name}. Actual: ${lesson.name}", lesson.name == newLesson.name)
      for ((task, newTask) in lesson.taskList.zip(newLesson.taskList)) {
        TestCase.assertTrue("Task files number mismatch.\n" +
                            "Lesson \"${lesson.name}\". \n" +
                            "Task \"${task.name}\". \n" +
                            "Expected task files number: ${newTask.taskFiles.size}. Actual: ${task.taskFiles.size}",
                            task.taskFiles.size == newTask.taskFiles.size)
        TestCase.assertTrue("Test files number mismatch.\n" +
                            "Lesson \"${lesson.name}\". \n" +
                            "Task \"${task.name}\". \n" +
                            "Expected test files number: ${newTask.testsText.size}. Actual: ${task.testsText.size}",
                            task.testsText.size == newTask.testsText.size)


        TestCase.assertTrue("Task text mismatch.\n" +
                            "Lesson \"${lesson.name}\". \n" +
                            "Task \"${task.name}\". \n" +
                            "Expected:\n \"${newTask.description}\"\n" +
                            "Actual:\n \"${task.description}\"",
                            newTask.description == task.description)

        TestCase.assertTrue("Lesson index mismatch.\n Expected: Lesson \"${newLesson.name}\", index: ${newLesson.index}.\n" +
                            " Actual: Lesson \"${lesson.name}\", index: ${lesson.index}", lesson.index == newLesson.index)

      }
      expectedFileTree.assertEquals(rootDir)
    }
  }
}