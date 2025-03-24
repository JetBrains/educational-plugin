package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.update.FrameworkLessonsUpdateTest
import org.junit.Test

class MarketplaceFrameworkLessonsUpdateTest : FrameworkLessonsUpdateTest<EduCourse>() {

  @Test
  fun `test do not update when tasks ids changed`() {
    val oldDescriptionTextTask1 = localCourse.task1.descriptionText
    val oldDescriptionTextTask2 = localCourse.task2.descriptionText

    updateCourse(isShouldBeUpdated = false) {
      task1.apply {
        id = 101
        descriptionText = "New Description"
        descriptionFormat = DescriptionFormat.MD
      }
      task2.apply {
        id = 111
        descriptionText = "New Description"
        descriptionFormat = DescriptionFormat.MD
      }
    }

    with(localCourse) {
      assertEquals(oldDescriptionTextTask1, task1.descriptionText)
      assertEquals(oldDescriptionTextTask2, task1.descriptionText)
      assertEquals(DescriptionFormat.HTML, task2.descriptionFormat)
      assertEquals(DescriptionFormat.HTML, task2.descriptionFormat)
    }
  }

  @Test
  fun `update unmodified previous task for template based FL`() {
    frameworkLesson.isTemplateBased = true
    next()

    updateCourse {
      task1.mainFile.textContents = "fun taskMod1() {}"
      task1.nonEditFile.textContents = "val p = 141"
    }

    assertTaskFolder {
      file("Task.kt", "fun task() {}")
      file("NonEdit.kt", "val p = 42")
      file("Tests2.kt", "fun test2() {}")
    }
    previous()
    assertTaskFolder {
      file("Task.kt", "fun taskMod1() {}") // we see updated code because the task was not modified
      file("NonEdit.kt", "val p = 141") // new version of the file
      file("Tests1.kt", "fun test1() {}")
    }
  }

  @Test
  fun `update unmodified current task if it is second for template based FL`() {
    frameworkLesson.isTemplateBased = true
    next()
    updateCourse {
      task2.mainFile.textContents = "fun taskMod1() {}"

      task2.nonEditFile.textContents = "val p = 141"
      // make the file propagatable
      task2.nonEditFile.isEditable = true
      task3.nonEditFile.isEditable = true
    }

    assertTaskFolder {
      file("Task.kt", "fun taskMod1() {}") // previous is not propagated
      file("NonEdit.kt", "val p = 141") // the file became propagatable after the update, but the task is not modified, so we see the new text
      file("Tests2.kt", "fun test2() {}")
    }
  }

  override fun produceCourse(): EduCourse = EduCourse()

  override fun setupLocalCourse(course: EduCourse) {
    course.marketplaceCourseVersion = 1
  }

  override fun getUpdater(localCourse: EduCourse): CourseUpdater<EduCourse> = MarketplaceCourseUpdaterNew(project, localCourse)
}