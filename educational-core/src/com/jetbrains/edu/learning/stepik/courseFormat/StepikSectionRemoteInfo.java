package com.jetbrains.edu.learning.stepik.courseFormat;

import com.google.gson.annotations.Expose;
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;

public class StepikSectionRemoteInfo implements RemoteInfo {
  @Expose
  private int myId;

  public int getId() {
    return myId;
  }

  public void setId(int id) {
    myId = id;
  }
}
