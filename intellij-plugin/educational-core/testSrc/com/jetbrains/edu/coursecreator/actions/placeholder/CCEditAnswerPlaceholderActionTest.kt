package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.learning.yaml.getConfigDir
import org.junit.Test

class CCEditAnswerPlaceholderActionTest : EduActionTestCase() {
  @Test
  fun `test navigate to yaml`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>")
        }
        eduTask("task2") {
          taskFile("Task.kt", "<p>fun foo()</p>: String = TODO()")
        }
      }
    }
    createConfigFiles(project)
    val task = course.findTask("lesson1", "task2")
    val virtualFile = findFile("lesson1/task2/Task.kt")
    myFixture.openFileInEditor(virtualFile)
    testAction(CCEditAnswerPlaceholder.ACTION_ID)
    val navigatedFile = FileEditorManagerEx.getInstanceEx(project).currentFile ?: error("Navigated file should not be null here")

    assertEquals(task.getConfigDir(project).findChild(task.configFileName), navigatedFile)
  }
}