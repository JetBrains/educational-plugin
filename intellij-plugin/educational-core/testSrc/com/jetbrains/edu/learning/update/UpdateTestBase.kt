package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.actions.navigate.NavigationTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import kotlinx.coroutines.runBlocking

abstract class UpdateTestBase<T : Course> : NavigationTestBase() {
  protected lateinit var localCourse: T

  abstract fun getUpdater(localCourse: T): CourseUpdater<T>

  abstract fun initiateLocalCourse()

  override fun runInDispatchThread(): Boolean = false

  protected fun updateCourse(remoteCourse: T, isShouldBeUpdated: Boolean = true) {
    val updater = getUpdater(localCourse)
    val updates = runBlocking {
      updater.collect(remoteCourse)
    }
    assertEquals("Updates are" + (if (isShouldBeUpdated) " not" else "") + " available", isShouldBeUpdated, updates.isNotEmpty())
    val isUpdateSucceed = runBlocking {
      try {
        updater.update(remoteCourse)
        true
      }
      catch (e: Exception) {
        LOG.error(e)
        false
      }
    }
    if (isShouldBeUpdated) {
      assertTrue("Update failed", isUpdateSucceed)
    }
  }

  protected fun toRemoteCourse(changeCourse: T.() -> Unit): T =
    localCourse.copy().apply {
      additionalFiles = localCourse.additionalFiles
      copyFileContents(localCourse, this)
      changeCourse()
      init(false)
    }

  protected fun createBasicHyperskillCourse(buildCourse: (CourseBuilder.() -> Unit)? = null): HyperskillCourse {
    val title = "Hyperskill Project"
    val courseBuilder = buildCourse ?: {
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
      additionalFile("build.gradle", "apply plugin: \"java\"")
      additionalFile("settings.gradle")
    }
    val course = courseWithFiles(id = 1, name = title, language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      courseBuilder()
    } as HyperskillCourse
    course.marketplaceCourseVersion = 1
    course.hyperskillProject = HyperskillProject().apply {
      this.title = title
      description = "Project Description"
    }
    course.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2))
    return course
  }

  protected fun createBasicMarketplaceCourse(buildCourse: (CourseBuilder.() -> Unit)? = null): EduCourse {
    val courseBuilder = buildCourse ?: {
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
    val course = courseWithFiles(id = 1, name = "Marketplace Course", language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      courseBuilder()
    } as EduCourse
    course.marketplaceCourseVersion = 1
    return course
  }

  protected fun checkIndices(items: List<StudyItem>) {
    val distinctIndices = items.map { it.index }.distinct().sorted()
    val expectedIndices = List(items.size) { it + 1 }
    assertEquals("Indices are incorrect", expectedIndices, distinctIndices)
  }
}