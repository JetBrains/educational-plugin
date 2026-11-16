package com.jetbrains.edu.slow.checkerTests

import com.intellij.ide.starter.community.model.BuildType
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.tools.ide.starter.product.clion.CLion
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import com.intellij.tools.ide.starter.product.pycharm.PyCharm
import com.intellij.tools.ide.starter.product.rider.Rider

fun ideaUltimate(): IdeInfo = IdeInfo.IdeaUltimate.withVersionFromSystemProperties()

fun pyCharm(): IdeInfo = IdeInfo.PyCharm.withVersionFromSystemProperties()

fun clion(): IdeInfo = IdeInfo.CLion.withVersionFromSystemProperties()

fun rider(): IdeInfo = IdeInfo.Rider.withVersionFromSystemProperties()

/**
 * Applies the IDE version and build type passed by the `test` Gradle task via system properties
 * keyed by the IDE product code.
 * See `integration-tests/build.gradle.kts`
 */
private fun IdeInfo.withVersionFromSystemProperties(): IdeInfo = copy(
  version = requiredSystemProperty("edu.integration.tests.$productCode.version"),
  buildType = BuildType.valueOf(requiredSystemProperty("edu.integration.tests.$productCode.build.type")).type
)

private fun requiredSystemProperty(name: String): String =
  checkNotNull(System.getProperty(name)) { "System property '$name' is not set" }
