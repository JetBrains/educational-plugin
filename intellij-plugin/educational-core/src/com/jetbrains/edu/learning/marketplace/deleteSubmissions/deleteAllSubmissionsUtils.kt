package com.jetbrains.edu.learning.marketplace.deleteSubmissions

import org.jetbrains.annotations.TestOnly


interface SubmissionsDeleteDialog {
  fun showWithResult(): Int
}

@TestOnly
fun deleteSubmissionsWithTestDialog(dialog: SubmissionsDeleteDialog, action: () -> Unit) = try {
  AdvancedSubmissionsDeleteDialog.testDialog = dialog
  action()
}
finally {
  AdvancedSubmissionsDeleteDialog.testDialog = null
}
