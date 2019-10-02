@file:JvmName("StudyItemMoveUtils")

package com.jetbrains.edu.coursecreator.handlers.move

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.coursecreator.ui.CCMoveStudyItemDialog
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

private var MOCK: MoveStudyItemUI? = null

/** Returns delta */
fun showMoveStudyItemDialog(project: Project, itemName: String, thresholdName: String): Int {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("Mock UI should be set via `withMockMoveStudyItemUI`")
  } else {
    DialogMoveStudyItemUI()
  }
  return ui.showDialog(project, itemName, thresholdName)
}

@TestOnly
fun withMockMoveStudyItemUI(mockUi: MoveStudyItemUI, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  } finally {
    MOCK = null
  }
}

interface MoveStudyItemUI {
  fun showDialog(project: Project, itemName: String, thresholdName: String): Int
}

class DialogMoveStudyItemUI : MoveStudyItemUI {
  override fun showDialog(project: Project, itemName: String, thresholdName: String): Int {
    val dialog = CCMoveStudyItemDialog(project, itemName, thresholdName)
    dialog.show()
    return if (dialog.exitCode != DialogWrapper.OK_EXIT_CODE) -1 else dialog.indexDelta
  }
}
