package com.jetbrains.edu.learning.coursera

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle

@Suppress("ComponentNotRegistered") // educational-core.xml
class ImportCourseraAssignment : ImportLocalCourseAction(EduCoreBundle.lazyMessage("action.import.coursera.assigment.text")) {

  override fun update(e: AnActionEvent) {
    e.presentation.icon = AllIcons.ToolbarDecorator.Import
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
  }
}