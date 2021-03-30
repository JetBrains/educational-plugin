package com.jetbrains.edu.learning.courseFormat.tasks;

import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class TheoryTask extends Task {
  public static final String THEORY_TASK_TYPE = "theory";

  @SuppressWarnings("unused") //used for deserialization
  public TheoryTask() {}

  public TheoryTask(@NotNull final String name) {
    super(name);
  }

  public TheoryTask(@NotNull final String name,
                    int id,
                    int position,
                    @NotNull Date updateDate,
                    @NotNull CheckStatus status) {
    super(name, id, position, updateDate, status);
  }

  // needed to prohibit post empty submission at unsupported tasks opening (sorting, matching, text, etc)
  public boolean postSubmissionOnOpen = true;

  @Override
  public String getItemType() {
    return THEORY_TASK_TYPE;
  }
}
