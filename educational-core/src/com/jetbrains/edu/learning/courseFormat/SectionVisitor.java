package com.jetbrains.edu.learning.courseFormat;


import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SectionVisitor {

  void visit(@NotNull Section section);
}

