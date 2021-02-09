package com.jetbrains.edu.learning.actions

import java.util.function.Supplier

fun LeaveCommentAction.addSynonym(text: String) {
  addSynonym(Supplier { text })
}