package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of windows which user should type in
 */

public class AnswerPlaceholder {

  @SerializedName("offset")
  @Expose private int myOffset = -1;

  @SerializedName("length")
  @Expose private int myLength = -1;

  private int myIndex = -1;
  private MyInitialState myInitialState;
  private boolean myUseLength = true; // if true -- taskText length used, else -- possible answer. Always true in student view

  @Transient private TaskFile myTaskFile;

  @Expose
  @SerializedName("dependency")
  @Nullable
  private AnswerPlaceholderDependency myPlaceholderDependency = null;

  @SerializedName("hints")
  @Expose private List<String> myHints = new ArrayList<>();

  @SerializedName("possible_answer")
  @Expose private String myPossibleAnswer = "";

  @SerializedName("placeholder_text")
  @Expose private String myPlaceholderText;

  @SerializedName("selected")
  private boolean mySelected = false;

  @SerializedName("status")
  private CheckStatus myStatus = CheckStatus.Unchecked;

  /*
   * Actual student's answer, used to restore state of task of framework lesson after navigation actions
   */
  @Nullable
  private String myStudentAnswer = null;

  public AnswerPlaceholder() {
  }

  public void initAnswerPlaceholder(final TaskFile file, boolean isRestarted) {
    setTaskFile(file);
    if (!isRestarted) {
      if (myPlaceholderDependency != null) {
        myPlaceholderDependency.setAnswerPlaceholder(this);
      }
      setInitialState(new MyInitialState(myOffset, myLength));
      myStatus = file.getTask().getStatus();
    }
  }

  public int getIndex() {
    return myIndex;
  }

  public void setIndex(int index) {
    myIndex = index;
  }

  /**
   * in actions {@link AnswerPlaceholder#getRealLength()} should be used
   */
  public int getLength() {
    return myLength;
  }

  public void setLength(int length) {
    myLength = length;
  }

  public String getPossibleAnswer() {
    return myPossibleAnswer;
  }

  public void setPossibleAnswer(String possibleAnswer) {
    myPossibleAnswer = possibleAnswer;
  }

  public MyInitialState getInitialState() {
    return myInitialState;
  }

  public void setInitialState(MyInitialState initialState) {
    myInitialState = initialState;
  }

  public String getPlaceholderText() {
    return myPlaceholderText;
  }

  public void setPlaceholderText(String placeholderText) {
    myPlaceholderText = placeholderText;
  }

  @Transient
  public TaskFile getTaskFile() {
    return myTaskFile;
  }

  @Transient
  public void setTaskFile(TaskFile taskFile) {
    myTaskFile = taskFile;
  }

  public int getPossibleAnswerLength() {
    return getPossibleAnswer().length();
  }

  /**
   * Returns window to its initial state
   */
  public void reset() {
    myOffset = myInitialState.getOffset();
    myLength = myInitialState.getLength();
  }

  public CheckStatus getStatus() {
    return myStatus;
  }

  public void setStatus(CheckStatus status) {
    myStatus = status;
  }

  public boolean getSelected() {
    return mySelected;
  }

  public void setSelected(boolean selected) {
    mySelected = selected;
  }

  public void init() {
    setInitialState(new MyInitialState(myOffset, getPlaceholderText().length()));
  }

  public boolean getUseLength() {
    return myUseLength;
  }

  /**
   * @return length or possible answer length
   */
  public int getRealLength() {
    return myUseLength ? getLength() : getPossibleAnswer().length();
  }

  public void setUseLength(boolean useLength) {
    myUseLength = useLength;
  }

  public int getOffset() {
    return myOffset;
  }

  public void setOffset(int offset) {
    myOffset = offset;
  }

  public List<String> getHints() {
    return myHints;
  }

  public void setHints(@NotNull final List<String> hints) {
    myHints = hints;
  }

  @Nullable
  public String getStudentAnswer() {
    return myStudentAnswer;
  }

  public void setStudentAnswer(@Nullable String studentAnswer) {
    myStudentAnswer = studentAnswer;
  }

  public static class MyInitialState {
    private int length = -1;
    private int offset = -1;

    public MyInitialState() {
    }

    public MyInitialState(int initialOffset, int length) {
      this.offset = initialOffset;
      this.length = length;
    }

    public int getLength() {
      return length;
    }

    public void setLength(int length) {
      this.length = length;
    }

    public int getOffset() {
      return offset;
    }

    public void setOffset(int offset) {
      this.offset = offset;
    }
  }

  boolean isValid(int textLength) {
    int end = getOffset() + getRealLength();
    return getOffset() >= 0 && getRealLength() >= 0 && end <= textLength;
  }

  @Nullable
  public AnswerPlaceholderDependency getPlaceholderDependency() {
    return myPlaceholderDependency;
  }

  public void setPlaceholderDependency(@Nullable AnswerPlaceholderDependency placeholderDependency) {
    myPlaceholderDependency = placeholderDependency;
    if (placeholderDependency != null) {
      myPlaceholderDependency.setAnswerPlaceholder(this);
    }
  }

  public int getEndOffset() {
    return myOffset + getRealLength();
  }

  @NotNull
  public JBColor getColor() {
    final CheckStatus status = getStatus();
    if (status == CheckStatus.Solved) {

      Color colorLight = ColorUtil.fromHex("26993D", JBColor.LIGHT_GRAY);
      colorLight = ColorUtil.toAlpha(colorLight, 90);

      Color colorDark = ColorUtil.fromHex("47CC5E", JBColor.LIGHT_GRAY);
      colorDark = ColorUtil.toAlpha(colorDark, 82);

      return new JBColor(colorLight, colorDark);
    }
    if (status == CheckStatus.Failed) {
      Color colorLight = ColorUtil.fromHex("CC0000", JBColor.GRAY);
      colorLight = ColorUtil.toAlpha(colorLight, 64);
      Color colorDark = ColorUtil.fromHex("FF7373", JBColor.GRAY);
      colorDark = ColorUtil.toAlpha(colorDark, 90);
      return new JBColor(colorLight, colorDark);
    }
    return getDefaultPlaceholderColor();
  }

  @NotNull
  public static JBColor getDefaultPlaceholderColor() {
    Color colorLight = ColorUtil.fromHex("284B73", JBColor.GRAY);
    colorLight = ColorUtil.toAlpha(colorLight, 64);
    Color colorDark = ColorUtil.fromHex("A1C1E6", JBColor.GRAY);
    colorDark = ColorUtil.toAlpha(colorDark, 72);
    return new JBColor(colorLight, colorDark);
  }
}
