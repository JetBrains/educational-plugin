package com.jetbrains.edu.learning.checkio.courseFormat;

import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class CheckiOMission extends EduTask {
  @NotNull
  @Transient
  private CheckiOStation myStation;

  @NotNull
  private String myCode;

  private long mySecondsFromLastChangeOnServer;

  public CheckiOMission() {
    myCode = "";
    myStation = new CheckiOStation();
  }

  @Transient
  @NotNull
  public CheckiOStation getStation() {
    return myStation;
  }

  @Transient
  public void setStation(@NotNull CheckiOStation station) {
    myStation = station;
  }

  public void setSecondsFromLastChangeOnServer(long secondsFromLastChangeOnServer) {
    // zero value means that this mission hasn't been started on CheckiO yet,
    // in this case we should use local task file content after course updating,
    // so time from any local change must be less than from server
    this.mySecondsFromLastChangeOnServer = (secondsFromLastChangeOnServer == 0 ? Long.MAX_VALUE : secondsFromLastChangeOnServer);
  }

  public long getSecondsFromLastChangeOnServer() {
    return mySecondsFromLastChangeOnServer;
  }

  @NotNull
  public String getCode() {
    return myCode;
  }

  public void setCode(@NotNull String code) {
    this.myCode = code;
  }

  @Nullable
  public TaskFile getTaskFile() {
    final Collection<TaskFile> taskFiles = getTaskFiles().values();
    return taskFiles.isEmpty() ? null : taskFiles.iterator().next();
  }

  @Override
  public void setStatus(CheckStatus status) {
    if (myStatus == CheckStatus.Unchecked) {
      myStatus = status;
    }
    else if (myStatus == CheckStatus.Failed && status == CheckStatus.Solved) {
      myStatus = CheckStatus.Solved;
    }
  }

  @Override
  public boolean isToSubmitToStepik() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CheckiOMission other = (CheckiOMission)o;
    return getStepId() == other.getStepId();
  }

  @Override
  public int hashCode() {
    return getStepId();
  }

  @Override
  public String toString() {
    return "CheckiOMissionWrapper{" +
           "id=" + getId() +
           ", stationId=" + myStation.getId() +
           ", stationName='" + myStation.getName() + '\'' +
           ", title='" + getName() + '\'' +
           ", secondsPast=" + getSecondsFromLastChangeOnServer() +
           '}';
  }
}
