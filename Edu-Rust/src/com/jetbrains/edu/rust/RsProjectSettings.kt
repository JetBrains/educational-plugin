package com.jetbrains.edu.rust

import org.rust.cargo.toolchain.RsToolchain

data class RsProjectSettings(val toolchain: RsToolchain? = null)
