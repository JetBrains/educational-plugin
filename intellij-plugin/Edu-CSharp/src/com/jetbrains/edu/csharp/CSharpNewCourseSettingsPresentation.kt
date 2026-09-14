package com.jetbrains.edu.csharp

import com.jetbrains.edu.csharp.messages.EduCSharpBundle
import com.jetbrains.edu.learning.newproject.ui.newCourseSettings.NewCourseSettingsListPresenter
import javax.swing.Icon

object CSharpNewCourseSettingsPresentation : NewCourseSettingsListPresenter<CSharpNewCourseSettings> {
  override fun label(): String = EduCSharpBundle.message("target.framework")

  override fun name(settings: CSharpNewCourseSettings): String = settings.languageVersion

  override fun icon(settings: CSharpNewCourseSettings): Icon? = null
}
