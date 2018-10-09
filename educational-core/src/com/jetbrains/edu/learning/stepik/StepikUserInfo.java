package com.jetbrains.edu.learning.stepik;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class StepikUserInfo {
  private int id = -1;
  @SerializedName("first_name") private String myFirstName;
  @SerializedName("last_name") private String myLastName;
  private boolean isGuest;

  private StepikUserInfo() {
    myFirstName = "";
    myLastName = "";
  }
  public static StepikUserInfo createEmptyUser() {
    return new StepikUserInfo();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getFirstName() {
    return myFirstName;
  }

  public void setFirstName(final String firstName) {
    myFirstName = firstName;
  }

  public String getLastName() {
    return myLastName;
  }

  public void setLastName(final String lastName) {
    myLastName = lastName;
  }

  @NotNull
  public String getName() {
    return StringUtil.join(new String[]{myFirstName, myLastName}, " ");
  }

  public boolean isGuest() {
    return isGuest;
  }

  public void setGuest(boolean guest) {
    isGuest = guest;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StepikUserInfo user = (StepikUserInfo)o;

    if (id != user.id) return false;
    if (isGuest != user.isGuest) return false;
    if (!myFirstName.equals(user.myFirstName)) return false;
    if (!myLastName.equals(user.myLastName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id;
    result = 31 * result + myFirstName.hashCode();
    result = 31 * result + myLastName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return myFirstName;
  }
}
