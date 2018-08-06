package com.jetbrains.edu.learning.checkio.courseFormat;

import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class CheckiOMission extends EduTask {
  @Transient private CheckiOStation myStation;

  private String code;
  private long secondsFromLastChangeOnServer;

  public CheckiOMission() {  }

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
    // zero value means that this mission hasn't start on checkio yet,
    // so any local changes have to be after that
    this.secondsFromLastChangeOnServer = (secondsFromLastChangeOnServer == 0 ? Long.MAX_VALUE : secondsFromLastChangeOnServer);
  }

  public long getSecondsFromLastChangeOnServer() {
    return secondsFromLastChangeOnServer;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
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
    } else if (myStatus == CheckStatus.Failed && status == CheckStatus.Solved) {
      myStatus = CheckStatus.Solved;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CheckiOMission other = (CheckiOMission) o;
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
