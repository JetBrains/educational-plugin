package com.jetbrains.edu.yaml

import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.coursecreator.archive.CourseArchiveTestBase
import com.jetbrains.edu.coursecreator.archive.FailedToProcessEduFileAsTextualError
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class YamlNavigateToConfigEntryForEduFile : CourseArchiveTestBase() {

  @Test
  /**
   * Repeats the scenario of [com.jetbrains.edu.coursecreator.archive.CCCreateCourseArchiveTest.test course archive creation with task file of wrong binarity]
   * but with fewer checks.
    */
  fun `test course archive creation with task file of wrong binarity, notification navigates to correct config entry`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, createYamlConfigs = true) {
      lesson {
        eduTask {
          // JPEG is treated as binary by the platform, and it cannot create a document for it
          taskFile("cat.jpg", InMemoryTextualContents("A cat image"))
        }
      }
    }

    val error = createCourseArchiveWithError<FailedToProcessEduFileAsTextualError>(course)

    val notification = error.notification(project, "test title")
    val action = notification.actions.single {
      // Simple additional check not to get the wrong action in the future
      it.templatePresentation.text == EduCoreBundle.message("action.mark.file.as.binary")
    }

    val context = SimpleDataContext.builder()
      .setParent(DataManager.getInstance().getDataContext(null))
      .add(Notification.KEY, notification)
      .build()

    testAction(action, context)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

    val navigatedFile = kotlin.test.assertNotNull(FileEditorManagerEx.getInstanceEx(project).currentFile)
    assertEquals("task-info.yaml", navigatedFile.name)
    myFixture.openFileInEditor(navigatedFile)
    myFixture.checkResult("""
        |type: edu
        |files:
        |  - name: <caret>cat.jpg
        |    visible: true
        |    is_binary: true
        |
      """.trimMargin()
    )
  }
}