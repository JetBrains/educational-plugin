package com.jetbrains.edu.rust

import com.jetbrains.edu.learning.newproject.EduProjectSettings
import org.rust.cargo.toolchain.RsToolchainBase

// TODO rewrite as a sealed class with options: InstallToolchain, and WithToolchain
data class RsProjectSettings(val toolchain: RsToolchainBase? = null, val installRustup: Boolean = false) : EduProjectSettings
