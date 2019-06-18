package com.jetbrains.edu.learning.stepik;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StepikUserInfo {
  @JsonProperty("id")
  private int id = -1;

  @JsonProperty("first_name")
  private String myFirstName;

  @JsonProperty("last_name")
  private String myLastName;

  @SuppressWarnings("unused") //needed for deserialization
  private StepikUserInfo() {
    myFirstName = "";
    myLastName = "";
  }

  public StepikUserInfo(String fullName) {
    this();
    final List<String> firstLast = StringUtil.split(fullName, " ");
    if (firstLast.isEmpty()) {
      return;
    }
    setFirstName(firstLast.remove(0));
    if (firstLast.size() > 0) {
      setLastName(StringUtil.join(firstLast, " "));
    }
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
    List<String> names = new ArrayList<>();
    names.add(myFirstName);
    if (!myLastName.isEmpty()) {
      names.add(myLastName);
    }
    return StringUtil.join(names, " ");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StepikUserInfo user = (StepikUserInfo)o;

    if (id != user.id) return false;
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
