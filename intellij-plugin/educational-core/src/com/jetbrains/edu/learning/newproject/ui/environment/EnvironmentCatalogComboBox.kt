package com.jetbrains.edu.learning.newproject.ui.environment

import com.intellij.openapi.ui.ComboBoxWithWidePopup
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.listCellRenderer.LcrInitParams
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalog
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.ListCellRenderer
import javax.swing.plaf.basic.BasicComboBoxEditor

/**
 * A combobox to choose one [LanguageEnvironment] from a [LanguageEnvironmentCatalog].
 */
class EnvironmentCatalogComboBox<E: LanguageEnvironment>(
  private val presenter: LanguageEnvironmentPresenter<E>
): ComboBoxWithWidePopup<E>() {

  private var emptyMessage: String = EduCoreBundle.message("course.dialog.environment.loading.placeholder")
  private val editorComponent: ExtendableTextField = ExtendableTextField()

  init {
    editor = object : BasicComboBoxEditor() {
      override fun createEditorComponent() = this@EnvironmentCatalogComboBox.editorComponent
    }
    isEditable = false

    this.renderer = createCellRenderer()

    ComboboxSpeedSearch.installSpeedSearch(this) {
      """${presenter.name(it)} ${presenter.secondaryText(it)}"""
    }
  }

  @RequiresEdt
  fun setElements(environmentCatalog: LanguageEnvironmentCatalog<E>) {
    model = DefaultComboBoxModel(Vector(environmentCatalog.environments))
    selectedItem = environmentCatalog.recommended
  }

  fun setEmptyElements() {
    emptyMessage = EduCoreBundle.message("course.dialog.environment.empty.placeholder")
  }

  @Suppress("UNCHECKED_CAST")
  val selectedEnvironment: E?
    get() = selectedItem as? E

  private fun createCellRenderer(): ListCellRenderer<E?> = listCellRenderer {
    val value = this.value

    if (value == null) {
      text(emptyMessage) {
        attributes = SimpleTextAttributes.GRAYED_ATTRIBUTES
      }
      return@listCellRenderer
    }

    val icon = presenter.icon(value)
    if (icon != null) {
      icon(icon)
    }

    text(presenter.name(value)) {
      attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES
    }

    val secondaryText = presenter.secondaryText(value)
    if (secondaryText != null) {
      text(secondaryText) {
        attributes = SimpleTextAttributes.GRAYED_ATTRIBUTES
        align = LcrInitParams.Align.RIGHT
      }
    }
  }
}
