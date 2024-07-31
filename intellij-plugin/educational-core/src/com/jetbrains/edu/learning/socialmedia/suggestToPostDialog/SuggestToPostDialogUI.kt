package com.jetbrains.edu.learning.socialmedia.suggestToPostDialog

interface SuggestToPostDialogUI {
  val message: String
  fun showAndGet(): Boolean
}