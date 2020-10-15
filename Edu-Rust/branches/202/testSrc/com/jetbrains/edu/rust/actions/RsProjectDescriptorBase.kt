package com.jetbrains.edu.rust.actions

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.Urls
import org.rust.cargo.CfgOptions
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.project.workspace.CargoWorkspaceData
import org.rust.cargo.project.workspace.PackageOrigin
import java.nio.file.Paths

abstract class RsProjectDescriptorBase : LightProjectDescriptor() {

  protected fun testCargoProject(contentRoot: String): CargoWorkspace {
    val packages = listOf(testCargoPackage(contentRoot))
    return CargoWorkspace.deserialize(Paths.get("${Urls.newFromIdea(contentRoot).path}/Cargo.toml"),
                                      CargoWorkspaceData(packages, emptyMap(), emptyMap()), CfgOptions.DEFAULT)
  }

  private fun testCargoPackage(contentRoot: String): CargoWorkspaceData.Package = CargoWorkspaceData.Package(
    id = "task1 0.0.1",
    contentRootUrl = contentRoot,
    name = "task1",
    version = "0.0.1",
    targets = listOf(
      CargoWorkspaceData.Target("$contentRoot/lesson1/task1/main.rs", "task1", CargoWorkspace.TargetKind.Bin,
                                edition = CargoWorkspace.Edition.EDITION_2015, doctest = false, requiredFeatures = emptyList())
    ),
    source = null,
    origin = PackageOrigin.WORKSPACE,
    edition = CargoWorkspace.Edition.EDITION_2018,
    features = emptyMap(),
    cfgOptions = CfgOptions.EMPTY,
    env = emptyMap(),
    outDirUrl = null,
    enabledFeatures = emptySet()
  )
}
