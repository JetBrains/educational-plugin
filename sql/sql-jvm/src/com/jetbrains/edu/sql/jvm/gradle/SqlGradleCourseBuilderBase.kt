package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.Disposable
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.core.SqlConfiguratorBase
import javax.swing.JComponent

abstract class SqlGradleCourseBuilderBase : GradleCourseBuilderBase() {
  override val taskTemplateName: String
    get() = SqlConfiguratorBase.TASK_SQL

  override fun getDefaultTaskTemplates(
    course: Course,
    info: NewStudyItemInfo,
    withSources: Boolean,
    withTests: Boolean
  ): List<TemplateFileInfo> {
    val templates = super.getDefaultTaskTemplates(course, info, withSources, withTests)
    return if (withSources) {
      templates + TemplateFileInfo(INIT_SQL, INIT_SQL, isVisible = false)
    } else {
      templates
    }
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = BundledJdkProjectSettings()

  companion object {
    const val INIT_SQL = "init.sql"
  }
}

class BundledJdkProjectSettings : JdkLanguageSettings() {

  override fun setupProjectSdksModel(model: ProjectSdksModel) {
    val (jdkPath, sdk) = findBundledJdk(model) ?: return
    if (sdk == null) {
      model.addSdk(JavaSdk.getInstance(), jdkPath) {
        jdk = it
      }
    }
    else {
      jdk = sdk
    }
  }

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: Disposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    // Non-null jdk means that `setupProjectSdksModel` successfully found bundled JDK.
    // So there is no reason to show JDK settings at all
    return if (jdk != null) emptyList() else super.getLanguageSettingsComponents(course, disposable, context)
  }
}
