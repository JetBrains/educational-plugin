package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.application.runWriteAction
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.testFramework.utils.vfs.deleteRecursively
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.testAction
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
      file("Task.kt", "fun task() {solution2}")
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

  @Test
  fun `update two framework lessons at once`() {
    // clear the course created in `before()`, because we need to create another course with two lessons
    runInEdtAndWait {
      runWriteAction {
        project.courseDir.deleteRecursively("lesson1")
      }
    }

    val eduCourse = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = { produceCourse() },
      createYamlConfigs = true,
      id = 1234 // to ensure that course-remote-info.yaml is created
    ) {
      frameworkLesson("lesson1", isTemplateBased = false, id = 4321) {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("Task.kt", "fun task() {}", visible = true, editable = true)
          taskFile("NonEdit.kt", "val p = 41", visible = true, editable = false)
          taskFile("Tests1.kt", "fun test1() {}", visible = false, editable = false)
        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("Task.kt", "fun task() {solution2}", visible = true, editable = true)
          taskFile("NonEdit.kt", "val p = 42", visible = true, editable = false)
          taskFile("Tests2.kt", "fun test2() {}", visible = false, editable = false)
        }
        eduTask("task3", stepId = 3, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("Task.kt", "fun task() {solution3}", visible = true, editable = true)
          taskFile("NonEdit.kt", "val p = 43", visible = true, editable = false)
          taskFile("Tests3.kt", "fun test3() {}", visible = false, editable = false)
        }
      }
      frameworkLesson("lesson2", isTemplateBased = false, id = 4322) {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("Task.kt", "fun task() {}", visible = true, editable = true)
          taskFile("NonEdit.kt", "val p = 41", visible = true, editable = false)
          taskFile("Tests1.kt", "fun test1() {}", visible = false, editable = false)
        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("Task.kt", "fun task() {solution2}", visible = true, editable = true)
          taskFile("NonEdit.kt", "val p = 42", visible = true, editable = false)
          taskFile("Tests2.kt", "fun test2() {}", visible = false, editable = false)
        }
        eduTask("task3", stepId = 3, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("Task.kt", "fun task() {solution3}", visible = true, editable = true)
          taskFile("NonEdit.kt", "val p = 43", visible = true, editable = false)
          taskFile("Tests3.kt", "fun test3() {}", visible = false, editable = false)
        }
      }
    } as EduCourse
    setupLocalCourse(eduCourse)
    localCourse = eduCourse

    next()
    next() // go to task 3 in lesson1

    // go to task 3 in lesson2
    runInEdtAndWait {
      testAction(NextTaskAction.ACTION_ID)
      val lesson2 = localCourse.getLesson("lesson2")!! as FrameworkLesson
      withVirtualFileListener(localCourse) {
        lesson2.currentTask()?.openTaskFileInEditor("Task.kt")
        testAction(NextTaskAction.ACTION_ID)
        testAction(NextTaskAction.ACTION_ID)
      }
    }

    updateCourse {
      lessons[0].removeTask(lessons[0].taskList[2])
      lessons[1].removeTask(lessons[1].taskList[2])
    }

    runInEdtAndWait {
      fileTree {
        dir("lesson1") {
          dir("task") {
            // second task is chosen, because the third one was deleted
            file("Task.kt")
            file("Tests2.kt")
            file("NonEdit.kt")
          }
          dir("task1") {
            file("task.html")
            file("task-info.yaml")
            file("task-remote-info.yaml")
          }
          dir("task2") {
            file("task.html")
            file("task-info.yaml")
            file("task-remote-info.yaml")
          }
          file("lesson-info.yaml")
          file("lesson-remote-info.yaml")
        }
        dir("lesson2") {
          dir("task") {
            // second task is chosen, because the third one was deleted
            file("Task.kt")
            file("Tests2.kt")
            file("NonEdit.kt")
          }
          dir("task1") {
            file("task.html")
            file("task-info.yaml")
            file("task-remote-info.yaml")
          }
          dir("task2") {
            file("task.html")
            file("task-info.yaml")
            file("task-remote-info.yaml")
          }
          file("lesson-info.yaml")
          file("lesson-remote-info.yaml")
        }
        file("build.gradle")
        file("settings.gradle")
        file("course-info.yaml")
        file("course-remote-info.yaml")
      }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    }
  }

  override fun produceCourse(): EduCourse = EduCourse()

  override fun setupLocalCourse(course: EduCourse) {
    course.marketplaceCourseVersion = 1
  }

  override fun getUpdater(localCourse: EduCourse): CourseUpdater<EduCourse> = MarketplaceCourseUpdaterNew(project, localCourse)
}