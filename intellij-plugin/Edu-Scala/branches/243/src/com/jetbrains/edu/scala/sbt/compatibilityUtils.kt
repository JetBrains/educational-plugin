package com.jetbrains.edu.scala.sbt

import org.jetbrains.plugins.scala.project.Version
import org.jetbrains.sbt.Sbt

val latestSbtVersion: Version get() = Sbt.LatestVersion()
