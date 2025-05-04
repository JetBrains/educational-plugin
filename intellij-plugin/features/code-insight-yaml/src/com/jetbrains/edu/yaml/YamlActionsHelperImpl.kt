package com.jetbrains.edu.yaml

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import com.jetbrains.edu.coursecreator.actions.YamlActionsHelper
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLACEHOLDER_TEXT
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence

class YamlActionsHelperImpl : YamlActionsHelper {

  override fun findPsiElementCorrespondingToPlaceholder(taskInfoYamlPsiFile: PsiFile, placeholder: AnswerPlaceholder): NavigatablePsiElement? {
    val yamlDocument = taskInfoYamlPsiFile.childrenOfType<YAMLDocument>().firstOrNull() ?: return null

    val topLevelObject = yamlDocument.firstChild as? YAMLMapping ?: return null

    val filesList = topLevelObject.getKeyValueByKey(FILES)?.value as? YAMLSequence ?: return null

    val taskFile = placeholder.taskFile
    val task = taskFile.task
    val taskFileIndex = task.taskFileIndex(taskFile.name) ?: return null
    val placeholderIndex = taskFile.answerPlaceholders.indexOf(placeholder)

    val fileObject = filesList.items.getOrNull(taskFileIndex)?.value as? YAMLMapping ?: return null
    val placeholderList = fileObject.getKeyValueByKey(PLACEHOLDERS)?.value as? YAMLSequence ?: return null

    val entirePlaceholder = placeholderList.items.getOrNull(placeholderIndex)?.value as? YAMLMapping ?: return null

    val placeholderTextSubElement = entirePlaceholder.getKeyValueByKey(PLACEHOLDER_TEXT)?.value

    return placeholderTextSubElement ?: entirePlaceholder
  }
}