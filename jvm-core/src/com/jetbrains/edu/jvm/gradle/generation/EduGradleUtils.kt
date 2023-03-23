package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_INTERNAL_JAVA
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.jvm.gradle.GradleWrapperListener
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createFileFromTemplate
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.io.IOException

object EduGradleUtils {
  @JvmStatic
  fun isConfiguredWithGradle(project: Project): Boolean {
    return hasDefaultGradleScriptFile(project) || hasDefaultGradleKtsScriptFile(project)
  }

  @JvmStatic
  fun hasDefaultGradleScriptFile(project: Project): Boolean {
    return File(project.basePath, GradleConstants.DEFAULT_SCRIPT_NAME).exists()
  }

  @JvmStatic
  fun hasDefaultGradleKtsScriptFile(project: Project): Boolean {
    return File(project.basePath, GradleConstants.KOTLIN_DSL_SCRIPT_NAME).exists()
  }


  @JvmStatic
  @Throws(IOException::class)
  fun createProjectGradleFiles(
    holder: CourseInfoHolder<Course>,
    templates: Map<String, String>,
    templateVariables: Map<String, Any>
  ) {
    for ((name, templateName) in templates) {
      createFileFromTemplate(holder, holder.courseDir, name, templateName, templateVariables)
    }
  }

  @JvmOverloads
  @JvmStatic
  fun setGradleSettings(project: Project, sdk: Sdk?, location: String, distributionType: DistributionType = DistributionType.WRAPPED) {
    val systemSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
    val existingProject = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).getLinkedProjectSettings(location)
    if (existingProject is GradleProjectSettings) {
      if (existingProject.distributionType == null) {
        existingProject.distributionType = distributionType
      }
      if (existingProject.externalProjectPath == null) {
        existingProject.externalProjectPath = location
      }
      setUpGradleJvm(existingProject, sdk)
      return
    }

    val gradleProjectSettings = GradleProjectSettings()
    gradleProjectSettings.distributionType = distributionType
    gradleProjectSettings.externalProjectPath = location
    // IDEA runner is much more faster and it doesn't write redundant messages into console.
    // Note, it doesn't affect tests - they still are run with gradle runner
    gradleProjectSettings.delegatedBuild = false
    setUpGradleJvm(gradleProjectSettings, sdk)

    val projects = systemSettings.linkedProjectsSettings.toHashSet()
    projects.add(gradleProjectSettings)
    systemSettings.linkedProjectsSettings = projects
  }

  private fun setUpGradleJvm(projectSettings: GradleProjectSettings, sdk: Sdk?) {
    if (sdk == null) return
    val projectSdkVersion = sdk.javaSdkVersion
    val internalJdk = ExternalSystemJdkUtil.resolveJdkName(null, USE_INTERNAL_JAVA)
    val internalSdkVersion = internalJdk?.javaSdkVersion

    // Try to avoid incompatibility between gradle and jdk versions
    projectSettings.gradleJvm = when {
      internalSdkVersion == null -> USE_PROJECT_JDK
      projectSdkVersion == null -> USE_INTERNAL_JAVA
      else -> if (internalSdkVersion < projectSdkVersion) USE_INTERNAL_JAVA else USE_PROJECT_JDK
    }
  }

  private val Sdk.javaSdkVersion: JavaSdkVersion? get() = JavaSdk.getInstance().getVersion(this)

  fun updateGradleSettings(project: Project) {
    val projectBasePath = project.basePath ?: error("Failed to find base path for the project during gradle project setup")
    val sdk = ProjectRootManager.getInstance(project).projectSdk
    setGradleSettings(project, sdk, projectBasePath)
  }

  fun setupGradleProject(project: Project) {
    val projectBasePath = project.basePath
    if (projectBasePath != null) {
      // Android Studio creates non executable `gradlew`
      val gradlew = File(FileUtil.toSystemDependentName(projectBasePath), GRADLE_WRAPPER_UNIX)
      if (gradlew.exists()) {
        gradlew.setExecutable(true)
      }
      else {
        val taskManager = StudyTaskManager.getInstance(project)
        val connection = ApplicationManager.getApplication().messageBus.connect(taskManager)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, GradleWrapperListener(connection))
      }
    }
  }
}
