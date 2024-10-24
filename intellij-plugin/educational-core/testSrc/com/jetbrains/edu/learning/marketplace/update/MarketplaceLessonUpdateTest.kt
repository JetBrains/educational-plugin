package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.SectionBuilder
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.update.LessonUpdateTestBase
import org.junit.Test

class MarketplaceLessonUpdateTest : LessonUpdateTestBase<EduCourse>() {
  override fun getUpdater(localCourse: EduCourse): CourseUpdater<EduCourse> = MarketplaceCourseUpdaterNew(project, localCourse)

  @Test
  fun `test new lesson in course created`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse { }
    CourseBuilder(remoteCourse).lesson("lesson2") {
      eduTask("task3", stepId = 3, taskDescriptionFormat = DescriptionFormat.HTML) {
        taskFile("src/Task.kt")
        taskFile("src/Baz.kt")
        taskFile("test/Tests.kt")
      }
    }

    updateCourse(remoteCourse)

    assertEquals("Lesson hasn't been added", 2, localCourse.lessons.size)

    val expectedStructure = fileTree {
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
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new lesson created in the middle of the course`() {
    localCourse = createBasicMarketplaceCourse {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1)
      }
      lesson("lesson2", id = 2) {
        eduTask("task2", stepId = 2)
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }

    val remoteCourse = toRemoteCourse { }
    CourseBuilder(remoteCourse).lesson("lesson3", id = 3, index = 2) {
      eduTask("task3", stepId = 3)
    }
    remoteCourse.apply {
      lessons[1].index = 3
      sortItems()
    }

    updateCourse(remoteCourse)

    val lessons = localCourse.lessons
    assertEquals("Lesson hasn't been added", 3, lessons.size)
    checkIndices(lessons)
    lessons[0].let { lesson ->
      assertEquals(1, lesson.id)
      assertEquals(1, lesson.index)
      assertEquals("lesson1", lesson.name)
    }
    lessons[1].let { lesson ->
      assertEquals(3, lesson.id)
      assertEquals(2, lesson.index)
      assertEquals("lesson3", lesson.name)
    }
    lessons[2].let { lesson ->
      assertEquals(2, lesson.id)
      assertEquals(3, lesson.index)
      assertEquals("lesson2", lesson.name)
    }

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("task.md")
        }
      }
      dir("lesson3") {
        dir("task3") {
          file("task.md")
        }
      }
      dir("lesson2") {
        dir("task2") {
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
    
  }

  @Test
  fun `test new lesson in section created`() {
    localCourse = createBasicMarketplaceCourse {
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

    val remoteCourse = toRemoteCourse { }
    SectionBuilder(remoteCourse, remoteCourse.sections[0]).lesson("lesson2", id = 2) {
      eduTask("task3", stepId = 3) {
        taskFile("src/Task.kt")
        taskFile("src/Baz.kt")
        taskFile("test/Tests.kt")
      }
    }

    updateCourse(remoteCourse)

    assertEquals("Lesson hasn't been added", 2, localCourse.sections[0].lessons.size)

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
  fun `test new lesson created in the middle of the section`() {
    localCourse = createBasicMarketplaceCourse {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1)
        }
        lesson("lesson2", id = 2) {
          eduTask("task2", stepId = 2)
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }

    val remoteCourse = toRemoteCourse { }
    SectionBuilder(remoteCourse, remoteCourse.sections[0]).lesson("lesson3", id = 3, index = 2) {
      eduTask("task3", stepId = 3)
    }
    remoteCourse.apply { 
      sections[0].lessons[1].index = 3
      sortItems()
    }
    updateCourse(remoteCourse)

    val lessons = localCourse.sections[0].lessons
    assertEquals("Lesson hasn't been added", 3, lessons.size)
    checkIndices(lessons)
    lessons[0].let { lesson ->
      assertEquals(1, lesson.id)
      assertEquals(1, lesson.index)
      assertEquals("lesson1", lesson.name)
    }
    lessons[1].let { lesson ->
      assertEquals(3, lesson.id)
      assertEquals(2, lesson.index)
      assertEquals("lesson3", lesson.name)
    }
    lessons[2].let { lesson ->
      assertEquals(2, lesson.id)
      assertEquals(3, lesson.index)
      assertEquals("lesson2", lesson.name)
    }

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            file("task.md")
          }
        }
        dir("lesson3") {
          dir("task3") {
            file("task.md")
          }
        }
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
  fun `test lesson in course has been deleted`() {
    localCourse = createBasicMarketplaceCourse {
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
      lesson("lesson2", id = 2) {
        eduTask("task3", stepId = 3) {
          taskFile("src/Task.kt")
          taskFile("src/Baz.kt")
          taskFile("test/Tests.kt")
        }
        eduTask("task4", stepId = 4) {
          taskFile("src/Task.kt")
          taskFile("src/Baz.kt")
          taskFile("test/Tests.kt")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }

    val remoteCourse = toRemoteCourse {
      removeLesson(lessons[0])
    }

    updateCourse(remoteCourse)

    assertEquals("Lesson hasn't been deleted", 1, localCourse.lessons.size)

    val expectedStructure = fileTree {
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
        dir("task4") {
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
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test lesson in section has been deleted`() {
    localCourse = createBasicMarketplaceCourse {
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
        lesson("lesson2", id = 2) {
          eduTask("task3", stepId = 3) {
            taskFile("src/Task.kt")
            taskFile("src/Baz.kt")
            taskFile("test/Tests.kt")
          }
          eduTask("task4", stepId = 4) {
            taskFile("src/Task.kt")
            taskFile("src/Baz.kt")
            taskFile("test/Tests.kt")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }

    val remoteCourse = toRemoteCourse {
      val section = sections[0]
      section.removeLesson(section.lessons[0])
    }

    updateCourse(remoteCourse)

    assertEquals("Lesson hasn't been deleted", 1, localCourse.sections[0].lessons.size)

    val expectedStructure = fileTree {
      dir("section1") {
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
          dir("task4") {
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
  fun `test new lesson in section created and existing task has been changed`() {
    localCourse = createBasicMarketplaceCourse {
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
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }

    val updatedNameForTask1 = "task1 updated"
    val remoteCourse = toRemoteCourse {}
    SectionBuilder(remoteCourse, remoteCourse.sections[0]).apply {
      lesson("lesson2", id = 2, index = 2) {
        eduTask("task3", stepId = 3) {
          taskFile("src/Task.kt")
          taskFile("src/Baz.kt")
          taskFile("test/Tests.kt")
        }
      }
    }
    remoteCourse.sections[0].lessons[0].taskList[0].name = updatedNameForTask1

    updateCourse(remoteCourse)

    val actualTaskName = localCourse.sections[0].lessons[0].taskList[0].name
    assertEquals("Task name not updated", updatedNameForTask1, actualTaskName)
    assertEquals("Lesson hasn't been added", 2, localCourse.sections[0].lessons.size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir(updatedNameForTask1) {
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

  override fun initiateLocalCourse() {
    localCourse = createBasicMarketplaceCourse {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt")
          taskFile("src/Baz.kt")
          taskFile("test/Tests.kt")
        }
        eduTask("task2", stepId = 2, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt")
          taskFile("src/Baz.kt")
          taskFile("test/Tests.kt")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }
  }
}