package com.jetbrains.edu.learning.courseFormat.tasks;

import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class TheoryTask extends Task {
  @SuppressWarnings("unused") //used for deserialization
  public TheoryTask() {}

  public TheoryTask(@NotNull final String name, int id, int position, @NotNull Date updateDate, @NotNull CheckStatus status) {
    super(name, id, position, updateDate, status);
  }

  @Override
  public String getItemType() {
    return "theory";
  }
}
