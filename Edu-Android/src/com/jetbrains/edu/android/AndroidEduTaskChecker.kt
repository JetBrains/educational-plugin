package com.jetbrains.edu.android

import com.android.ddmlib.IDevice
import com.android.sdklib.internal.avd.AvdInfo
import com.android.tools.idea.adb.AdbService
import com.android.tools.idea.avdmanager.AvdManagerConnection
import com.android.tools.idea.avdmanager.AvdOptionsModel
import com.android.tools.idea.avdmanager.AvdWizardUtils
import com.android.tools.idea.run.LaunchableAndroidDevice
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.gradle.GradleCommandLine
import com.jetbrains.edu.learning.checker.gradle.GradleEduTaskChecker
import com.jetbrains.edu.learning.checker.gradle.getGradleProjectName
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class AndroidChecker(task: EduTask, project: Project) : GradleEduTaskChecker(task, project) {

  @Volatile
  private var deviceLaunching: Future<IDevice>? = null

  override fun check(indicator: ProgressIndicator): CheckResult {
    indicator.isIndeterminate = true

    val taskModuleName = getGradleProjectName(task)
    val assembleTask = "$taskModuleName:assemble"

    indicator.text = "Building task..."
    val assembleResult = GradleCommandLine.create(project, assembleTask)?.launchAndCheck() ?: CheckResult.FAILED_TO_CHECK
    if (assembleResult.status != CheckStatus.Solved) return assembleResult

    indicator.text = "Running unit tests..."
    val unitTestTask = "$taskModuleName:testDebugUnitTest"
    val unitTestResult = GradleCommandLine.create(project, unitTestTask)?.launchAndCheck() ?: CheckResult.FAILED_TO_CHECK
    if (unitTestResult.status != CheckStatus.Solved) return unitTestResult

    val hasInstrumentedTests = task.taskFiles.any { (path, _) -> path.startsWith("src/androidTest") && !path.endsWith("AndroidEduTestRunner.kt") }
    if (!hasInstrumentedTests) return unitTestResult

    indicator.text = "Launching emulator..."
    ApplicationManager.getApplication().invokeAndWait {
      deviceLaunching = launchEmulator()
    }
    val emulatorLaunched = try {
      deviceLaunching?.get() != null
    } catch (e: Exception) {
      LOG.warn("Failed to launch emulator", e)
      false
    }
    if (!emulatorLaunched) return CheckResult.FAILED_TO_CHECK

    indicator.text = "Running instrumented tests..."
    val instrumentedTestTask = "$taskModuleName:connectedDebugAndroidTest"
    return GradleCommandLine.create(project, instrumentedTestTask)?.launchAndCheck() ?: CheckResult.FAILED_TO_CHECK
  }

  override fun clearState() {
    deviceLaunching?.cancel(true)
    deviceLaunching = null
  }

  private fun launchEmulator(): Future<IDevice>? {
    val future = startEmulatorIfExists()
    if (future != null) return future

    Messages.showInfoMessage(project, "Android emulator is required to check tasks. New emulator will be created and launched.",
                             "Android Emulator not Found")

    val avdOptionsModel = AvdOptionsModel(null)
    val dialog = AvdWizardUtils.createAvdWizard(null, project, avdOptionsModel)
    return if (dialog.showAndGet()) {
      launchEmulator(avdOptionsModel.createdAvd)
    } else {
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

  private fun launchEmulator(avd: AvdInfo): Future<IDevice>? {
    return if (avd.status == AvdInfo.AvdStatus.OK) {
      LaunchableAndroidDevice(avd).launch(project)
    } else {
      null
    }
  }

  companion object {
    private val LOG = Logger.getInstance(AndroidChecker::class.java)
  }
}
