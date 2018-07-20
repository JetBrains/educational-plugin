package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public class AdditionalFile implements VisibleFile {

  @SerializedName("text")
  @Expose
  private String myText;

  @SerializedName("is_visible")
  @Expose
  private boolean myIsVisible = true;

  @SuppressWarnings("unused")
  public AdditionalFile() {}

  public AdditionalFile(@NotNull String text, boolean isVisible) {
    myText = text;
    myIsVisible = isVisible;
  }

  @NotNull
  public String getText() {
    return myText;
  }

  public void setText(@NotNull String text) {
    myText = text;
  }

  @Override
  public boolean isVisible() {
    return myIsVisible;
  }

  @Override
  public void setVisible(boolean visible) {
    myIsVisible = visible;
  }
}
