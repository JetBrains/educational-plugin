package com.jetbrains.edu.learning.handlers.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import com.jetbrains.edu.coursecreator.CCStudyItemPathInputValidator
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.studyItemType
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class StudyItemRenameDialog(
  private val item: StudyItem,
  project: Project,
  element: PsiElement,
  nameSuggestionContext: PsiElement?,
  editor: Editor?
) : RenamePsiFileProcessor.PsiFileRenameDialog(project, element, nameSuggestionContext, editor) {

  init {
    title = "Rename ${item.studyItemType.presentableTitleName}"
  }

  override fun performRename(newName: String) {
    super.performRename(newName)
    item.name = newName
    YamlFormatSynchronizer.saveItem(item.parent)
    item.course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
  }

  @Throws(ConfigurationException::class)
  override fun canRun() {
    if (item.course.isStudy) {
      throw ConfigurationException("This rename operation can break the course")
    }
    val itemDir = item.getDir(project)
    val validator = CCStudyItemPathInputValidator(item.course, item.studyItemType, itemDir.parent, item.name)
    if (!validator.checkInput(newName)) {
      throw ConfigurationException(validator.getErrorText(newName))
    }
  }
}
