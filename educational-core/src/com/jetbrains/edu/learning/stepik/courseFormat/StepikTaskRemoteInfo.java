package com.jetbrains.edu.learning.stepik.courseFormat;

import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;

import java.util.Date;

public class StepikTaskRemoteInfo implements RemoteInfo {
  private int myStepId;
  private Date myUpdateDate = new Date(0);


  public void setStepId(int stepId) {
    myStepId = stepId;
  }

  public int getStepId() {
    return myStepId;
  }

  public void setUpdateDate(Date date) {
    myUpdateDate = date;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

}
