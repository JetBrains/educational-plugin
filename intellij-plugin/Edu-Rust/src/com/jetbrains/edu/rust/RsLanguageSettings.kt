package com.jetbrains.edu.rust

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.ui.components.fields.ExtendableTextField
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_RUST
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.ModalityStateProvider
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.rust.messages.EduRustBundle
import org.jetbrains.annotations.VisibleForTesting
import org.rust.cargo.project.RsToolchainPathChoosingComboBox
import org.rust.cargo.toolchain.RsToolchainBase
import org.rust.cargo.toolchain.RsToolchainProvider
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor
import java.awt.BorderLayout
import java.nio.file.Path
import javax.swing.JComponent

class RsLanguageSettings : LanguageSettings<RsProjectSettings>() {

  private var toolchainComboBox: RsToolchainPathChoosingComboBox? = null

  private var loadingFinished: Boolean = false

  private var rustToolchain: RsToolchainBase? = null

  override fun getSettings(): RsProjectSettings = RsProjectSettings(
    rustToolchain,
    toolchainComboBox?.userAsksToInstallRustup() ?: false
  )

  override fun getLanguageSettingsComponents(
    course: Course,
    modalityStateProvider: ModalityStateProvider,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val comboBox = RsToolchainPathChoosingComboBox {
      toolchainComboBox?.let { updateToolchain(it) }
    }

    Disposer.register(disposable, comboBox)
    comboBox.addToolchainsAsync(::findAllToolchainsPath) {
      loadingFinished = true
      if (disposable.isDisposed) return@addToolchainsAsync

      comboBox.updateToolchainsComboBoxPlaceholderText(loadingFinished = true)

      // `RsToolchainPathChoosingComboBox` sets initial empty text after addition of all items
      // But we want to show text of selected item
      val combobox = comboBox.childComponent
      val selectedItem = combobox.selectedItem
      if (selectedItem is Path) {
        comboBox.selectedPath = selectedItem
      }
      updateToolchain(comboBox)
    }

    toolchainComboBox = comboBox
    comboBox.updateToolchainsComboBoxPlaceholderText(loadingFinished = false)

    return listOf(LabeledComponent.create(comboBox, EduRustBundle.message("toolchain.label.text"), BorderLayout.WEST))
  }

  private fun findAllToolchainsPath(): List<Path> {
    checkIsBackgroundThread()
    return RsToolchainFlavor.getApplicableFlavors(null).flatMap { it.suggestHomePaths(null) }.distinct()
  }

  private fun updateToolchain(comboBox: RsToolchainPathChoosingComboBox) {
    // Unfortunately, `RsToolchainPathChoosingComboBox` changes its text before final callback is called
    // To avoid unexpected updates of toolchain, just skip all changes before call of final callback
    if (!loadingFinished) return
    val toolchainPath = comboBox.selectedPath

    // We already have toolchain for this path
    if (rustToolchain?.location != toolchainPath) {
      rustToolchain = toolchainPath?.let { RsToolchainProvider.getToolchain(it) }
    }
    notifyListeners()
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    if (!loadingFinished) return SettingsValidationResult.Pending
    val toolchain = rustToolchain
    val validationMessage = when {
      toolchain == null -> {
        if (toolchainComboBox?.userAsksToInstallRustup() == true) {
          null
        }
        else {
          ValidationMessage(EduRustBundle.message("error.no.toolchain.location", ""), ENVIRONMENT_CONFIGURATION_LINK_RUST)
        }
      }

      !toolchain.looksLikeValidToolchain() -> {
        ValidationMessage(EduRustBundle.message("error.incorrect.toolchain.location"), ENVIRONMENT_CONFIGURATION_LINK_RUST)
      }

      else -> null
    }
    return SettingsValidationResult.Ready(validationMessage)
  }

  private fun RsToolchainPathChoosingComboBox.addToolchainsAsync(
    toolchainObtainer: () -> List<Path>,
    onFinish: () -> Unit
  ) {
    setBusy(true)
    ApplicationManager.getApplication().executeOnPooledThread {
      val toolchainPaths = try {
        toolchainObtainer()
      }
      catch (e: Throwable) {
        LOG.error(e)
        emptyList()
      }
      // `RsToolchainPathChoosingComboBox` is shown inside dialog,
      // so without proper modality state `invokeLater` won't be process until dialog closed
      invokeLater(ModalityState.any()) {
        setToolchains(toolchainPaths)
        setBusy(false)
        onFinish()
      }
    }
  }

  /**
   * Returns the underlying editor component of the toolchain combo box.
   * Assumes the specific implementation of the RsToolchainPathChoosingComboBox.
   */
  @VisibleForTesting
  fun RsToolchainPathChoosingComboBox.editorComponent(): ExtendableTextField {
    return childComponent.editor.editorComponent as ExtendableTextField
  }

  private fun RsToolchainPathChoosingComboBox.updateToolchainsComboBoxPlaceholderText(loadingFinished: Boolean) {
    val emptyText = when {
      !loadingFinished -> EduRustBundle.message("autoinstall.toolchain.placeholder.loading")
      hasDetectedToolchains -> EduRustBundle.message("autoinstall.toolchain.placeholder.select")
      else -> EduRustBundle.message("autoinstall.toolchain.placeholder.install")
    }
    setEmptyText(emptyText)
  }

  private fun RsToolchainPathChoosingComboBox.setEmptyText(emptyText: String) {
    editorComponent().emptyText.text = emptyText
  }

  private val RsToolchainPathChoosingComboBox.hasDetectedToolchains: Boolean
    get() = childComponent.model.size > 0

  private fun RsToolchainPathChoosingComboBox.userAsksToInstallRustup(): Boolean {
    return editorComponent().text.isEmpty() && !hasDetectedToolchains
  }

  companion object {
    private val LOG = logger<RsLanguageSettings>()
  }
}
