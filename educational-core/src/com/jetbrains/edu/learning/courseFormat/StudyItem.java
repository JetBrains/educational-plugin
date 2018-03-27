package com.jetbrains.edu.learning.courseFormat;

import org.jetbrains.annotations.Nullable;

public abstract class StudyItem {
  // from 1 to number of items
  private int myIndex = -1;

  // Non unique lesson/task/section names can be received from stepik. In this case unique directory name is generated,
  // but original non unique name is displayed
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
