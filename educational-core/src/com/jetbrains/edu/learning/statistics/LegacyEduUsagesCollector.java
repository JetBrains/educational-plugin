package com.jetbrains.edu.learning.statistics;

import com.intellij.internal.statistic.UsagesCollector;
import com.intellij.internal.statistic.beans.GroupDescriptor;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.jetbrains.edu.learning.statistics.EduUsagesCollector.GROUP_ID;

/**
 * To be removed in 2018.2
 * See FrameworkUsageCollector
 */
@Deprecated
public class LegacyEduUsagesCollector extends UsagesCollector {

  @NotNull
  @Override
  public GroupDescriptor getGroupId() {
    return GroupDescriptor.create(GROUP_ID);
  }

  @NotNull
  @Override
  public Set<UsageDescriptor> getUsages() {
    return EduUsagesCollector.collectUsages();
  }
}