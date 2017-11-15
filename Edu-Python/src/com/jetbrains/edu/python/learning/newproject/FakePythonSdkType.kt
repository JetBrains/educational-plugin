package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.util.IconLoader
import icons.PythonIcons
import org.jdom.Element
import javax.swing.Icon

internal object FakePythonSdkType : SdkType("") {
  override fun getPresentableName(): String = ""
  override fun isValidSdkHome(path: String?): Boolean = false
  override fun suggestSdkName(currentSdkName: String?, sdkHome: String?): String = ""
  override fun suggestHomePath(): String? = null
  override fun createAdditionalDataConfigurable(sdkModel: SdkModel,
                                                sdkModificator: SdkModificator): AdditionalDataConfigurable? = null
  override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {}
  override fun getIcon(): Icon = IconLoader.getTransparentIcon(PythonIcons.Python.Virtualenv)
}
