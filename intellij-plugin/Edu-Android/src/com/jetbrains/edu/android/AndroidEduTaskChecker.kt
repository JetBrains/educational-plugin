package com.jetbrains.edu.android

import com.android.ddmlib.IDevice
import com.android.sdklib.internal.avd.AvdInfo
import com.android.tools.idea.adb.AdbService
import com.android.tools.idea.avdmanager.AvdManagerConnection
import com.android.tools.idea.run.LaunchableAndroidDevice
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.containers.orNull
import com.jetbrains.edu.android.messages.EduAndroidBundle
import com.jetbrains.edu.jvm.gradle.checker.GradleCommandLine
import com.jetbrains.edu.jvm.gradle.checker.GradleEduTaskChecker
import com.jetbrains.edu.jvm.gradle.checker.getGradleProjectName
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class AndroidChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : GradleEduTaskChecker(task, envChecker, project) {

  @Volatile
  private var deviceLaunching: Future<IDevice>? = null

  override fun check(indicator: ProgressIndicator): CheckResult {
    val possibleError = envChecker.getEnvironmentError(project, task)
    if (possibleError != null) {
      return possibleError
    }

    indicator.isIndeterminate = true

    val taskModuleName = getGradleProjectName(task)
    val assembleTask = ":$taskModuleName:assemble"

    indicator.text = EduAndroidBundle.message("building.task")
    val assembleResult = GradleCommandLine.create(project, assembleTask)?.launchAndCheck(indicator) ?: CheckResult.failedToCheck
    if (assembleResult.status != CheckStatus.Solved) return assembleResult

    indicator.text = EduAndroidBundle.message("running.unit.tests")
    val unitTestTask = "$taskModuleName:testDebugUnitTest"
    val unitTestResult = GradleCommandLine.create(project, unitTestTask)?.launchAndCheck(indicator) ?: CheckResult.failedToCheck
    if (unitTestResult.status != CheckStatus.Solved) return unitTestResult

    val hasInstrumentedTests = task.taskFiles.any { (path, _) -> path.startsWith("src/androidTest") && !path.endsWith("AndroidEduTestRunner.kt") }
    if (!hasInstrumentedTests) return unitTestResult

    indicator.text = EduAndroidBundle.message("launching.emulator")
    ApplicationManager.getApplication().invokeAndWait {
      deviceLaunching = launchEmulator()
    }
    val emulatorLaunched = try {
      deviceLaunching?.get() != null
    } catch (e: Exception) {
      LOG.warn("Failed to launch emulator", e)
      false
    }
    if (!emulatorLaunched) return CheckResult.failedToCheck

    indicator.text = EduAndroidBundle.message("running.instrumented.tests")
    val instrumentedTestTask = "$taskModuleName:connectedDebugAndroidTest"
    return GradleCommandLine.create(project, instrumentedTestTask)?.launchAndCheck(indicator) ?: CheckResult.failedToCheck
  }

  override fun clearState() {
    deviceLaunching?.cancel(true)
    deviceLaunching = null
  }

  private fun launchEmulator(): Future<IDevice>? {
    val future = startEmulatorIfExists()
    if (future != null) return future

    Messages.showInfoMessage(
      project,
      EduAndroidBundle.message("error.no.emulator.message"),
      EduAndroidBundle.message("error.no.emulator.title")
    )

    val avdOptionsModel = AvdOptionsModel(null)
    val dialog = AvdWizardUtils.createAvdWizard(null, project, avdOptionsModel)
    return if (dialog.showAndGet()) {
      launchEmulator(avdOptionsModel.createdAvd.orNull())
    }
    else {
      null
    }
  }

  private fun startEmulatorIfExists(): Future<IDevice>? {
    val adbFile = AndroidSdkUtils.getAdb(project)
    if (adbFile == null) {
      LOG.warn("Can't find adbFile location")
      return null
    }
    val abd = AdbService.getInstance().getDebugBridge(adbFile).get()
    val device = abd.devices.find { it.isEmulator && it.avdName != null }
    if (device != null) {
      return CompletableFuture.completedFuture(device)
    }

    for (avd in AvdManagerConnection.getDefaultAvdManagerConnection().getAvds(true)) {
      return launchEmulator(avd) ?: continue
    }
    return null
  }

  private fun launchEmulator(avd: AvdInfo?): Future<IDevice>? {
    return if (avd?.status == AvdInfo.AvdStatus.OK) {
      LaunchableAndroidDevice(avd).launch(project)
    } else {
      null
    }
  }

  companion object {
    private val LOG = Logger.getInstance(AndroidChecker::class.java)
  }
}
