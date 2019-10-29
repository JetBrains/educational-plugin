package com.jetbrains.edu.coursecreator.handlers.rename

import com.intellij.ide.TitledHandler
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCStudyItemPathInputValidator
import com.jetbrains.edu.coursecreator.actions.StudyItemType
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import java.io.IOException

abstract class CCStudyItemRenameHandler(private val studyItemType: StudyItemType) : CCRenameHandler(), TitledHandler {

  protected fun processRename(item: StudyItem, namePrefix: String, course: Course, project: Project, directory: VirtualFile) {
    val configurator = course.configurator
    val name = item.name
    val text = "Rename ${StringUtil.toTitleCase(namePrefix)}"
    val validator = CCStudyItemPathInputValidator(course, studyItemType, directory.parent, name)
    val newName = Messages.showInputDialog(project, "$text '$name' to", text, null, name, validator) ?: return
    item.name = newName
    runWriteAction {
      try {
        directory.rename(CCStudyItemRenameHandler::class.java, newName)
      }
      catch (e: IOException) {
        Logger.getInstance(CCStudyItemRenameHandler::class.java).error(e)
      }
    }
    configurator?.courseBuilder?.refreshProject(project)
  }
}
