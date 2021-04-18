package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType
import java.awt.BorderLayout
import javax.swing.JPanel


class TabPanel(val tabType: TabType) : JPanel(BorderLayout()), Disposable {
  override fun dispose() {}
}