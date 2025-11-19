package com.jetbrains.edu.learning

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.fixtures.EditorNotificationFixture
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.stepik.hyperskill.InstallHyperskillPluginEditorNotificationsProvider
import com.jetbrains.edu.learning.stepik.hyperskill.isHyperskillProject
import kotlin.test.Test

class InstallNewHyperskillPluginEditorNotificationTest : CourseReopeningTestBase<EmptyProjectSettings>() {
  private val editorNotificationFixture = EditorNotificationFixture { project }

  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  override fun setUp() {
    super.setUp()
    registerConfigurator<PlainTextConfigurator>(PlainTextLanguage.INSTANCE, courseType = HYPERSKILL)
  }

  @Test
  fun `test editor hyperskill plugin notification is shown for hyperskill course`() {
    val course = course(courseProducer = ::HyperskillCourse) {
      lesson("lesson") {
        eduTask("task") {
          taskFile("task.txt", "text")
        }
      }
    }

    openStudentProjectThenReopenStudentProject(course, {}) { project ->
      assertTrue(project.isHyperskillProject)
      editorNotificationFixture.checkEditorNotification<InstallHyperskillPluginEditorNotificationsProvider>(
        findFile("lesson/task/task.txt"),
        EduCoreBundle.message("hyperskill.new.plugin.editor.notification.install.text")
      )
    }
  }

  @Test
  fun `test no editor hyperskill plugin notification is shown for marketplace course`() {
    val course = course {
      lesson("lesson") {
        eduTask("task") {
          taskFile("task.txt", "text")
        }
      }
    }

    openStudentProjectThenReopenStudentProject(course, {}) { project ->
      assertFalse(project.isHyperskillProject)
      editorNotificationFixture.checkNoEditorNotification<InstallHyperskillPluginEditorNotificationsProvider>(findFile("lesson/task/task.txt"))
    }
  }
}