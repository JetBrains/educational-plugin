package com.jetbrains.edu.learning.checkio.courseFormat;

import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CheckiOMission extends EduTask {
  public static final String CHECK_IO_MISSION_TASK_TYPE = "checkiO";

  @NotNull
  @Transient // used to get missions from server
  private CheckiOStation myStation;

  @NotNull
  @NonNls
  private String myCode;

  @NotNull
  @NonNls
  private String mySlug;

  private long mySecondsFromLastChangeOnServer;

  public CheckiOMission() {
    myCode = "";
    mySlug = "";
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
  @NonNls
  public String getCode() {
    return myCode;
  }

  public void setCode(@NotNull @NonNls String code) {
    myCode = code;
  }

  @NotNull
  @NonNls
  public String getSlug() {
    return mySlug;
  }

  public void setSlug(@NotNull @NonNls String slug) {
    mySlug = slug;
  }

  @NotNull
  public TaskFile getTaskFile() {
    final Collection<TaskFile> taskFiles = getTaskFiles().values();
    assert !taskFiles.isEmpty();

    return taskFiles.iterator().next();
  }

  @Override
  public void setStatus(@NotNull CheckStatus status) {
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
  @NonNls
  public String getItemType() {
    return CHECK_IO_MISSION_TASK_TYPE;
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
           ", stationName='" + myStation.getName() + "'" +
           ", title='" + getName() + "'" +
           ", secondsPast=" + getSecondsFromLastChangeOnServer() +
           "}";
  }
}
