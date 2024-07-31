package com.jetbrains.edu.learning.socialmedia.linkedIn.dialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.socialmedia.linkedIn.LinkedInSettings
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.SuggestToPostDialog
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.SuggestToPostDialogPanel

/**
 * Dialog wrapper class with DoNotAsk option for asking user to tweet.
 */
class LinkedInDialog(
  project: Project,
  dialogPanelCreator: (Disposable) -> SuggestToPostDialogPanel
) :
  SuggestToPostDialog(
    project,
    dialogPanelCreator,
    EduCoreBundle.message("linkedin.dialog.title"),
    EduCoreBundle.message("linkedin.post.button.text"),
    LinkedInSettings.getInstance()
  )