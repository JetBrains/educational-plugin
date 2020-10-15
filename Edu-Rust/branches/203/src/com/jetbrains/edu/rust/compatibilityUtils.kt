package com.jetbrains.edu.rust

import org.rust.cargo.toolchain.tools.Cargo
import org.rust.cargo.toolchain.tools.cargo

typealias RsToolchain = org.rust.cargo.toolchain.RsToolchain

// BACKCOMPAT: 2020.1. Inline it
fun RsToolchain.rawCargo(): Cargo = cargo()
