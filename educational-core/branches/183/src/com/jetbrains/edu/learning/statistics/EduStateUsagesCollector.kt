package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector

class EduStateUsagesCollector : ApplicationUsagesCollector() {

  override fun getUsages(): Set<UsageDescriptor> {
    // do nothing as data from 183 are not available
    return emptySet()
  }

  override fun getGroupId() = "educational.state"

}