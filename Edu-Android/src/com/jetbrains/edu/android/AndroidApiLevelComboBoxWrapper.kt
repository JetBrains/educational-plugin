package com.jetbrains.edu.android

import com.android.tools.idea.npw.FormFactor
import com.android.tools.idea.npw.module.AndroidApiLevelComboBox
import com.android.tools.idea.npw.platform.AndroidVersionsInfo
import javax.swing.JComboBox

class AndroidApiLevelComboBoxWrapper {

  private val _combobox: AndroidApiLevelComboBox = AndroidApiLevelComboBox()

  val combobox: JComboBox<AndroidVersionsInfo.VersionItem?> get() = _combobox

  fun init(formFactor: FormFactor, items: List<AndroidVersionsInfo.VersionItem>) {
    _combobox.init(formFactor, items)
  }
}
