package com.jetbrains.edu.learning.courseFormat.visitors;


import com.jetbrains.edu.learning.courseFormat.Section;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SectionVisitor {
  void visit(@NotNull Section section);
}

