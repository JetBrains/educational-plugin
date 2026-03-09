package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_INTERNAL_JAVA
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloadUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.lang.JavaVersion
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.GRADLE_WRAPPER_PROPERTIES_PATH
import com.jetbrains.edu.jvm.gradle.GradleWrapperListener
import com.jetbrains.edu.jvm.lookupJdkByPath
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.ext.getAdditionalFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createFromInternalTemplateOrFromDisk
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.plugins.gradle.jvmcompat.GradleJvmSupportMatrix
import org.jetbrains.plugins.gradle.service.GradleInstallationManager
import org.jetbrains.plugins.gradle.service.execution.GradleDaemonJvmCriteria
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil
import org.jetbrains.plugins.gradle.util.toJvmCriteria
import java.io.File
import java.io.IOException
import java.nio.file.Path

object EduGradleUtils {

  private val LOG = thisLogger()

  fun isConfiguredWithGradle(project: Project): Boolean {
    return hasDefaultGradleScriptFile(project) || hasDefaultGradleKtsScriptFile(project)
  }

  private fun hasDefaultGradleScriptFile(project: Project): Boolean {
    return File(project.basePath, GradleConstants.DEFAULT_SCRIPT_NAME).exists()
  }

  private fun hasDefaultGradleKtsScriptFile(project: Project): Boolean {
    return File(project.basePath, GradleConstants.KOTLIN_DSL_SCRIPT_NAME).exists()
  }

  fun hasCourseHaveGradleKtsFiles(course: Course): Boolean =
    course.getAdditionalFile(GradleConstants.KOTLIN_DSL_SCRIPT_NAME) != null &&
    course.getAdditionalFile(GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME) != null

  @Throws(IOException::class)
  fun createProjectGradleFiles(
    holder: CourseInfoHolder<Course>,
    templates: Map<String, String>,
    templateVariables: Map<String, Any>
  ): List<EduFile> =
    templates.map { (name, templateName) ->
      createFromInternalTemplateOrFromDisk(holder.courseDir, name, templateName, templateVariables)
    }

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
      setUpGradleJvm(project, existingProject, sdk)
      return
    }

    val gradleProjectSettings = GradleProjectSettings()
    gradleProjectSettings.distributionType = distributionType
    gradleProjectSettings.externalProjectPath = location
    // IDEA runner is much more faster and it doesn't write redundant messages into console.
    // Note, it doesn't affect tests - they still are run with gradle runner
    gradleProjectSettings.delegatedBuild = false
    setUpGradleJvm(project, gradleProjectSettings, sdk)

    val projects = systemSettings.linkedProjectsSettings.toHashSet()
    projects.add(gradleProjectSettings)
    systemSettings.linkedProjectsSettings = projects
  }

  @VisibleForTesting
  fun setUpGradleJvm(project: Project, projectSettings: GradleProjectSettings, sdk: Sdk?) {
    if (sdk == null) return

    val gradleVersion = detectGradleVersion(projectSettings)

    val internalJavaVersion = runWithModalProgressBlocking(project, EduJVMBundle.message("progress.resolving.suitable.jdk")) {
      ExternalSystemJdkUtil.resolveJdkName(null as Sdk?, USE_INTERNAL_JAVA)
    }?.javaVersion

    val projectJavaVersion = sdk.javaVersion

    val projectSupported = isGradleCompatible(gradleVersion, projectJavaVersion)
    val internalSupported = isGradleCompatible(gradleVersion, internalJavaVersion)

    val useGradleJdk = when {
      projectSupported -> {
        LOG.info("Project JDK is compatible with gradle $gradleVersion. Will be used")
        USE_PROJECT_JDK
      }
      internalSupported -> {
        LOG.info("Internal JDK is compatible with gradle $gradleVersion. Will be used")
        USE_INTERNAL_JAVA
      }
      else -> {
        // Use available compatible JDK if there is one, otherwise download a compatible JDK
        val availableCompatibleJdkName = findAvailableCompatibleGradleJdkName(project, gradleVersion)
        if (availableCompatibleJdkName != null) {
          LOG.info("Found compatible JDK $availableCompatibleJdkName for gradle $gradleVersion. Will use it.")
          availableCompatibleJdkName
        }
        else {
          downloadLatestGradleCompatibleJDK(project, gradleVersion)
        }
      }
    }

    if (useGradleJdk != null) {
      projectSettings.gradleJvm = useGradleJdk
    }
  }

  private val Sdk.javaSdkVersion: JavaSdkVersion? get() = JavaSdk.getInstance().getVersion(this)
  private val Sdk.javaVersion: JavaVersion? get() = javaSdkVersion?.maxLanguageLevel?.toJavaVersion()

  private fun isGradleCompatible(gradleVersion: GradleVersion, javaVersion: JavaVersion?): Boolean {
    return javaVersion != null && GradleJvmSupportMatrix.isSupported(gradleVersion, javaVersion)
  }

  private fun findAvailableCompatibleGradleJdkName(project: Project, gradleVersion: GradleVersion): String? {
    val (existingCompatibleJdkHomePath, _) =
      JavaSdk.getInstance().collectSdkEntries(project)
        .asSequence()
        .mapNotNull { e ->
          val javaVersion = JavaVersion.tryParse(e.versionString) ?: return@mapNotNull null
          e.homePath() to javaVersion
        }
        .filter { (homePath, javaVersion) ->
          ExternalSystemJdkUtil.isValidJdk(homePath) && isGradleCompatible(gradleVersion, javaVersion)
        }
        .maxByOrNull { (_, javaVersion) -> javaVersion } ?: return null

    val existingCompatibleJdk = lookupJdkByPath(project, existingCompatibleJdkHomePath)

    return existingCompatibleJdk.name
  }

  private fun downloadLatestGradleCompatibleJDK(
    project: Project,
    gradleVersion: GradleVersion
  ): String? {
    val supportedJavaVersion = GradleJvmSupportMatrix.suggestLatestSupportedJavaVersion(gradleVersion)

    if (supportedJavaVersion == null) {
      LOG.warn("Failed to determine compatible JDK for gradle $gradleVersion")
      return null
    }

    val criteria = GradleDaemonJvmCriteria(supportedJavaVersion.feature.toString(), null)

    return runWithModalProgressBlocking(project, EduJVMBundle.message("progress.resolving.suitable.gradle.jdk")) {
      withContext(Dispatchers.IO) {
        findAndDownloadGradleJVM(project, criteria, gradleVersion)
      }
    }
  }

  private suspend fun findAndDownloadGradleJVM(
    project: Project,
    criteria: GradleDaemonJvmCriteria,
    gradleVersion: GradleVersion
  ): String? {
    val jdkItemAndHome = JdkDownloadUtil.pickJdkItemAndPath(project) { jdkItem ->
      jdkItem.toJvmCriteria().matches(criteria)
    }

    if (jdkItemAndHome == null) {
      LOG.warn("Failed to find a compatible downloadable JDK for gradle $gradleVersion")
      return null
    }
    val (jdkItem, jdkHome) = jdkItemAndHome

    LOG.info("Found JDK $jdkItem for gradle $gradleVersion. Will be downloaded")

    val downloadTask = JdkDownloadUtil.createDownloadTask(project, jdkItem, jdkHome)

    if (downloadTask == null) {
      LOG.warn("Failed to create download task for JDK $jdkItem. Need JDK for gradle $gradleVersion.")
      return null
    }

    val sdk = JdkDownloadUtil.createDownloadSdk(ExternalSystemJdkUtil.getJavaSdkType(), downloadTask)

    return if (JdkDownloadUtil.downloadSdk(sdk)) {
      LOG.info("Successfully downloaded JDK $jdkItem for gradle $gradleVersion. Name: ${sdk.name}")
      sdk.name
    }
    else {
      LOG.warn("Failed to download SDK $jdkItem. Need JDK for gradle $gradleVersion.")
      null
    }
  }

  fun updateGradleSettings(project: Project) {
    val projectBasePath = project.basePath ?: error("Failed to find base path for the project during gradle project setup")
    val sdk = ProjectRootManager.getInstance(project).projectSdk
    setGradleSettings(project, sdk, projectBasePath)
  }

  fun setupGradleProject(project: Project) {
    makeGradlewExecutable(project)
    addGradleWrapperPropertiesToAdditionalFiles(project)
  }

  private fun makeGradlewExecutable(project: Project) {
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

  /*
    Starting from 2025.10.1, `gradle/wrapper/gradle-wrapper.properties` should be in the current archive.
    Since this file is generated by gradle wrapper task and is not tracked by CCVirtualFIleListener,
    it should be manually added as an additional file.
  */
  private fun addGradleWrapperPropertiesToAdditionalFiles(project: Project) {
    val course = project.course ?: return
    val fileName = GRADLE_WRAPPER_PROPERTIES_PATH
    if (course.additionalFiles.any { it.name == fileName }) return
    invokeAndWaitIfNeeded {
      runWriteAction {
        course.additionalFiles += EduFile(fileName, TextualContents.EMPTY)
      }
    }
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun detectGradleVersion(projectSettings: GradleProjectSettings): GradleVersion {
    val guessedVersion = GradleInstallationManager.guessGradleVersion(projectSettings)

    if (guessedVersion != null) {
      LOG.info("Guessed Gradle version to be: $guessedVersion")
      return guessedVersion
    }

    // If the project has DistributionType.WRAPPED, the `guessGradleVersion` always returns null.
    // So we should manually guess the version by inspecting the wrapper properties file.
    val versionFromGradleWrapperProperties = findVersionInsideGradleWrapperProperties(projectSettings)

    if (versionFromGradleWrapperProperties != null) {
      LOG.info("Found Gradle version in gradle-wrapper.properties: $versionFromGradleWrapperProperties")
      return versionFromGradleWrapperProperties
    }

    val currentGradleVersion = GradleVersion.current()

    LOG.warn("Failed to determine gradle version. Will use the current version $currentGradleVersion")

    return currentGradleVersion
  }

  private fun findVersionInsideGradleWrapperProperties(projectSettings: GradleProjectSettings): GradleVersion? {
    // This code repeats org.jetbrains.plugins.gradle.service.execution.LocalBuildLayoutParameters.guessGradleVersion.
    // with the mode = DistributionType.DEFAULT_WRAPPED

    val projectPath = Path.of(projectSettings.externalProjectPath)
    val wrapperConfiguration = GradleUtil.getWrapperConfiguration(projectPath)
    val gradleWrapperProperties = wrapperConfiguration?.distribution?.rawPath ?: return null
    return GradleInstallationManager.parseDistributionVersion(gradleWrapperProperties)
  }
}
