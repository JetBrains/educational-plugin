package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.CourseUpdateTestBase
import com.jetbrains.edu.learning.update.CourseUpdater
import org.junit.Test

class HyperskillCourseUpdateTest : CourseUpdateTestBase<HyperskillCourse>() {
  override fun getUpdater(course: HyperskillCourse): CourseUpdater<HyperskillCourse> = HyperskillCourseUpdaterNew(project, course)

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
  fun `test lesson added`() {
    initiateLocalCourse()

    val newTask = EduTask("task2").apply {
      id = 2
      descriptionFormat = DescriptionFormat.HTML
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests.kt" to TaskFile("test/Tests.kt", "fun test2() {}")
      )
    }
    val newLesson = Lesson().apply {
      id = 2
      name = "new_lesson"
      addTask(newTask)
    }
    val remoteCourse = toRemoteCourse {
      val section = sections.first()
      section.addLesson(newLesson)
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
            file("task.html")
          }
        }
        dir("new_lesson") {
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
  fun `test section added`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse { }
    CourseBuilder(remoteCourse).section("section2", id = 2) {
      lesson("lesson2", id = 2) {
        eduTask("task2", stepId = 2, taskDescriptionFormat = DescriptionFormat.HTML) {
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
  fun `test task added`() {
    initiateLocalCourse()

    val newTask = EduTask("task2").apply {
      id = 2
      descriptionFormat = DescriptionFormat.HTML
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun bar() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests.kt" to TaskFile("test/Tests.kt", "fun test2() {}")
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
  fun `test course name updated`() {
    initiateLocalCourse()

    val newCourseName = "Updated Course Name"
    val remoteCourse = toRemoteCourse {
      name = newCourseName
      hyperskillProject!!.title = newCourseName
    }
    updateCourse(remoteCourse)
    assertEquals("Course name hasn't been updated", "Updated Course Name", localCourse.name)
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
            file("task.html")
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
            file("task.html")
          }
        }
      }
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test additional file updated`() {
    initiateLocalCourse()

    val updatedBuildGradleText = "updated content"
    val remoteCourse = toRemoteCourse {
      additionalFiles = listOf(
        TaskFile("build.gradle", updatedBuildGradleText),
        TaskFile("settings.gradle", "")
      ).map { EduFile(it.name, it.contents) }
    }
    updateCourse(remoteCourse)
    assertEquals(
      "Additional file hasn't been updated",
      updatedBuildGradleText,
      localCourse.additionalFiles[0].contents.textualRepresentation
    )

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
        }
      }
      file("build.gradle", updatedBuildGradleText)
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
            file("task.html")
          }
        }
      }
      file("build.gradle", buildGradleText)
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test hyperskill stages updated`() {
    initiateLocalCourse()

    val updatedStageTitle = "Updated Stage Title"
    val remoteCourse = toRemoteCourse {
      val updatedStage = stages.first().apply {
        title = updatedStageTitle
      }
      stages = listOf(updatedStage)
    }
    updateCourse(remoteCourse)
    assertEquals("Stage title hasn't been updated", updatedStageTitle, localCourse.stages.first().title)

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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test hyperskill project updated`() {
    initiateLocalCourse()

    val newId = 2
    val newTitle = "Hyperskill Project Updated"
    val newDescription = "Updated project description"
    val remoteCourse = toRemoteCourse {
      hyperskillProject = HyperskillProject().apply {
        id = newId
        title = newTitle
        description = newDescription
      }
    }
    updateCourse(remoteCourse)
    val hyperskillProject = localCourse.hyperskillProject ?: error("Hyperskill project is null")
    assertEquals("Hyperskill project hasn't been updated", newId, hyperskillProject.id)
    assertEquals("Course ID hasn't been updated", newId, localCourse.id)
    assertEquals("Hyperskill project hasn't been updated", newTitle, hyperskillProject.title)
    assertEquals("Course name hasn't been updated", newTitle, localCourse.name)
    assertEquals("Hyperskill project hasn't been updated", newDescription, hyperskillProject.description)

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
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  override fun initiateLocalCourse() {
    val title = "Hyperskill Project"
    localCourse = courseWithFiles(
      id = 1,
      name = title,
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      section("section1") {
        lesson("lesson1") {
          eduTask("task1", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt")
            taskFile("src/Baz.kt")
            taskFile("test/Tests.kt")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
      additionalFile("settings.gradle")
    } as HyperskillCourse
    localCourse.hyperskillProject = HyperskillProject().apply {
      this.title = title
      description = "Project Description"
    }
    localCourse.stages = listOf(HyperskillStage(1, "Stage Title", 1))
  }
}