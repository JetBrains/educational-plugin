package com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo;

import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;

import java.util.Date;
import java.util.List;

public class StepikLessonRemoteInfo implements RemoteInfo {
  private int myId;
  private List<Integer> mySteps;
  private boolean myIsPublic;
  private Date myUpdateDate = new Date(0);
  private int myUnitId = 0;

  public int getId() {
    return myId;
  }

  public void setId(int id) {
    myId = id;
  }

  public List<Integer> getSteps() {
    return mySteps;
  }

  public void setSteps(List<Integer> steps) {
    mySteps = steps;
  }

  public boolean isPublic() {
    return myIsPublic;
  }

  public void setPublic(boolean aPublic) {
    myIsPublic = aPublic;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  public void setUpdateDate(Date updateDate) {
    myUpdateDate = updateDate;
  }

  public int getUnitId() {
    return myUnitId;
  }

  public void setUnitId(int unitId) {
    myUnitId = unitId;
  }
}
