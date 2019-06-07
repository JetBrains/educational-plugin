package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector

/**
 * IMPORTANT: if you modify anything in this class, updated whitelist rules should be
 * provided to analytics platform team.
 * See `docs/statisticsRules.md` for more information
 */
class EduStateUsagesCollector : ApplicationUsagesCollector() {

  override fun getUsages(): Set<UsageDescriptor> {
    // do nothing as data from 183 are not available
    return emptySet()
  }

  override fun getGroupId() = "educational.state"

}