package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.SectionBuilder
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.update.UpdateTestBase
import org.junit.Test

class MarketplaceSectionUpdateTest : UpdateTestBase<EduCourse>() {
  override fun getUpdater(localCourse: EduCourse): CourseUpdater<EduCourse> = MarketplaceCourseUpdaterNew(project, localCourse)

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
            file("task.md")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.md")
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
            file("task.md")
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
    localCourse = createBasicMarketplaceCourse {
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
      additionalFile("build.gradle", "apply plugin: \"java\"")
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
    assertEquals(3, sections.size)
    checkIndices(sections)
    localCourse.sections[0].let { section ->
      assertEquals(1, section.id)
      assertEquals(1, section.index)
      assertEquals("section1", section.name)
      assertEquals("section1", section.presentableName)
      assertEquals("lesson1", section.lessons[0].name)
    }
    localCourse.sections[1].let { section ->
      assertEquals(3, section.id)
      assertEquals(2, section.index)
      assertEquals("section3", section.name)
      assertEquals("section3", section.presentableName)
      assertEquals("lesson3", section.lessons[0].name)
    }
    localCourse.sections[2].let { section ->
      assertEquals(2, section.id)
      assertEquals(3, section.index)
      assertEquals("section2", section.name)
      assertEquals("section2", section.presentableName)
      assertEquals("lesson2", section.lessons[0].name)
    }

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            file("task.md")
          }
        }
      }
      dir("section3") {
        dir("lesson3") {
          dir("task3") {
            file("task.md")
          }
        }
      }
      dir("section2") {
        dir("lesson2") {
          dir("task2") {
            file("task.md")
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
      sections.forEach { removeSection(it) }
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

    val updatedSectionName = "updated_section"
    val remoteCourse = toRemoteCourse {
      sections[0].name = updatedSectionName
    }

    updateCourse(remoteCourse)

    assertEquals("Section name hasn't been updated", updatedSectionName, localCourse.sections[0].name)

    val expectedStructure = fileTree {
      dir(updatedSectionName) {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.md")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.md")
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

    val updatedSectionName = "updated_section"
    val updatedLessonName = "updated_lesson"
    val updatedTaskName = "updated_task"
    val remoteCourse = toRemoteCourse {
      sections[0].apply {
        name = updatedSectionName
        lessons[0].apply {
          name = updatedLessonName
          taskList[0].name = updatedTaskName
        }
      }
    }

    updateCourse(remoteCourse)

    val sections = localCourse.sections[0]
    assertEquals(updatedSectionName,sections.name)
    assertEquals(updatedLessonName, sections.lessons[0].name)
    assertEquals(updatedTaskName, sections.lessons[0].taskList[0].name)

    val expectedStructure = fileTree {
      dir(updatedSectionName) {
        dir(updatedLessonName) {
          dir(updatedTaskName) {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.md")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.md")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test section lessons and tasks added`() {
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

    assertEquals(2, localCourse.sections[0].lessons.size)
    assertEquals(newLessonName, localCourse.sections[0].lessons[1].name)
    assertEquals(newTaskName, localCourse.sections[0].lessons[1].taskList[0].name)

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
            file("task.md")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.md")
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
            file("task.md")
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
            file("task.md")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
            file("task.md")
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
            file("task.md")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  override fun initiateLocalCourse() {
    localCourse = createBasicMarketplaceCourse {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests.kt", "fun test1() {}")
          }
          eduTask("task2", stepId = 2) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests.kt", "fun test2() {}")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }
  }
}
