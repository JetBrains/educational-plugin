package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

class CCNewCourseAction(title: Supplier<String> = EduCoreBundle.lazyMessage("action.new.course.default.text"))
  : DumbAwareAction(title, EduCoreBundle.lazyMessage("action.new.course.description"), null) {

  constructor(@Nls(capitalization = Nls.Capitalization.Title) title: String) : this(Supplier { title })

  override fun actionPerformed(e: AnActionEvent) {
    CCNewCourseDialog(EduCoreBundle.message("dialog.title.create.course"), EduCoreBundle.message("button.create")).show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
  }
}
