package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.taskFile.CCShowPreview
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.withTestDialog

class CCCreateTaskFilePreviewTest : EduActionTestCase() {
  fun `test show error if placeholder is broken`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """fn fizzz() = <p>TODO()</p>""")
        }
      }
    }
    course.description = "my summary"
    val placeholder = course.lessons.first().taskList.first().taskFiles["fizz.kt"]!!.answerPlaceholders?.firstOrNull()
                      ?: error("Cannot find placeholder")
    placeholder.offset = 1000

    withTestDialog(EduTestDialog()) {
      testAction(createDataContext(findFile("lesson1/task1/fizz.kt")), CCShowPreview())
    }.checkWasShown("Broken placeholder 1 of 1, offset 1000, length 0.")
  }

  fun `test show error if we have no placeholders`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("fizz.kt", "no placeholders")
        }
      }
    }
    course.description = "my summary"

    withTestDialog(EduTestDialog()) {
      testAction(createDataContext(findFile("lesson1/task1/fizz.kt")), CCShowPreview())
    }.checkWasShown(CCShowPreview.NO_PREVIEW_MESSAGE)
  }

  private fun createDataContext(file: VirtualFile): DataContext {
    val context = MapDataContext()
    context.put(CommonDataKeys.PSI_FILE, PsiManager.getInstance(project).findFile(file))
    context.put(CommonDataKeys.PROJECT, project)
    context.put(LangDataKeys.MODULE, myFixture.module)
    return context
  }
}