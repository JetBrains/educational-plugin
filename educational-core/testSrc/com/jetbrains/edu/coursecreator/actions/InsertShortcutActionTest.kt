package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
import java.awt.event.KeyEvent


class InsertShortcutActionTest : EduActionTestCase() {
  fun `test shortcut inserted`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    val task = findTask(0, 0)
    val taskDescriptionFile = task.getDir(project)?.findChild(EduNames.TASK_HTML) ?: error("No task description file")

    myFixture.openFileInEditor(taskDescriptionFile)

    testAction(createDataContext(taskDescriptionFile), object : InsertShortcutAction() {
      override fun createAndShowBalloon(listWithSearchField: ListWithSearchField, project: Project): JBPopup? {
        val balloon = super.createAndShowBalloon(listWithSearchField, project)
        val actionList = listWithSearchField.list
        actionList.selectedIndex = 0
        actionList.keyListeners.forEach {
          it.keyPressed(KeyEvent(actionList, 0, 0, 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED))
        }
        assertTrue(myFixture.editor.document.text.contains(ActionManager.getInstance().getId(actionList.selectedValue)))
        return balloon
      }
    })
  }

  fun `test action not available in task file`() {
    val taskFileName = "taskFile1.txt"
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile(taskFileName)
        }
      }
    }

    checkActionNotAvailable(findFileInTask(0, 0, taskFileName))
  }

  private fun checkActionNotAvailable(virtualFile: VirtualFile) {
    myFixture.openFileInEditor(virtualFile)
    val presentation = testAction(createDataContext(virtualFile), InsertShortcutAction(), false)
    checkActionEnabled(presentation, false)
  }

  fun `test action not available in student project`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    val task = findTask(0, 0)
    val taskDescriptionFile = task.getDir(project)?.findChild(EduNames.TASK_HTML) ?: error("No task description file")

    checkActionNotAvailable(taskDescriptionFile)
  }

  private fun createDataContext(virtualFile: VirtualFile): DataContext {
    return MapDataContext().apply {
      put(CommonDataKeys.PROJECT, project)
      put(CommonDataKeys.VIRTUAL_FILE, virtualFile)
      put(CommonDataKeys.EDITOR, myFixture.editor)
    }
  }
}