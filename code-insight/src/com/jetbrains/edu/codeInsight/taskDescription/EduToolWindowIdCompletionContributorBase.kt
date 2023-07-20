package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ToolWindowManager

abstract class EduToolWindowIdCompletionContributorBase : EduUriPathCompletionContributorBase() {

  override fun collectUriPathLookupElements(parameters: CompletionParameters): List<UriPathLookupElement> {
    val toolWindowManager = ToolWindowManager.getInstance(parameters.position.project)
    // In production, each tool window may return its actual ID via `com.intellij.openapi.wm.ToolWindow.getId`
    // But it tests, all tool windows are `com.intellij.toolWindow.ToolWindowHeadlessManagerImpl.MockToolWindow`,
    // which return empty string as id
    return ToolWindowEP.EP_NAME.extensionList.mapNotNull {
      val id = it.id
      val toolWindow = toolWindowManager.getToolWindow(id) ?: return@mapNotNull null
      UriPathLookupElement(
        id,
        // Empty `stripeTitle` may be in tests
        toolWindow.stripeTitle.ifEmpty { id },
        toolWindow.icon
      )
    }
  }
}
