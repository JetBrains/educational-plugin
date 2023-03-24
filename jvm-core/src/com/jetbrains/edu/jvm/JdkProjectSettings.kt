package com.jetbrains.edu.jvm

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.jetbrains.edu.learning.courseFormat.Course

class JdkProjectSettings(val model: ProjectSdksModel, val jdk: Sdk?) {

  fun setUpProjectJdk(
    project: Project,
    course: Course,
    getJdk: JdkProjectSettings.() -> Sdk? = { jdk }
  ): Sdk? {
    val jdk = getJdk()

    // Try to apply model, i.e. commit changes from sdk model into ProjectJdkTable
    try {
      model.apply()
    }
    catch (e: ConfigurationException) {
      LOG.error(e)
    }

    runWriteAction {
      ProjectRootManager.getInstance(project).projectSdk = jdk
      addAnnotations(ProjectRootManager.getInstance(project).projectSdk?.sdkModificator)
      val sdkVersion = course.minJvmSdkVersion
      if (sdkVersion is JavaVersionParseSuccess) {
        LanguageLevelProjectExtension.getInstance(project).languageLevel = sdkVersion.javaSdkVersion.maxLanguageLevel
      }
    }
    return jdk
  }

  private fun addAnnotations(sdkModificator: SdkModificator?) {
    sdkModificator?.apply {
      JavaSdkImpl.attachJdkAnnotations(this)
      commitChanges()
    }
  }

  companion object {

    private val LOG = logger<JdkProjectSettings>()

    fun emptySettings(): JdkProjectSettings {
      val configurable = ProjectStructureConfigurable.getInstance(ProjectManager.getInstance().defaultProject)
      return JdkProjectSettings(configurable.projectJdksModel, null)
    }
  }
}
