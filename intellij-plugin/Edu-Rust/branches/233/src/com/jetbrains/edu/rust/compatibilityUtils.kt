package com.jetbrains.edu.rust

import com.intellij.openapi.Disposable
import org.rust.cargo.project.RsToolchainPathChoosingComboBox

fun RsToolchainPathChoosingComboBox(
  @Suppress("UNUSED_PARAMETER") parentDisposable: Disposable,
  onTextChanged: () -> Unit
): RsToolchainPathChoosingComboBox = RsToolchainPathChoosingComboBox(onTextChanged)
