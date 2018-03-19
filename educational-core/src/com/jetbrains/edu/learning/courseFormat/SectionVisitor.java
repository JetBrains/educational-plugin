package com.jetbrains.edu.learning.courseFormat;


import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SectionVisitor {

  /**
   * @return true to continue visiting sections, false -- to abort
   */
  boolean visit(@NotNull Section section);
}

