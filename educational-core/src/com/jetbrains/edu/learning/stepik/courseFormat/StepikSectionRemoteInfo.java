package com.jetbrains.edu.learning.stepik.courseFormat;

import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;

import java.util.Date;
import java.util.List;

public class StepikSectionRemoteInfo implements RemoteInfo {
  private int myId;
  private List<Integer> myUnits;
  private int myCourseId;
  private int position;
  private Date myUpdateDate = new Date(0);

  public int getId() {
    return myId;
  }

  public void setId(int id) {
    myId = id;
  }

  public List<Integer> getUnits() {
    return myUnits;
  }

  public void setUnits(List<Integer> units) {
    myUnits = units;
  }

  public void setCourseId(int courseId) {
    myCourseId = courseId;
  }

  public int getCourseId() {
    return myCourseId;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public int getPosition() {
    return position;
  }

  public void setUpdateDate(Date updateDate) {
    myUpdateDate = updateDate;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }
}
