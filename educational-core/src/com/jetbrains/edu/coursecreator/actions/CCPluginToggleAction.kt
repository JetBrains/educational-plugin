package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class CCPluginToggleAction : DumbAwareToggleAction(EduCoreBundle.lazyMessage("action.toggle.cc.features.text")) {
  override fun isSelected(e: AnActionEvent): Boolean = isCourseCreatorFeaturesEnabled

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    isCourseCreatorFeaturesEnabled = state
  }

  companion object {
    @NonNls
    private const val COURSE_CREATOR_ENABLED = "Edu.CourseCreator.Enabled"

    var isCourseCreatorFeaturesEnabled: Boolean
      get() = PropertiesComponent.getInstance().getBoolean(COURSE_CREATOR_ENABLED, true) || isUnitTestMode
      set(value) {
        // `PropertiesComponent` removes entry from the corresponding map if `value` equals default value.
        // But we always want to write property value to property component so let's pass `!value` as default value
        PropertiesComponent.getInstance().setValue(COURSE_CREATOR_ENABLED, value, !value)
      }
  }
}
