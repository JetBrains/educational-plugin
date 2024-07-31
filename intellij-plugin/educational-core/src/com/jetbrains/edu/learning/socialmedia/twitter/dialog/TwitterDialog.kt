package com.jetbrains.edu.learning.socialmedia.twitter.dialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.SuggestToPostDialog
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.SuggestToPostDialogPanel
import com.jetbrains.edu.learning.socialmedia.twitter.TwitterSettings

/**
 * Dialog wrapper class with DoNotAsk option for asking user to tweet.
 */
class TwitterDialog(
  project: Project,
  dialogPanelCreator: (Disposable) -> SuggestToPostDialogPanel
) : SuggestToPostDialog(
  project,
  dialogPanelCreator,
  EduCoreBundle.message("twitter.dialog.title"),
  EduCoreBundle.message("twitter.dialog.ok.action"),
  TwitterSettings.getInstance()
)
