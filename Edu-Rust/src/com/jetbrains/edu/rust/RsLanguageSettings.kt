package com.jetbrains.edu.rust

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import org.rust.cargo.toolchain.RustToolchain
import java.awt.BorderLayout
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class RsLanguageSettings : LanguageSettings<RsProjectSettings>() {

    private val toolchainLocation = TextFieldWithBrowseButton()
    private var rustToolchain: RustToolchain? = null

    init {
        val toolchain = RustToolchain.suggest()
        if (toolchain != null) {
            toolchainLocation.text = toolchain.location.toString()
            rustToolchain = toolchain
        }
        toolchainLocation.addBrowseFolderListener(
          "Select directory with `rustup` binary", null, null,
          FileChooserDescriptorFactory.createSingleFolderDescriptor(),
          TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        )

        toolchainLocation.childComponent.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val location = toolchainLocation.text
                rustToolchain = if (location.isNotBlank()) RustToolchain(Paths.get(location)) else null
                notifyListeners()
            }
        })
    }

    override fun getSettings(): RsProjectSettings = RsProjectSettings(rustToolchain)

    override fun getLanguageSettingsComponents(course: Course): List<LabeledComponent<JComponent>> {
        return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(toolchainLocation, "Toolchain", BorderLayout.WEST))
    }

    override fun validate(course: Course?): String? {
        val toolchain = rustToolchain
        return when {
            toolchain == null -> "Specify Rust toolchain location"
            !toolchain.looksLikeValidToolchain() -> "Can't find `rustup` in specified location"
            else -> null
        }
    }
}
