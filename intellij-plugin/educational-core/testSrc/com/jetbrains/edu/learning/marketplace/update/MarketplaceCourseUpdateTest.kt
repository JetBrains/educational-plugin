package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.CourseUpdateTestBase
import com.jetbrains.edu.learning.update.CourseUpdater
import org.junit.Test

class MarketplaceCourseUpdateTest : CourseUpdateTestBase<EduCourse>() {
  override fun getUpdater(course: Course): CourseUpdater = MarketplaceCourseUpdaterNew(project, course)

  @Test
  fun `test nothing to update`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse { }
    updateCourse(remoteCourse, isShouldBeUpdated = false)

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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test lesson added`() {
    initiateLocalCourse()

    val newTask = EduTask("task2").apply {
      id = 2
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests.kt" to TaskFile("test/Tests.kt", "fun test3() {}")
      )
    }
    val newLesson = Lesson().apply {
      id = 2
      name = "lesson2"
      addTask(newTask)
    }
    val remoteCourse = toRemoteCourse {
      sections.first().addLesson(newLesson)
    }
    updateCourse(remoteCourse)
    assertEquals("Lesson hasn't been added", 2, localCourse.sections.first().lessons.size)

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
        }
        dir("lesson2") {
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
  fun `test lesson deleted`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      val section = sections.first()
      section.removeLesson(section.lessons.first())
    }
    updateCourse(remoteCourse)
    assertEquals("Lesson hasn't been deleted", 0, localCourse.sections.first().lessons.size)

    val expectedStructure = fileTree {
      dir("section1")
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test lesson updated`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      sections.first().lessons.first().name = "Updated Lesson Name"
    }
    updateCourse(remoteCourse)
    assertEquals("Lesson name hasn't been updated", "Updated Lesson Name", localCourse.sections.first().lessons.first().name)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("Updated Lesson Name") {
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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test section added`() {
    initiateLocalCourse()

    val newTask = EduTask("task2").apply {
      id = 2
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests.kt" to TaskFile("test/Tests.kt", "fun test3() {}")
      )
    }
    val newLesson = Lesson().apply {
      id = 2
      name = "lesson2"
      addTask(newTask)
    }
    val newSection = Section().apply {
      id = 2
      name = "section2"
      addLesson(newLesson)
    }
    val remoteCourse = toRemoteCourse {
      addSection(newSection)
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
        }
      }
      dir("section2") {
        dir("lesson2") {
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
  fun `test section deleted`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      removeSection(sections.first())
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
  fun `test section updated`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      sections.first().name = "Updated Section Name"
    }
    updateCourse(remoteCourse)
    assertEquals("Section name hasn't been updated", "Updated Section Name", localCourse.sections.first().name)

    val expectedStructure = fileTree {
      dir("Updated Section Name") {
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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test task added`() {
    initiateLocalCourse()

    val newTask = EduTask("task2").apply {
      id = 2
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun bar() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests.kt" to TaskFile("test/Tests.kt", "fun test3() {}")
      )
    }
    val remoteCourse = toRemoteCourse {
      sections.first().lessons.first().addTask(newTask)
    }
    updateCourse(remoteCourse)
    assertEquals("Task hasn't been added", 2, localCourse.sections.first().lessons.first().taskList.size)

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
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test course name updated`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      name = "Updated Course Name"
    }
    updateCourse(remoteCourse)
    assertEquals("Course name hasn't been updated", "Updated Course Name", localCourse.name)
  }

  @Test
  fun `test course version updated`() {
    initiateLocalCourse()

    localCourse.marketplaceCourseVersion = 1
    val remoteCourse = toRemoteCourse {
      marketplaceCourseVersion = 2
    }
    updateCourse(remoteCourse)
    assertEquals("Course version hasn't been updated", 2, localCourse.marketplaceCourseVersion)
  }

  @Test
  fun `test additional file added`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      additionalFiles += TaskFile("newFile.txt", "New file content")
      additionalFiles = additionalFiles.map { EduFile(it.name, it.contents) }
    }
    updateCourse(remoteCourse)
    assertEquals("Additional file hasn't been added", 3, localCourse.additionalFiles.size)

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
        }
      }
      file("build.gradle")
      file("settings.gradle")
      file("newFile.txt", "New file content")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test additional file deleted`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      additionalFiles = emptyList()
    }
    updateCourse(remoteCourse)
    assertTrue("Additional file hasn't been deleted", localCourse.additionalFiles.isEmpty())

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
        }
      }
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test additional file updated`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      additionalFiles = listOf(
        TaskFile("build.gradle", "updated content"),
        TaskFile("settings.gradle", "")
      ).map { EduFile(it.name, it.contents) }
    }
    updateCourse(remoteCourse)
    assertEquals("Additional file hasn't been updated", "updated content", localCourse.additionalFiles[0].contents.textualRepresentation)

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
        }
      }
      file("build.gradle", "updated content")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test additional file not updated`() {
    initiateLocalCourse()

    val buildGradleText = localCourse.additionalFiles[0].contents.textualRepresentation
    val remoteCourse = toRemoteCourse {
      additionalFiles = listOf(
        TaskFile("settings.gradle", ""),
        TaskFile("build.gradle", buildGradleText),
      ).map { EduFile(it.name, it.contents) }
    }
    updateCourse(remoteCourse, false)
    assertEquals("Additional file has been updated", buildGradleText, localCourse.additionalFiles[0].contents.textualRepresentation)

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
        }
      }
      file("build.gradle", buildGradleText)
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  override fun initiateLocalCourse() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      section("section1") {
        lesson("lesson1") {
          eduTask("task1") {
            taskFile("src/Task.kt")
            taskFile("src/Baz.kt")
            taskFile("test/Tests.kt")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
      additionalFile("settings.gradle")
    } as EduCourse
  }
}
