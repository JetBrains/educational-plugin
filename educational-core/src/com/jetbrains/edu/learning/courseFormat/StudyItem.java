package com.jetbrains.edu.learning.courseFormat;

import org.jetbrains.annotations.Nullable;

public abstract class StudyItem {
  // index is visible to user number of lesson from 1 to lesson number
  private int myIndex = -1;

  @Nullable private String myCustomPresentableName = null;

  //TODO: move name to this class
  // can't do it now because name in descendants have different serialized names and it will cause additional migration

  abstract public String getName();
  abstract public void setName(String name);

  /**
   *
   * @deprecated Should be used only for deserialization. Use {@link StudyItem#getPresentableName()} instead
   */
  @Deprecated
  @Nullable
  public String getCustomPresentableName() {
    return myCustomPresentableName;
  }

  public void setCustomPresentableName(@Nullable String customPresentableName) {
    myCustomPresentableName = customPresentableName;
  }

  public String getPresentableName() {
    return myCustomPresentableName != null ? myCustomPresentableName : getName();
  }

  public int getIndex() {
    return myIndex;
  }

  public void setIndex(int index) {
    myIndex = index;
  }
}
