package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface TabManager : Disposable {
  fun getTab(tabType: TabType): AdditionalTab
  fun isShowing(tabType: TabType): Boolean
  fun selectTab(tabType: TabType)
  fun updateTab(tabType: TabType, task: Task?)
  fun updateTabs(task: Task?)
}