package com.jetbrains.edu.rust

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.UserDataHolder
import com.intellij.ui.DocumentAdapter
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_RUST
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.rust.messages.EduRustBundle
import org.rust.cargo.toolchain.RsToolchain
import java.awt.BorderLayout
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class RsLanguageSettings : LanguageSettings<RsProjectSettings>() {

  private val toolchainLocation = TextFieldWithBrowseButton()
  private var rustToolchain: RsToolchain? = null

  init {
    val toolchain = RsToolchain.suggest()
    if (toolchain != null) {
      toolchainLocation.text = toolchain.location.toString()
      rustToolchain = toolchain
    }
    toolchainLocation.addBrowseFolderListener(
      EduRustBundle.message("select.rustup.binary"), null, null,
      FileChooserDescriptorFactory.createSingleFolderDescriptor(),
      TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
    )

    toolchainLocation.childComponent.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        val location = toolchainLocation.text
        rustToolchain = if (location.isNotBlank()) RsToolchain(Paths.get(location)) else null
        notifyListeners()
      }
    })
  }

  override fun getSettings(): RsProjectSettings = RsProjectSettings(rustToolchain)

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: Disposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(toolchainLocation, EduRustBundle.message("toolchain.label.text"), BorderLayout.WEST))
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    val toolchain = rustToolchain
    val validationMessage = when {
      toolchain == null -> {
        ValidationMessage(EduRustBundle.message("error.no.toolchain.location", ""), ENVIRONMENT_CONFIGURATION_LINK_RUST)
      }
      !toolchain.looksLikeValidToolchain() -> {
        ValidationMessage(EduRustBundle.message("error.incorrect.toolchain.location"), ENVIRONMENT_CONFIGURATION_LINK_RUST)
      }
      else -> null
    }
    return SettingsValidationResult.Ready(validationMessage)
  }
}
