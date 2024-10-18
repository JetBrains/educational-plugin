package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.UpdateTestBase
import org.junit.Test

class HyperskillCourseUpdateTest : UpdateTestBase<HyperskillCourse>() {
  override fun getUpdater(localCourse: HyperskillCourse) = HyperskillCourseUpdaterNew(project, localCourse)

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
  fun `test course name updated`() {
    initiateLocalCourse()

    val newCourseName = "Updated Course Name"
    val remoteCourse = toRemoteCourse {
      name = newCourseName
      hyperskillProject!!.title = newCourseName
    }

    updateCourse(remoteCourse)

    assertEquals("Course name hasn't been updated", newCourseName, localCourse.name)
    assertEquals("Hyperskill project title hasn't been updated", newCourseName, localCourse.hyperskillProject!!.title)
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
        TaskFile("build.gradle", buildGradleText),
        TaskFile("settings.gradle", ""),
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
      stages[0].title = updatedStageTitle
    }

    updateCourse(remoteCourse)

    assertEquals("Stage title hasn't been updated", updatedStageTitle, localCourse.stages[0].title)

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
    assertEquals("Hyperskill project ID hasn't been updated", newId, hyperskillProject.id)
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
    localCourse = createBasicHyperskillCourse {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1) {
            taskFile("src/Task.kt")
            taskFile("src/Baz.kt")
            taskFile("test/Tests.kt")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
      additionalFile("settings.gradle")
    }
    localCourse.stages = listOf(HyperskillStage(1, "", 1))
  }
}