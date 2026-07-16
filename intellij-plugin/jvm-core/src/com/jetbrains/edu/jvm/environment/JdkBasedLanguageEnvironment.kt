package com.jetbrains.edu.jvm.environment

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.util.lang.JavaVersion
import com.jetbrains.edu.jvm.JavaVersionParseSuccess
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.jvm.minJvmSdkVersion
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.InstallationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A base for all JDK language environments that have some [Sdk] and [ProjectSdksModel] inside.
 */
sealed class JdkBasedLanguageEnvironment : JdkLanguageEnvironment {
  abstract val homePath: String
  abstract val version: JavaVersion
  abstract val model: ProjectSdksModel

  /**
   * Presentation text with the item name
   */
  abstract val itemName: String

  abstract val buildSystemSupport: JdkBuildSystemSupport

  /**
   * Returns a JDK already prepared, configured, and already added to the [model].
   */
  protected abstract suspend fun buildJdk(): Result<Sdk, String>

  final override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult {
    val jdk = when (val jdkResult = buildJdk()) {
      is Err -> return InstallationResult.Error(jdkResult.error)
      is Ok -> jdkResult.value
    }

    // Try to apply model, i.e. commit changes from sdk model into ProjectJdkTable
    try {
      withContext(Dispatchers.EDT) {
        model.apply()
      }
    }
    catch (e: ConfigurationException) {
      LOG.error("Failed to apply SDK model changes", e)
      return InstallationResult.Error(EduJVMBundle.message("error.jdk.model.apply.failed", e.message ?: ""))
    }

    edtWriteAction {
      ProjectRootManager.getInstance(project).projectSdk = jdk
      addAnnotations(jdk.sdkModificator)
      val sdkVersion = course.minJvmSdkVersion
      if (sdkVersion is JavaVersionParseSuccess) {
        LanguageLevelProjectExtension.getInstance(project).languageLevel = sdkVersion.javaSdkVersion.maxLanguageLevel
      }
    }

    buildSystemSupport.configureProject(project, course, jdk)

    return InstallationResult.Installed
  }

  private fun addAnnotations(sdkModificator: SdkModificator?) {
    sdkModificator?.apply {
      JavaSdkImpl.attachJdkAnnotations(this)
      commitChanges()
    }
  }

  companion object {
    private val LOG = logger<JdkBasedLanguageEnvironment>()
  }
}