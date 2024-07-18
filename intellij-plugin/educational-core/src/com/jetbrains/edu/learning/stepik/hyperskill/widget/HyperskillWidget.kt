package com.jetbrains.edu.learning.stepik.hyperskill.widget

import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.api.EduOAuthCodeFlowConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.profileUrl
import com.jetbrains.edu.learning.stepik.hyperskill.update.SyncHyperskillCourseAction

class HyperskillWidget(project: Project) : LoginWidget<HyperskillAccount>(
  project,
  EduCoreBundle.message("hyperskill.widget.title"),
  EduCoreBundle.message("hyperskill.widget.tooltip"),
  EducationalCoreIcons.Platform.JetBrainsAcademy
) {
  override val connector: EduOAuthCodeFlowConnector<HyperskillAccount, *>
    get() = HyperskillConnector.getInstance()

  override val synchronizeCourseActionId: String
    get() = SyncHyperskillCourseAction.ACTION_ID

  override fun profileUrl(account: HyperskillAccount): String = account.profileUrl

  override fun ID() = "HyperskillAccountWidget"

}
