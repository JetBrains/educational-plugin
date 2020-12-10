package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class CCPluginToggleAction : ToggleAction(EduCoreBundle.lazyMessage("action.toggle.cc.features.text")) {
  override fun isSelected(e: AnActionEvent): Boolean = isCourseCreatorFeaturesEnabled

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    PropertiesComponent.getInstance().setValue(COURSE_CREATOR_ENABLED, state)
  }

  companion object {
    @NonNls
    const val COURSE_CREATOR_ENABLED = "Edu.CourseCreator.Enabled"

    @JvmStatic
    val isCourseCreatorFeaturesEnabled: Boolean
      get() = PropertiesComponent.getInstance().getBoolean(COURSE_CREATOR_ENABLED) || isUnitTestMode
  }
}