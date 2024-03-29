package com.jetbrains.edu.jvm

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.DefaultSettingsUtils.findPath
import com.jetbrains.edu.learning.DefaultSettingsUtils.propertyValue
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.EduProjectSettings

open class JdkProjectSettings(val model: ProjectSdksModel, val jdk: Sdk?) : EduProjectSettings {

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

    private const val DEFAULT_JDK_PROPERTY: String = "project.jdk"
    private const val DEFAULT_JDK_NAME_PROPERTY: String = "project.jdk.name"

    private const val DEFAULT_JDK_NAME: String = "jdk"

    fun emptySettings(): JdkProjectSettings {
      val configurable = ProjectStructureConfigurable.getInstance(ProjectManager.getInstance().defaultProject)
      return JdkProjectSettings(configurable.projectJdksModel, null)
    }

    fun defaultSettings(): Result<JdkProjectSettings, String> {
      // Use `EnvironmentService` instead to get default JDK path and name
      return findPath(DEFAULT_JDK_PROPERTY, "jdk").flatMap { jdkPath ->
        val jdkName = propertyValue(DEFAULT_JDK_NAME_PROPERTY, "JDK name").onError { DEFAULT_JDK_NAME }

        var jdk = ProjectJdkTable.getInstance().findJdk(jdkName)

        if (jdk == null) {
          val jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(jdkPath)
          if (jdkHomeDir == null) {
            return@flatMap Err("$jdkPath doesn't exist")
          }
          jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), jdkHomeDir, JavaSdk.getInstance(), true, null, jdkName)
          if (jdk == null) {
            return@flatMap Err("Failed to create JDK for $jdkPath")
          }
        }

        val sdksModel = ProjectSdksModel()
        sdksModel.addSdk(jdk)

        Ok(JdkProjectSettings(sdksModel, jdk))
      }
    }
  }
}
