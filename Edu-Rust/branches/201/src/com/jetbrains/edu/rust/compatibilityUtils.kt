package com.jetbrains.edu.rust

import org.rust.cargo.toolchain.Cargo

typealias RsToolchain = org.rust.cargo.toolchain.RustToolchain

// BACKCOMPAT: 2020.1. Inline it
fun RsToolchain.rawCargo(): Cargo = rawCargo()
