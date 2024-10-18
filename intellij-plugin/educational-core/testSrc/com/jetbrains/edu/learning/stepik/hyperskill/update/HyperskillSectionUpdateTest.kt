package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.SectionBuilder
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.UpdateTestBase
import org.junit.Test

class HyperskillSectionUpdateTest : UpdateTestBase<HyperskillCourse>() {
  override fun getUpdater(localCourse: HyperskillCourse) = HyperskillCourseUpdaterNew(project, localCourse)

  @Test
  fun `test new section created`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse { }
    CourseBuilder(remoteCourse).section("section2", id = 2) {
      lesson("lesson2", id = 2) {
        eduTask("task3", stepId = 2) {
          taskFile("src/Task.kt")
          taskFile("src/Baz.kt")
          taskFile("test/Tests.kt")
        }
      }
    }

    updateCourse(remoteCourse)

    assertEquals("Section hasn't been added", 2, localCourse.sections.size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
      }
      dir("section2") {
        dir("lesson2") {
          dir("task3") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new section created in the middle of the course`() {
    localCourse = createBasicHyperskillCourse {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1)
        }
      }
      section("section2", id = 2) {
        lesson("lesson2", id = 2) {
          eduTask("task2", stepId = 2)
        }
      }
    }

    val remoteCourse = toRemoteCourse { }
    CourseBuilder(remoteCourse).section("section3", id = 3, index = 2) {
      lesson("lesson3", id = 3) {
        eduTask("task3", stepId = 3)
      }
    }
    remoteCourse.apply {
      sections[1].index = 3
      sortItems()
    }

    updateCourse(remoteCourse)

    val sections = localCourse.sections
    assertEquals("Section hasn't been added", 3, sections.size)
    checkIndices(sections)
    sections[0].let { section ->
      assertEquals(1, section.index)
      assertEquals("section1", section.name)
      assertEquals("section1", section.presentableName)
      assertEquals("lesson1", section.lessons[0].name)
    }
    sections[1].let { section ->
      assertEquals(2, section.index)
      assertEquals("section3", section.name)
      assertEquals("section3", section.presentableName)
      assertEquals("lesson3", section.lessons[0].name)
    }
    sections[2].let { section ->
      assertEquals(3, section.index)
      assertEquals("section2", section.name)
      assertEquals("section2", section.presentableName)
      assertEquals("lesson2", section.lessons[0].name)
    }

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            file("task.html")
          }
        }
      }
      dir("section3") {
        dir("lesson3") {
          dir("task3") {
            file("task.html")
          }
        }
      }
      dir("section2") {
        dir("lesson2") {
          dir("task2") {
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test section deleted`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      removeSection(sections[0])
    }

    updateCourse(remoteCourse)

    assertEquals("Section hasn't been deleted", 0, localCourse.sections.size)

    val expectedStructure = fileTree {
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test section name updated`() {
    initiateLocalCourse()

    val newSectionName = "updated_section"
    val remoteCourse = toRemoteCourse {
      sections[0].name = newSectionName
    }

    updateCourse(remoteCourse)

    assertEquals("Section name hasn't been updated", newSectionName, localCourse.sections[0].name)

    val expectedStructure = fileTree {
      dir(newSectionName) {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test section with lessons and tasks updated`() {
    initiateLocalCourse()

    val updatedSection = "updated_section"
    val updatedLesson = "updated_lesson"
    val updatedTask = "updated_task"
    val remoteCourse = toRemoteCourse {
      sections[0].apply {
        name = updatedSection
        lessons[0].apply {
          name = updatedLesson
          taskList[0].name = updatedTask
        }
      }
    }

    updateCourse(remoteCourse)

    localCourse.sections[0].let { section ->
      assertEquals("Section hasn't been renamed", updatedSection, section.name)
      assertEquals("Lesson hasn't been renamed", updatedLesson, section.lessons[0].name)
      assertEquals("Task hasn't been renamed", updatedTask, section.lessons[0].taskList[0].name)
    }

    val expectedStructure = fileTree {
      dir(updatedSection) {
        dir(updatedLesson) {
          dir(updatedTask) {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test section lesson and tasks added`() {
    initiateLocalCourse()

    val newLessonName = "new_lesson"
    val newTaskName = "new_task"
    val remoteCourse = toRemoteCourse { }
    SectionBuilder(remoteCourse, remoteCourse.sections[0]).lesson(newLessonName, id = 2) {
      eduTask(newTaskName, stepId = 3) {
        taskFile("src/Task.kt")
        taskFile("src/Baz.kt")
        taskFile("test/Tests.kt")
      }
    }

    updateCourse(remoteCourse)

    localCourse.sections[0].let { section ->
      assertEquals(2, section.lessons.size)
      assertEquals(newLessonName, section.lessons[1].name)
      assertEquals(1, section.lessons[1].taskList.size)
      assertEquals(newTaskName, section.lessons[1].taskList[0].name)
    }

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
        dir(newLessonName) {
          dir(newTaskName) {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test sections and lessons swapped and renamed`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse { }
    CourseBuilder(remoteCourse).section("section1", id = 2, index = 1) {
      lesson("lesson1", id = 2) {
        eduTask("task3", stepId = 3) {
          taskFile("src/Task.kt")
          taskFile("src/Baz.kt")
          taskFile("test/Tests.kt")
        }
      }
    }
    remoteCourse.apply {
      sections[0].apply {
        index = 2
        name = "section2"
        lessons[0].name = "lesson2"
      }
      sortItems()
    }

    updateCourse(remoteCourse)

    assertEquals(2, localCourse.sections.size)
    localCourse.sections[0].let { section ->
      assertEquals(2, section.id)
      assertEquals(1, section.index)
      assertEquals("section1", section.name)
      assertEquals("section1", section.presentableName)
      assertEquals("lesson1", section.lessons[0].name)
    }
    localCourse.sections[1].let { section ->
      assertEquals(1, section.id)
      assertEquals(2, section.index)
      assertEquals("section2", section.name)
      assertEquals("section2", section.presentableName)
      assertEquals("lesson2", section.lessons[0].name)
    }

    val expectedStructure = fileTree {
      dir("section2") {
        dir("lesson2") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
      }
      dir("section1") {
        dir("lesson1") {
          dir("task3") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  override fun initiateLocalCourse() {
    localCourse = createBasicHyperskillCourse {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1) {
            taskFile("src/Task.kt")
            taskFile("src/Baz.kt")
            taskFile("test/Tests.kt")
          }
          eduTask("task2", stepId = 2) {
            taskFile("src/Task.kt")
            taskFile("src/Baz.kt")
            taskFile("test/Tests.kt")
          }
        }
      }
    }
  }
}
