package com.jetbrains.edu.learning.courseFormat;

import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.coursecreator.StudyItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Base class for all items of course: section, lessons, tasks, etc.
 *
 * For each base type of study item ({@code Section}, {@code Lesson}, etc.)
 * there is the corresponding element in {@code StudyItemType} enum.
 *
 * @see Section
 * @see Lesson
 * @see FrameworkLesson
 * @see com.jetbrains.edu.learning.courseFormat.tasks.Task
 * @see StudyItemType
 */
public abstract class StudyItem {
  // from 1 to number of items
  private int myIndex = -1;
  private String myName;
  protected int myId;
  private Date myUpdateDate = new Date(0);
  private final UserDataHolder myDataHolder = new UserDataHolderBase();

  @Transient
  public UserDataHolder getDataHolder() {
    return myDataHolder;
  }

  // Non unique lesson/task/section names can be received from stepik. In this case unique directory name is generated,
  // but original non unique name is displayed
  @Nullable private String myCustomPresentableName = null;

  public StudyItem() {}

  public StudyItem(@NotNull String name) {
    myName = name;
  }

  /**
   * Initializes state of StudyItem
   */
  public abstract void init(@Nullable final Course course, @Nullable final StudyItem parentItem, boolean isRestarted);

  public String getName() {
    return myName;
  }

  public void setName(String name) {
    myName = name;
  }

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

  /**
   * @return id on remote resource (Stepik, CheckIO, Codeforces)
   */
  public int getId() {
    return myId;
  }

  public void setId(int id) {
    myId = id;
  }

  public abstract VirtualFile getDir(@NotNull final VirtualFile baseDir);

  @NotNull
  public abstract Course getCourse();

  @NotNull
  public abstract StudyItem getParent();

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  public void setUpdateDate(Date updateDate) {
    myUpdateDate = updateDate;
  }

  // used in json/yaml serialization/deserialization
  public abstract String getItemType();

  public void generateId() {
    if (myId == 0) {
      myId = System.identityHashCode(this);
    }
  }

  public String getPathInCourse() {
    List<String> parents = new ArrayList<>();
    StudyItem parent = getParent();
    while (!(parent instanceof Course)) {
      parents.add(parent.getName());
      parent = parent.getParent();
    }
    Collections.reverse(parents);

    String parentsLine = StringUtil.join(parents, VfsUtilCore.VFS_SEPARATOR);
    if (parentsLine.isEmpty()) {
      return getName();
    }
    return parentsLine + VfsUtilCore.VFS_SEPARATOR + getName();
  }
}
