package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.lang.Language;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.stepik.StepicUser;
import one.util.streamex.StreamEx;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Course extends ItemContainer {
  transient private List<StepicUser> authors = new ArrayList<>();
  @Expose @SerializedName("summary") private String description;
  @Expose @SerializedName("title") private String name;

  @Expose @SerializedName("programming_language") private String myProgrammingLanguage = EduNames.PYTHON;
  @Expose @SerializedName("language") private String myLanguageCode = "en";

  //this field is used to distinguish ordinary and CheckIO projects,
  //"PyCharm" is used here for historical reasons
  private String courseType = EduNames.PYCHARM;
  protected String courseMode = EduNames.STUDY; //this field is used to distinguish study and course creator modes

  protected CourseVisibility myVisibility = CourseVisibility.LocalVisibility.INSTANCE;

  public Course() {}

  public void init(@Nullable Course course, @Nullable StudyItem parentItem, boolean isRestarted) {
    for (StudyItem item : items) {
      item.init(this, this, isRestarted);
    }
  }

  /**
   * exclude service lesson containing additional files for the course. Returns lessons copy.
   */
  @NotNull
  @Override
  public List<Lesson> getLessons() {
    return getLessons(false);
  }

  /**
   * returns service lesson as well. Meant to be used in project generation/serialization
   */
  public List<Lesson> getLessons(boolean withAdditional) {
    final List<Lesson> lessons = items.stream().filter(Lesson.class::isInstance).map(Lesson.class::cast).collect(Collectors.toList());
    return withAdditional ? lessons : lessons.stream().filter(lesson -> !lesson.isAdditional()).collect(Collectors.toList());
  }

  public void addSection(@NotNull Section section) {
    items.add(section);
  }

  @NotNull
  public List<Section> getSections() {
    return items.stream().filter(Section.class::isInstance).map(Section.class::cast).collect(Collectors.toList());
  }

  public void removeSection(@NotNull final Section toRemove) {
    items.remove(toRemove);
  }

  public void removeAdditionalLesson() {
    items.stream().filter(it -> it instanceof Lesson && ((Lesson)it).isAdditional()).findFirst().
      ifPresent(lesson -> items.remove(lesson));
  }

  @Nullable
  public Lesson getLesson(@Nullable final String sectionName, @NotNull final String lessonName) {
    if (sectionName != null) {
      final Section section = getSection(sectionName);
      if (section != null) {
        return section.getLesson(lessonName);
      }
    }
    return (Lesson)StreamEx.of(items).filter(Lesson.class::isInstance)
      .findFirst(lesson -> lessonName.equals(lesson.getName())).orElse(null);
  }

  @Nullable
  public Section getSection(@NotNull final String name) {
    return (Section)items.stream().filter(Section.class::isInstance).
      filter(item -> item.getName().equals(name)).findFirst().orElse(null);
  }

  @Nullable
  public Lesson getLesson(int lessonId) {
    return (Lesson)items.stream().filter(Lesson.class::isInstance).
      filter(item -> ((Lesson)item).getId() == lessonId).findFirst().orElse(null);
  }

  @NotNull
  public List<StepicUser> getAuthors() {
    return authors;
  }

  public static String getAuthorsString(@NotNull List<StepicUser> authors) {
    return StringUtil.join(authors, StepicUser::getName, ", ");
  }

  @Transient
  public void setAuthorsAsString(String[] authors) {
    this.authors = new ArrayList<>();
    for (String name : authors) {
      final List<String> firstLast = StringUtil.split(name, " ");
      if (!firstLast.isEmpty()) {
        final StepicUser user = StepicUser.createEmptyUser();
        user.setFirstName(firstLast.remove(0));
        if (firstLast.size() > 0) {
          user.setLastName(StringUtil.join(firstLast, " "));
        }
        this.authors.add(user);
      }
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isUpToDate() {
    return true;
  }

  public void setUpdated() {}

  public Language getLanguageById() {
    return Language.findLanguageByID(getLanguageID());
  }

  /**
   * This method should be used by serialized only
   * Use {@link #getLanguageID()} and {@link #getLanguageVersion()} methods instead
   */
  @Deprecated
  public String getLanguage() {
    return myProgrammingLanguage;
  }

  public void setLanguage(@NotNull final String language) {
    myProgrammingLanguage = language;
  }

  public String getLanguageID() {
    return myProgrammingLanguage.split(" ")[0];
  }

  @Nullable
  public String getLanguageVersion() {
    String[] split = myProgrammingLanguage.split(" ");
    if (split.length <= 1) {
      return null;
    }
    return split[1];
  }

  public void setAuthors(List<StepicUser> authors) {
    this.authors = authors;
  }

  @NotNull
  public String getCourseType() {
    return courseType;
  }

  public void setCourseType(String courseType) {
    this.courseType = courseType;
  }

  public String getCourseMode() {
    return courseMode;
  }

  public void setCourseMode(String courseMode) {
    this.courseMode = courseMode;
  }

  public Course copy() {
    Element element = XmlSerializer.serialize(this);
    Course copy = XmlSerializer.deserialize(element, getClass());
    copy.init(null, null, true);
    return copy;
  }

  public boolean isAdaptive() {
    return false;
  }

  public boolean isStudy() {
    return EduNames.STUDY.equals(courseMode);
  }

  @Override
  public void sortItems() {
    super.sortItems();
    for (Section section : getSections()) {
      section.sortItems();
    }
  }

  @Override
  public String toString() {
    return getName();
  }

  @NotNull
  public List<String> getAuthorFullNames() {
    return authors.stream()
      .map(user -> StringUtil.join(Arrays.asList(user.getFirstName(), user.getLastName()), " "))
      .collect(Collectors.toList());
  }

  @NotNull
  public List<Tag> getTags() {
    List<Tag> tags = new ArrayList<>();
    tags.add(new ProgrammingLanguageTag(getLanguageById()));
    if (isAdaptive()) {
      tags.add(new Tag(EduNames.ADAPTIVE));
    }
    tags.add(new HumanLanguageTag(getHumanLanguage()));
    return tags;
  }

  public String getHumanLanguage() {
    Locale loc = new Locale(myLanguageCode);
    return loc.getDisplayName();
  }

  public String getLanguageCode() {
    return myLanguageCode;
  }

  public void setLanguageCode(String languageCode) {
    myLanguageCode = languageCode;
  }

  @Transient
  public CourseVisibility getVisibility() {
    return myVisibility;
  }

  @Transient
  public void setVisibility(CourseVisibility visibility) {
    myVisibility = visibility;
  }

  public void setItems(List<StudyItem> items) {
    this.items = items;
  }

  public void addItem(@NotNull StudyItem item, int index) {
    items.add(index, item);
  }
}
