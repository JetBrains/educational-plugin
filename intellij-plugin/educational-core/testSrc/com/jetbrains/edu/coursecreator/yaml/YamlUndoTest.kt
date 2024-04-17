package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeTask
import com.jetbrains.edu.learning.yaml.YamlMapper.MAPPER
import com.jetbrains.edu.learning.yaml.YamlTestCase
import org.junit.Test

open class YamlUndoTest : YamlTestCase() {

  override fun setUp() {
    super.setUp()
    // as we don't create config file in CCProjectComponent, we have to create them manually
    createConfigFiles(project)
  }

  override fun createCourse() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile(TASK_FILE_NAME, "42 is the <p>TODO()</p>. 43 is not") {
            placeholder(0, possibleAnswer = "right answer")
          }
        }
      }
    }
    course.description = "test"
  }

  // EDU-2217
  @Test
  fun `test undo inside placeholder`() {
    val (task, taskFile, placeholder) = getCourseElements()
    val startOffset = placeholder.offset
    val endOffset = placeholder.endOffset

    typeInTaskFile(taskFile, startOffset + 1)

    performUndo()

    checkPlaceholders(task.getDir(project.courseDir)!!, placeholder, startOffset, endOffset)
  }

  // EDU-2217
  @Test
  fun `test undo after placeholder`() {
    val (task, taskFile, placeholder) = getCourseElements()
    val startOffset = placeholder.offset
    val endOffset = placeholder.endOffset

    typeInTaskFile(taskFile, endOffset + 1)

    performUndo()

    checkPlaceholders(task.getDir(project.courseDir)!!, placeholder, startOffset, endOffset)
  }

  // EDU-2161
  @Test
  fun `test two undo inside placeholder`() {
    val (task, taskFile, placeholder) = getCourseElements()
    val startOffset = placeholder.offset
    val endOffset = placeholder.endOffset

    typeInTaskFile(taskFile, startOffset + 1)
    typeInTaskFile(taskFile, startOffset + 2)

    performUndo()
    performUndo()

    checkPlaceholders(task.getDir(project.courseDir)!!, placeholder, startOffset, endOffset)
  }

  private fun performUndo() {
    UndoManager.getInstance(project).undo(getFileEditor(myFixture.editor))
  }

  private fun getCourseElements(): Triple<Task, TaskFile, AnswerPlaceholder> {
    val course = StudyTaskManager.getInstance(project).course!!
    val task = course.lessons.first().taskList.first()
    val taskFile = task.getTaskFile(TASK_FILE_NAME)!!
    val placeholder = taskFile.answerPlaceholders.first()
    return Triple(task, taskFile, placeholder)
  }

  private fun checkPlaceholders(taskDir: VirtualFile,
                                placeholder: AnswerPlaceholder,
                                expectedStartOffset: Int,
                                expectedEndOffset: Int) {
    UIUtil.dispatchAllInvocationEvents()
    assertEquals(expectedStartOffset, placeholder.offset)
    assertEquals(expectedEndOffset, placeholder.endOffset)

    val taskConfig = taskDir.findChild(YamlConfigSettings.TASK_CONFIG)!!
    val document = FileDocumentManager.getInstance().getDocument(taskConfig)!!
    val deserializedTask = MAPPER.deserializeTask(document.text)
    val deserializedPlaceholder = deserializedTask.getTaskFile(TASK_FILE_NAME)!!.answerPlaceholders.first()
    assertEquals(expectedStartOffset, deserializedPlaceholder.offset)
    assertEquals(expectedEndOffset, deserializedPlaceholder.endOffset)
  }

  private fun typeInTaskFile(taskFile: TaskFile, offset: Int) {
    typeInEditor(taskFile.getVirtualFile(project)!!, offset)
  }

  private fun typeInEditor(virtualFile: VirtualFile, offset: Int) {
    myFixture.openFileInEditor(virtualFile)
    myFixture.editor.selectionModel.removeSelection()
    myFixture.editor.caretModel.moveToOffset(offset)
    myFixture.type("t")
  }

  private fun getFileEditor(e: Editor?): FileEditor? {
    return if (e == null) null else TextEditorProvider.getInstance().getTextEditor(e)
  }

  companion object {
    private const val TASK_FILE_NAME = "Test.java"
  }
}