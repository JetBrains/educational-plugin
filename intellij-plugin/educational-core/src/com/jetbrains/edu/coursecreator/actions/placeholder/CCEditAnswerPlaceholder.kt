package com.jetbrains.edu.coursecreator.actions.placeholder

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.mapper
import org.jetbrains.annotations.NonNls

open class CCEditAnswerPlaceholder : CCAnswerPlaceholderAction() {

  override fun performAnswerPlaceholderAction(state: EduState) {
    val answerPlaceholder = state.answerPlaceholder ?: return
    val task = answerPlaceholder.taskFile.task
    val configFileName = task.configFileName
    val project = state.project

    val taskDir = task.getDir(project.courseDir)
    if (taskDir == null) {
      LOG.error("Failed to find task directory")
      return
    }
    val configFile = taskDir.findChild(configFileName)
    if (configFile == null) {
      LOG.error("Failed to find task config file")
      return
    }

    val fileEditorManager = FileEditorManager.getInstance(project)
    fileEditorManager.openFile(configFile, true)

    val textEditor = fileEditorManager.selectedTextEditor
    moveCaretToPlaceholder(textEditor, answerPlaceholder, project)
  }

  /**
   * Parses yaml until the placeholder is found.
   * The YAML Psi Tree is not used to avoid the YAML Plugin dependency.
   * Although, if the YAML Plugin was available, the code would be shorter and more straightforward.
   */
  private fun moveCaretToPlaceholder(textEditor: Editor?, answerPlaceholder: AnswerPlaceholder, project: Project) {
    textEditor ?: return
    val yamlMapper = project.course?.mapper ?: return

    val taskFile = answerPlaceholder.taskFile
    val taskFileIndex = taskFile.task.taskFileIndex(taskFile.name) ?: return

    val answerPlaceholderIndex = taskFile.answerPlaceholders.indexOf(answerPlaceholder)
    if (answerPlaceholderIndex < 0) return

    val yaml = textEditor.document.text
    val offset = yamlMapper.createParser(yaml).use { parser ->
      searchPlaceholderOffsetInYAML(parser, taskFileIndex, answerPlaceholderIndex)
    }

    if (offset != null) {
      textEditor.caretModel.moveToOffset(offset)
      textEditor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
  }

  private fun searchPlaceholderOffsetInYAML(parser: JsonParser?, taskFileIndex: Int, answerPlaceholderIndex: Int): Int? {
    val tokenLocation = parser
      ?.skipInsideObjectToValue("files")
      ?.skipInsideArrayToIndex(taskFileIndex)
      ?.skipInsideObjectToValue("placeholders")
      ?.skipInsideArrayToIndex(answerPlaceholderIndex)
      ?.tokenLocation
      ?:return null

    return tokenLocation.charOffset.toInt()
  }

  /**
   * The next token must be START_OBJECT.
   * The parser will be returned such that its next token is the value.
   * @return [this] or null, if failed to skip
   */
  private fun JsonParser.skipInsideObjectToValue(key: String): JsonParser? {
    if (nextToken() != JsonToken.START_OBJECT) return null

    while (nextToken() == JsonToken.FIELD_NAME) {
      val fieldName = currentName() ?: return null

      if (fieldName == key) {
        return this
      }
      else {
        nextToken()
        skipChildren()
      }
    }

    return null
  }

  /**
   * The next token must be START_ARRAY.
   * The parser will be returned such that its next token is the array value.
   * @return [this] or null, if failed to skip
   */
  private fun JsonParser.skipInsideArrayToIndex(index: Int): JsonParser? {
    if (nextToken() != JsonToken.START_ARRAY) return null

    for (i in 0 until index) {
      val token = nextToken() ?: return null
      if (token == JsonToken.END_ARRAY) return null

      skipChildren()
    }

    return this
  }

  override fun updatePresentation(eduState: EduState, presentation: Presentation) {
    presentation.isEnabledAndVisible = eduState.answerPlaceholder != null
  }

  companion object {
    private val LOG = Logger.getInstance(CCEditAnswerPlaceholder::class.java)

    @NonNls
    const val ACTION_ID = "Educational.Educator.EditAnswerPlaceholder"
  }
}