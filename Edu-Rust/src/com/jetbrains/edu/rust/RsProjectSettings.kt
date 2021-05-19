package com.jetbrains.edu.rust

import org.rust.cargo.toolchain.RsToolchainBase

data class RsProjectSettings(val toolchain: RsToolchainBase? = null)
