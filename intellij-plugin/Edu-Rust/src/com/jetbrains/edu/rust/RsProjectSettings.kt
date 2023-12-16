package com.jetbrains.edu.rust

import com.jetbrains.edu.learning.newproject.EduProjectSettings
import org.rust.cargo.toolchain.RsToolchainBase

data class RsProjectSettings(val toolchain: RsToolchainBase? = null) : EduProjectSettings
