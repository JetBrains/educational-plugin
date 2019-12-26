package com.jetbrains.edu.cpp

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspaceListener
import com.jetbrains.edu.learning.courseDir

// BACKCOMPAT: 2019.2. Delete this class. In 2019.3 all files are refreshed correctly.
// registered in Edu-Cpp.xml
@Suppress("ComponentNotRegistered")
class CppProjectComponent(private val project: Project) : ProjectComponent {
  override fun projectOpened() {
    if (project.isDisposed || !isEduCppProject(project)) {
      return
    }

    project.messageBus.connect().subscribe(CMakeWorkspaceListener.TOPIC, object : CMakeWorkspaceListener {
      override fun reloadingFinished(canceled: Boolean) {
        if (canceled) {
          return
        }

        // Current method is called in WriteAction, so we could use it.
        // You could look at realisation of com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace#doReload method.
        VfsUtil.refreshAndFindChild(project.courseDir, TEST_FRAMEWORK_DIR)?.refresh(true, true)
      }
    })

  }
}