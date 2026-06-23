package com.jetbrains.edu.learning.newproject.ui.newCourseSettings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettings
import com.jetbrains.edu.learning.newproject.newCourseSettings.NewCourseSettingsCatalog

/**
 * A combobox to choose one [NewCourseSettings] from a [NewCourseSettingsCatalog.List].
 */
class NewCourseSettingsComboBox<S : NewCourseSettings>(
  private val catalog: NewCourseSettingsCatalog.List<S>, private val presentation: NewCourseSettingsListPresenter<S>
) : ComboBox<S>(CollectionComboBoxModel(catalog.configs)) {

  @Suppress("UNCHECKED_CAST")
  val selectedSettings: S
    get() = selectedItem as? S ?: catalog.preferred

  init {
    selectedItem = catalog.preferred

    renderer = listCellRenderer {
      val value = this.value
      val icon = presentation.icon(value)
      if (icon != null) {
        icon(icon)
      }
      text(presentation.name(value))
    }
  }
}