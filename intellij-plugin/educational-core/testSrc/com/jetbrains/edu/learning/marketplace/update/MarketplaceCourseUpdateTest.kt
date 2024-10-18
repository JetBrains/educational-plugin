package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.update.UpdateTestBase
import org.junit.Test

class MarketplaceCourseUpdateTest : UpdateTestBase<EduCourse>() {
  override fun getUpdater(localCourse: EduCourse): CourseUpdater<EduCourse> = MarketplaceCourseUpdaterNew(project, localCourse)

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
  fun `test course name updated`() {
    initiateLocalCourse()

    val updateCourseName = "Updated Course Name"
    val remoteCourse = toRemoteCourse {
      name = updateCourseName
    }
    updateCourse(remoteCourse)
    assertEquals("Course name hasn't been updated", updateCourseName, localCourse.name)
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
    localCourse = createBasicMarketplaceCourse {
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
    }
  }
}
