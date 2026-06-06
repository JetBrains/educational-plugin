package com.jetbrains.edu.rust

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.rust.cargo.project.model.CargoProject
import org.rust.cargo.project.model.RustcInfo
import org.rust.cargo.project.model.impl.CargoProjectImpl
import org.rust.cargo.project.model.impl.CargoProjectsServiceImpl
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.openapiext.pathAsPath
import kotlin.time.Duration.Companion.minutes

class TestCargoProjectsServiceImpl(project: Project, cs: CoroutineScope) : CargoProjectsServiceImpl(project, cs) {

  fun createTestProject(rootDir: VirtualFile, ws: CargoWorkspace, rustcInfo: RustcInfo? = null) {
    val manifest = rootDir.pathAsPath.resolve("Cargo.toml")
    val testProject = CargoProjectImpl(
      manifest, this, emptyMap(), ws, null, rustcInfo,
      workspaceStatus = CargoProject.UpdateStatus.UpToDate,
      rustcInfoStatus = if (rustcInfo != null) CargoProject.UpdateStatus.UpToDate else CargoProject.UpdateStatus.NeedsUpdate
    )
    testProject.setRootDir(rootDir)
    launchAndWait {
      modifyProjects { listOf(testProject) }.await()
    }
  }

  private fun <T> launchAndWait(timeout: Long = 2.minutes.inWholeMilliseconds, call: suspend () -> T): T {
    return cs.async(CoroutineName("TestCargoProjectsServiceImpl.launchAndWait")) { call() }.asCompletableFuture().let {
      PlatformTestUtil.waitForFuture(it, timeout)
    }
  }
}
