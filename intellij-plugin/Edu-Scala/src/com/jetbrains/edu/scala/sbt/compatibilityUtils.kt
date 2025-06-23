package com.jetbrains.edu.scala.sbt

import org.jetbrains.plugins.scala.project.Version
import org.jetbrains.sbt.SbtVersion

// BACKCOMPAT: 2024.3
val latestSbtVersion: Version get() = SbtVersion.`Latest$`.`MODULE$`.Sbt_1().value()
