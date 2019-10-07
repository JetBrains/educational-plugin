package com.jetbrains.edu.learning.courseFormat.tasks;

import org.jetbrains.annotations.NotNull;

public class CodeTask extends Task {
  @SuppressWarnings("unused") //used for deserialization
  public CodeTask() {}

  public CodeTask(@NotNull final String name) {
    super(name);
  }

  @Override
  public String getItemType() {
    return "code";
  }

  @Override
  public boolean supportSubmissions() {
    return true;
  }
}
