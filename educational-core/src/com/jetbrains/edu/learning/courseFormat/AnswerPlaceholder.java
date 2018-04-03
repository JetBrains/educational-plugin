package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
  private boolean mySelected = false;
  private boolean myUseLength = true;

  @Transient private TaskFile myTaskFile;

  @SerializedName("subtask_infos")
  @Expose private Map<Integer, AnswerPlaceholderSubtaskInfo> mySubtaskInfos = new HashMap<>();

  @Expose
  @SerializedName("dependency")
  @Nullable
  private AnswerPlaceholderDependency myPlaceholderDependency = null;

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
      for (AnswerPlaceholderSubtaskInfo info : getSubtaskInfos().values()) {
        info.setStatus(file.getTask().getStatus());
      }
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

  @Transient
  public String getPossibleAnswer() {
    return getActiveSubtaskInfo().getPossibleAnswer();
  }

  @Transient
  public void setPossibleAnswer(String possibleAnswer) {
    getActiveSubtaskInfo().setPossibleAnswer(possibleAnswer);
  }

  public MyInitialState getInitialState() {
    return myInitialState;
  }

  public void setInitialState(MyInitialState initialState) {
    myInitialState = initialState;
  }

  @Transient
  public String getTaskText() {
    return getActiveSubtaskInfo().getPlaceholderText();
  }

  @Transient
  public void setTaskText(String taskText) {
    getActiveSubtaskInfo().setPlaceholderText(taskText);
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

  @Transient
  public CheckStatus getStatus() {
    AnswerPlaceholderSubtaskInfo info = getActiveSubtaskInfo();
    return info != null ? info.getStatus() : CheckStatus.Unchecked;
  }

  @Transient
  public void setStatus(CheckStatus status) {
    getActiveSubtaskInfo().setStatus(status);
  }

  public boolean getSelected() {
    return mySelected;
  }

  public void setSelected(boolean selected) {
    mySelected = selected;
  }

  public void init() {
    setInitialState(new MyInitialState(myOffset, getTaskText().length()));
  }

  public boolean getUseLength() {
    return myUseLength;
  }

  /**
   * @return length or possible answer length
   */
  public int getRealLength() {
    return myUseLength ? getLength() : getVisibleLength(getActiveSubtaskIndex());
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

  @Transient
  public List<String> getHints() {
    return getActiveSubtaskInfo().getHints();
  }

  @Transient
  public void setHints(@NotNull final List<String> hints) {
   getActiveSubtaskInfo().setHints(hints);
  }

  public void addHint(@NotNull final String text) {
    getActiveSubtaskInfo().addHint(text);
  }

  public void removeHint(int i) {
    getActiveSubtaskInfo().removeHint(i);
  }

  public Map<Integer, AnswerPlaceholderSubtaskInfo> getSubtaskInfos() {
    return mySubtaskInfos;
  }

  public void setSubtaskInfos(Map<Integer, AnswerPlaceholderSubtaskInfo> subtaskInfos) {
    mySubtaskInfos = subtaskInfos;
  }

  public boolean isActive() {
    return getActiveSubtaskInfo() != null;
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

  public AnswerPlaceholderSubtaskInfo getActiveSubtaskInfo() {
    return mySubtaskInfos.get(getActiveSubtaskIndex());
  }

  public int getActiveSubtaskIndex() {
    if (myTaskFile == null || myTaskFile.getTask() == null) {
      return 0;
    }
    final Task task = myTaskFile.getTask();
    return 0;
  }

  public int getVisibleLength(int subtaskIndex) {
    int minIndex = Collections.min(mySubtaskInfos.keySet());
    AnswerPlaceholderSubtaskInfo minInfo = mySubtaskInfos.get(minIndex);
    if (minIndex == subtaskIndex) {
      return getUseLength() ? myLength : minInfo.getPossibleAnswer().length();
    }
    if (minIndex > subtaskIndex) {
      return minInfo.isNeedInsertText() ? 0 : minInfo.getPlaceholderText().length();
    }
    int maxIndex = Collections.max(ContainerUtil.filter(mySubtaskInfos.keySet(), i -> i <= subtaskIndex));
    return getUseLength() ? myLength : mySubtaskInfos.get(maxIndex).getPossibleAnswer().length();
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
}
