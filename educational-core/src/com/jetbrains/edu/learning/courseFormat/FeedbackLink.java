package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeedbackLink {
  @Expose
  @SerializedName("link_type")
  @NotNull
  private LinkType myType;

  @Expose
  @SerializedName("link")
  @Nullable
  private String myLink;

  public FeedbackLink() {
    myType = LinkType.STEPIK;
  }

  @NotNull
  public LinkType getType() {
    return myType;
  }

  public void setType(@NotNull LinkType type) {
    myType = type;
  }

  @Nullable
  public String getLink() {
    return myLink;
  }

  public void setLink(@Nullable String link) {
    myLink = link;
  }

  public enum LinkType {
    STEPIK, CUSTOM, NONE
  }
}
