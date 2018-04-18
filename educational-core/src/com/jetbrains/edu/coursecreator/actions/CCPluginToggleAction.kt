package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

class CCPluginToggleAction : ToggleAction() {
  override fun isSelected(e: AnActionEvent): Boolean = isCourseCreatorFeaturesEnabled

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    PropertiesComponent.getInstance().setValue(COURSE_CREATOR_ENABLED, state)
  }

  companion object {
    const val COURSE_CREATOR_ENABLED = "Edu.CourseCreator.Enabled"

    @JvmStatic
    val isCourseCreatorFeaturesEnabled: Boolean get() = PropertiesComponent.getInstance().getBoolean(COURSE_CREATOR_ENABLED)
  }
}