package com.jetbrains.edu.aiHints.core.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.jetbrains.edu.aiHints.core.ui.CodeHintInlineBanner
import com.jetbrains.edu.aiHints.core.ui.HintInlineBanner
import com.jetbrains.edu.aiHints.core.ui.TextHintInlineBanner
import com.jetbrains.edu.learning.statistics.EventLogGroup
import com.jetbrains.edu.learning.statistics.enumField
import com.jetbrains.edu.learning.statistics.registerEvent

class EduAIHintsCounterUsageCollector : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  enum class HintBannerType {
    CODE,
    TEXT,
    ERROR;

    companion object {
      @JvmStatic
      fun from(banner: HintInlineBanner): HintBannerType = when (banner) {
        is CodeHintInlineBanner -> CODE
        is TextHintInlineBanner -> TEXT
        else -> ERROR
      }
    }
  }

  @Suppress("CompanionObjectInExtension")
  companion object {
    private const val TYPE: String = "type"

    private val GROUP = EventLogGroup(
      id = "educational.ai.hints",
      description = "The group that gathers all records related to AI Hints feature in the JetBrains Academy plugin.",
      version = 1
    )

    private val GET_HINT = GROUP.registerEvent(
      "get.hint",
      "The event is recorded when Get Hint button is clicked"
    )
    private val HINT_BANNER_SHOWN = GROUP.registerEvent(
      "hint.banner.shown",
      "Represents the type of banner that is shown to the user",
      enumField<HintBannerType>(TYPE)
    )
    private val HINT_BANNER_CLOSED = GROUP.registerEvent(
      "hint.banner.closed",
      "The event is recorded when the hint banner is closed by user"
    )
    private val SHOW_IN_CODE_LINK_CLICKED = GROUP.registerEvent(
      "show.in.code.clicked",
      "The event is recorded `Show in code` link is clicked from the CodeHint banner",
    )
    private val CODE_HINT_ACCEPTED = GROUP.registerEvent(
      "code.hint.accepted",
      "The event is recorded in case the shown CodeHint was accepted"
    )
    private val CODE_HINT_DECLINED = GROUP.registerEvent(
      "code.hint.declined",
      "The event is recorded in case the shown CodeHint was declined"
    )

    fun getHint() = GET_HINT.log()

    fun hintBannerShown(type: HintBannerType) = HINT_BANNER_SHOWN.log(type)

    fun hintBannerClosed() = HINT_BANNER_CLOSED.log()

    fun showInCodeClicked() = SHOW_IN_CODE_LINK_CLICKED.log()

    fun codeHintAccepted() = CODE_HINT_ACCEPTED.log()

    fun codeHintDeclined() = CODE_HINT_DECLINED.log()
  }
}