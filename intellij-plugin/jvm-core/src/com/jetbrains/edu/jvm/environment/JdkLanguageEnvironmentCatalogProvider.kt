package com.jetbrains.edu.jvm.environment

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.rethrowControlFlowException
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.projectRoots.impl.SdkVersionUtil
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloadUtil
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkItem
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.lang.JavaVersion
import com.jetbrains.edu.jvm.*
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.DefaultSettingsUtils.findPath
import com.jetbrains.edu.learning.DefaultSettingsUtils.propertyValue
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.EnvironmentUiKind
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalog
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

open class JdkLanguageEnvironmentCatalogProvider(
  private val buildSystemSupport: JdkBuildSystemSupport
) : LanguageEnvironmentCatalogProvider<JdkLanguageEnvironment> {
  override val uiKind: EnvironmentUiKind
    get() = EnvironmentUiKind.ComboBox

  override suspend fun default(): Result<JdkLanguageEnvironment, String> {
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

      val versionInfo = SdkVersionUtil.getJdkVersionInfo(jdkPath) ?: return Err("Failed to get JDK version info for $jdkPath")

      Ok(JdkLanguageEnvironmentExisting(sdksModel, jdkPath, versionInfo.version, jdk.name, buildSystemSupport, jdk))
    }
  }

  override suspend fun collectEnvironmentsForCourse(
    course: Course,
    context: UserDataHolder
  ): Result<LanguageEnvironmentCatalog<JdkLanguageEnvironment>, String> {
    val defaultProject = ProjectManager.getInstance().defaultProject

    val sdkModel = withContext(Dispatchers.EDT) {
      ProjectStructureConfigurable.getInstance(defaultProject).projectJdksModel.apply {
        reset(defaultProject)
      }
    }

    val jdksFromModel = jdksFromModel(sdkModel).sortedByDescending { it.version }
    val autoDetectedJdks = autoDetectedJdks(defaultProject, sdkModel).sortedByDescending { it.version }
    val bundledJdks = listOfNotNull(bundledJdk(sdkModel))

    // the order is important, prefer first suitable jdk from this list
    val allAvailableJdks = (jdksFromModel + autoDetectedJdks + bundledJdks).distinctBy { it.homePath }

    val (jdkVersionRange, preferredJdkVersion) = when (val result = suitableJdkVersions(course)) {
      is Err -> return Err(result.error)
      is Ok -> result.value
    }

    val availableSuitableJdks = allAvailableJdks.filter { jdkVersionRange.contains(it.version.feature) }

    // the list of available environments must not be empty. Otherwise, return the error
    val availableEnvironments = availableSuitableJdks.ifEmpty {
      LOG.info("No suitable JDKs found. Creating a JDK that will be automatically downloaded.")
      when (val prepareDownloadResult = prepareDownloadableJdk(sdkModel, jdkVersionRange, preferredJdkVersion)) {
        is Ok -> listOf(prepareDownloadResult.value)
        is Err -> {
          return Err(EduJVMBundle.message("error.jdk.downloadable.create.failed", prepareDownloadResult.error))
        }
      }
    }

    return Ok(LanguageEnvironmentCatalog(availableEnvironments))
  }

  private suspend fun jdksFromModel(sdksModel: ProjectSdksModel): List<JdkLanguageEnvironmentExisting> {
    val modelSdks = withContext(Dispatchers.EDT) {
      sdksModel.sdks
    }

    return modelSdks.filter { it.sdkType is JavaSdk }.mapNotNull { sdk ->
      val homePath = sdk.homePath ?: return@mapNotNull null
      val versionInfo = SdkVersionUtil.getJdkVersionInfo(homePath) ?: return@mapNotNull null
      JdkLanguageEnvironmentExisting(sdksModel, homePath, versionInfo.version, sdk.name, buildSystemSupport, sdk)
    }
  }

  private fun autoDetectedJdks(project: Project, sdkModel: ProjectSdksModel): List<JdkLanguageEnvironmentFromDisk> =
    ExternalSystemJdkUtil.suggestJdkHomePaths(project).mapNotNull { it.toEnvironmentFromDiskIfValid(sdkModel) }

  private fun bundledJdk(sdkModel: ProjectSdksModel): JdkLanguageEnvironmentFromDisk? {
    val bundledJdkPath = PathManager.getBundledRuntimeDir().toAbsolutePath().toString()
    return bundledJdkPath.toEnvironmentFromDiskIfValid(sdkModel)
  }

  protected open fun Course.minJvmSdkVersion(): ParsedJavaVersion = course.minJvmSdkVersion

  private fun String.toEnvironmentFromDiskIfValid(sdkModel: ProjectSdksModel): JdkLanguageEnvironmentFromDisk? {
    if (!ExternalSystemJdkUtil.isValidJdk(this)) return null
    val versionInfo = SdkVersionUtil.getJdkVersionInfo(this) ?: return null
    return JdkLanguageEnvironmentFromDisk(
      model = sdkModel,
      homePath = this,
      version = versionInfo.version,
      itemName = versionInfo.displayVersionString(),
      buildSystemSupport = buildSystemSupport
    )
  }

  @VisibleForTesting
  data class SuitableJdkVersions(
    /**
     * The range of suitable jdk versions
     */
    val jdkVersionRange: JdkVersionRange,
    /**
     * The preferred JDK version. Used to suggest the JDK to download
     */
    val preferredJdkVersion: Int?
  )

  @VisibleForTesting
  fun suitableJdkVersions(course: Course): Result<SuitableJdkVersions, String> {
    val minJdkVersion = when (val parsedCourseMinJvmSdkVersion = course.minJvmSdkVersion()) {
      is JavaVersionParseFailed -> return Err(
        EduJVMBundle.message(
          "error.jdk.required.version.unknown",
          parsedCourseMinJvmSdkVersion.versionAsText
        )
      )

      is JavaVersionParseSuccess -> parsedCourseMinJvmSdkVersion.javaSdkVersion.maxLanguageLevel.feature()
      is JavaVersionNotProvided -> null
    }

    val courseJdkVersionRange = JdkVersionRange(minJdkVersion, null)
    val buildSystemVersionRange = when (val buildSystemVersionRangeResult = buildSystemSupport.getJdkVersionRange(course)) {
      is Err -> return Err(buildSystemVersionRangeResult.error)
      is Ok -> buildSystemVersionRangeResult.value
    }

    val jdkVersionRange = courseJdkVersionRange.intersect(buildSystemVersionRange)

    if (jdkVersionRange.isEmpty()) {
      return Err(EduJVMBundle.message(
        "error.jdk.version.range.contradiction",
        courseJdkVersionRange.userString(),
        buildSystemVersionRange.userString()
      ))
    }

    // take version from Course, but if absent, take the best from the build system
    val preferredJdkVersion = minJdkVersion ?: buildSystemVersionRange.max
    // make sure the preferred version is in the range of suitable versions
    val preferredJdkVersionInRange = preferredJdkVersion?.let { jdkVersionRange.nearestVersion(it) }

    return Ok(SuitableJdkVersions(jdkVersionRange, preferredJdkVersionInRange))
  }

  private suspend fun prepareDownloadableJdk(
    sdkModel: ProjectSdksModel,
    jdkVersionRange: JdkVersionRange,
    preferredJdkVersion: Int?
  ): Result<JdkLanguageEnvironment, String> {
    val defaultProject = ProjectManager.getInstance().defaultProject

    val (jdkItem, jdkHome) = JdkDownloadUtil.pickJdkItemAndPath(defaultProject) { item ->
      !item.isPreview && item.matchesJdkMajorVersion(preferredJdkVersion)
    } ?: return Err(
      EduJVMBundle.message(
        "error.jdk.downloadable.not.found",
        preferredJdkVersion ?: EduJVMBundle.message("error.jdk.downloadable.not.found.version.not.provided"),
        jdkVersionRange.userString()
      )
    )

    val task = JdkDownloadUtil.createDownloadTask(defaultProject, jdkItem, jdkHome)
    if (task == null) {
      return Err(EduJVMBundle.message("error.jdk.download.task.create.failed", jdkItem, jdkHome))
    }

    val incompleteSdk = runCatching {
      edtWriteAction {
        sdkModel.createIncompleteSdk(JavaSdk.getInstance(), task, null)
      }
    }.getOrElse { th ->
      rethrowControlFlowException(th)
      LOG.error("Failed to create incomplete downloadable JDK", th)
      return Err(EduJVMBundle.message("error.jdk.incomplete.create.failed"))
    }

    val version = JavaVersion.tryParse(jdkItem.versionString)
                  ?: return Err(EduJVMBundle.message("error.jdk.download.version.unknown", jdkItem.versionString))

    return Ok(
      JdkLanguageEnvironmentInstall(
        sdkModel,
        jdkHome.toAbsolutePath().toString(),
        version,
        jdkItem.fullPresentationWithVendorText,
        buildSystemSupport,
        jdkItem.downloadSizePresentationText,
        incompleteSdk
      )
    )
  }

  private fun JdkItem.matchesJdkMajorVersion(jdkMajorVersion: Int?): Boolean =
    jdkMajorVersion == null || this.jdkMajorVersion == jdkMajorVersion

  companion object {
    private val LOG = logger<JdkLanguageEnvironmentCatalogProvider>()

    private const val DEFAULT_JDK_PROPERTY: String = "project.jdk"
    private const val DEFAULT_JDK_NAME_PROPERTY: String = "project.jdk.name"

    private const val DEFAULT_JDK_NAME: String = "jdk"
  }
}
