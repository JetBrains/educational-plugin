package com.jetbrains.edu.python.learning

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.python.learning.newproject.PySdkSettingsHelper
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater
import javax.swing.JComponent

class PyIdeaSdkSettingsHelper : PySdkSettingsHelper {

  override fun isAvailable(): Boolean = !(PlatformUtils.isPyCharm() || PlatformUtils.isCLion())

  override fun getInterpreterComboBox(fakeSdk: Sdk?, onSdkSelected: (Sdk?) -> Unit): JComponent {
    val project = ProjectManager.getInstance().defaultProject
    val model = ProjectSdksModel()
    model.reset(project)
    if (fakeSdk != null) {
      model.addSdk(fakeSdk)
    }

    model.addListener(object : SdkModel.Listener {
      override fun sdkAdded(sdk: Sdk) = SdkConfigurationUtil.addSdk(sdk)
      override fun beforeSdkRemove(sdk: Sdk) {}
      override fun sdkChanged(sdk: Sdk, previousName: String) {}
      override fun sdkHomeSelected(sdk: Sdk, newSdkHome: String) {}
    })

    val sdkTypeIdFilter = Condition<SdkTypeId> { it == PythonSdkType.getInstance() }
    val sdkFilter = JdkComboBox.getSdkFilter(sdkTypeIdFilter)
    val comboBox = JdkComboBox(null, model, sdkTypeIdFilter, sdkFilter, sdkTypeIdFilter, null)
    comboBox.addActionListener {
      onSdkSelected(comboBox.selectedJdk)
    }

    if (fakeSdk != null) {
      comboBox.selectedJdk = fakeSdk
    }
    else {
      onSdkSelected(comboBox.selectedJdk)
    }

    return comboBox
  }

  override fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
    if (sdk !is PyDetectedSdk) {
      return sdk
    }
    val name = sdk.name
    val sdkHome = WriteAction.compute<VirtualFile, RuntimeException> { LocalFileSystem.getInstance().refreshAndFindFileByPath(name) }
    val newSdk = SdkConfigurationUtil.createAndAddSDK(sdkHome.path, PythonSdkType.getInstance())
    if (newSdk != null) {
      @Suppress("UnstableApiUsage")
      PythonSdkUpdater.updateOrShowError(newSdk, project, null)
      SdkConfigurationUtil.addSdk(newSdk)
    }
    return newSdk
  }

  override fun getAllSdks(): List<Sdk> = ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance())
}
