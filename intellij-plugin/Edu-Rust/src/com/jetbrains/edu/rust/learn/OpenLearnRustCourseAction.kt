package com.jetbrains.edu.rust.learn

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import kotlinx.coroutines.launch
import org.rust.cargo.toolchain.RsToolchainBase
import org.rust.cargo.toolchain.RsToolchainProvider
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor
import org.rust.ide.learn.OpenCourseHandler
import java.nio.file.Path

private const val COURSE_ID = 16631

@Suppress("HardCodedStringLiteral") // since it's a temporary internal action, there is no need to localize it
class OpenLearnRustCourseAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.text = if (OpenCourseHandler.isAlreadyStartedCourse(COURSE_ID)) {
      "Open Learn Rust"
    }
    else {
      "Start Learn Rust"
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    currentThreadCoroutineScope().launch {
      val path = RsToolchainFlavor.getApplicableFlavors().flatMap { it.suggestHomePaths() }.first()
      val toolchain = RsToolchainProvider.getToolchain(path) ?: return@launch
      OpenCourseHandler.openCourse(COURSE_ID, toolchain, null)
    }
  }

  private fun OpenCourseHandler.Companion.isAlreadyStartedCourse(courseId: Int): Boolean {
    return OpenCourseHandler.EP.extensions.any { it.isAlreadyStartedCourse(courseId) }
  }

  private suspend fun OpenCourseHandler.Companion.openCourse(courseId: Int, toolchain: RsToolchainBase, projectLocation: Path?) {
    OpenCourseHandler.EP.extensions.filterIsInstance<RsOpenCourseHandler>().firstOrNull()?.openCourse(courseId, toolchain, projectLocation)
  }
}
