package com.jetbrains.edu.learning.checkio.courseFormat;

import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CheckiOMission extends EduTask {
  private int myId;
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
    mySecondsFromLastChangeOnServer = secondsFromLastChangeOnServer;
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

  @NotNull
  public TaskFile getTaskFile() {
    final Collection<TaskFile> taskFiles = getTaskFiles().values();
    assert !taskFiles.isEmpty();

    return taskFiles.iterator().next();
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
    return getId() == other.getId();
  }

  @Override
  public int hashCode() {
    return getId();
  }

  @Override
  public String toString() {
    return "CheckiOMission{" +
           "id=" + getId() +
           ", stationId=" + myStation.getId() +
           ", stationName='" + myStation.getName() + '\'' +
           ", title='" + getName() + '\'' +
           ", secondsPast=" + getSecondsFromLastChangeOnServer() +
           '}';
  }

  public void setId(int id) {
    myId = id;
  }


  public int getId() {
    return myId;
  }

}
