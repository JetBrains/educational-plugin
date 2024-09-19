package com.jetbrains.edu.learning.submissions.ui.segmentedButton

import com.jetbrains.edu.learning.messages.EduCoreBundle


class CommunitySegmentedButtonItem : SegmentedButtonItem("submissions.button.community") {
  override val text: String
    get() = EduCoreBundle.message(nameId)
}