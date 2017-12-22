package com.jetbrains.edu.learning.courseFormat.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

public class TaskWithSubtasks extends EduTask {
  @SerializedName("active_subtask_index")
  private int myActiveSubtaskIndex = 0;

  @SerializedName("last_subtask_index")
  @Expose private int myLastSubtaskIndex = 0;

  public TaskWithSubtasks() {}

  public TaskWithSubtasks(@NotNull final String name) {
    super(name);
  }

  @Override
  protected String getTaskDescriptionName() {
    return super.getTaskDescriptionName() + EduNames.SUBTASK_MARKER + myActiveSubtaskIndex;
  }

  public TaskWithSubtasks(Task task) {
    copyTaskParameters(task);
  }

  public int getActiveSubtaskIndex() {
    return myActiveSubtaskIndex;
  }

  public void setActiveSubtaskIndex(int activeSubtaskIndex) {
    myActiveSubtaskIndex = activeSubtaskIndex;
  }

  public int getLastSubtaskIndex() {
    return myLastSubtaskIndex;
  }

  public void setLastSubtaskIndex(int lastSubtaskIndex) {
    myLastSubtaskIndex = lastSubtaskIndex;
  }

  public void setStatus(CheckStatus status) {
    for (TaskFile taskFile : taskFiles.values()) {
      for (AnswerPlaceholder placeholder : taskFile.getActivePlaceholders()) {
        placeholder.setStatus(status);
      }
    }
    if (status == CheckStatus.Solved) {
      if (activeSubtaskNotLast()) {
        if (myStatus == CheckStatus.Failed) {
          myStatus = CheckStatus.Unchecked;
        }
      }
      else {
        myStatus = CheckStatus.Solved;
      }
    }
  }

  public boolean activeSubtaskNotLast() {
    return getActiveSubtaskIndex() != getLastSubtaskIndex();
  }

  public String getTaskType() {
    return "subtasks";
  }

  @Override
  public boolean isToSubmitToStepik() {
    if (myStatus == CheckStatus.Unchecked) {
      if (myActiveSubtaskIndex > 0) {
        return true;
      }

      return taskFiles.values().stream()
        .flatMap(taskFile -> taskFile.getAnswerPlaceholders().stream())
        .anyMatch(placeholder -> placeholder.getStatus() != CheckStatus.Unchecked);
    }

    return true;
  }
}
