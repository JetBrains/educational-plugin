package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor

/**
 * Does nothing
 * It is used to avoid the exception called from the scala plugin
 * when calling [org.jetbrains.bsp.project.importing.BspProjectOpenProcessor] using LightVirtualFiles
 */
class FLProjectOpenProcessor : ProjectOpenProcessor() {
  override val name: String = "Framework Lesson ProjectOpenProcessor"

  override fun canOpenProject(file: VirtualFile): Boolean {
    return isProjectFile(file)
  }

  override fun isProjectFile(file: VirtualFile): Boolean {
    return file is FLLightVirtualFile
  }

  override fun doOpenProject(virtualFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
    return null
  }

  override fun lookForProjectsInDirectory(): Boolean {
    return false
  }
}