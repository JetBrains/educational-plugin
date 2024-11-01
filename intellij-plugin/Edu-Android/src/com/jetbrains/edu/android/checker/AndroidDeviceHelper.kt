package com.jetbrains.edu.android.checker

import com.android.sdklib.deviceprovisioner.DeviceProvisioner
import com.android.tools.idea.deviceprovisioner.DeviceProvisionerService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class AndroidDeviceHelper(private val project: Project, private val scope: CoroutineScope) {

  fun hasDevices(): Boolean = deviceProvisioner.hasDevices()

  /**
   * Shows a user dialog to create a new device if there is no device
   */
  fun createDeviceIfNeeded() {
    val deviceProvisioner = deviceProvisioner
    // There is at least one device, so no need to force a user to create new one
    if (deviceProvisioner.hasDevices()) return

    scope.launch {
      val actions = deviceProvisioner.createDeviceActions()
      // It seems we don't have access to `com.android.sdklib.deviceprovisioner.LocalEmulatorProvisionerPlugin.getCreateDeviceAction`,
      // so let's try to find it using some heuristic
      val createDeviceAction = actions.firstOrNull { it.presentation.value.label == "Create Virtual Device" } ?: actions.firstOrNull()
      if (createDeviceAction != null) {
        createDeviceAction.create()
      }
      else {
        LOG.warn("Failed to find an action to create a new device. Available actions: ${actions.map { it.presentation.value.label }}")
      }
    }
  }

  private fun DeviceProvisioner.hasDevices(): Boolean = devices.value.isNotEmpty()

  @Suppress("IncorrectServiceRetrieving") // Suppresses false-positive error
  private val deviceProvisioner: DeviceProvisioner get() = project.service<DeviceProvisionerService>().deviceProvisioner

  companion object {
    private val LOG = logger<AndroidDeviceHelper>()

    fun getInstance(project: Project): AndroidDeviceHelper = project.service()
  }
}
