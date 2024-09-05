package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl

class EduToolWindowHeadlessManager(private val project: Project) : ToolWindowHeadlessManagerImpl(project) {

  private val toolWindowsMap: MutableMap<String, ToolWindow> = mutableMapOf()

  override val toolWindowIds: Array<String>
    get() = toolWindowsMap.keys.toTypedArray()
  override val toolWindowIdSet: Set<String>
    get() = toolWindowIds.toSet()
  override val toolWindows: List<ToolWindow>
    get() = toolWindowsMap.values.toList()

  override fun doRegisterToolWindow(id: String): ToolWindow {
    val toolWindow = EduMockToolWindow(id, project)
    toolWindowsMap[id] = toolWindow
    return toolWindow
  }

  @Suppress("OVERRIDE_DEPRECATION")
  override fun unregisterToolWindow(id: String) {
    toolWindowsMap.remove(id)
  }

  override fun getToolWindow(id: String?): ToolWindow? = toolWindowsMap[id]
}

private class EduMockToolWindow(private val id: String, project: Project) : ToolWindowHeadlessManagerImpl.MockToolWindow(project) {

  private var isShown: Boolean = false

  override fun show(runnable: Runnable?) {
    isShown = true
  }

  override fun hide(runnable: Runnable?) {
    isShown = false
  }

  override fun isVisible(): Boolean = isShown
  override fun getId(): String = id
}
