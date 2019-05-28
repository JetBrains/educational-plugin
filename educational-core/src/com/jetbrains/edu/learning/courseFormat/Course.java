package com.jetbrains.edu.learning.courseFormat;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.StepikUserInfo;
import one.util.streamex.StreamEx;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * To introduce new course it's required to:
 *  - Extend Course class
 *  - Update {@link SerializationUtils.Xml#COURSE_ELEMENT_TYPES} to handle xml migrations and deserialization
 *  - Update CourseBuilder#build() in {@link com.jetbrains.edu.coursecreator.yaml.format.CourseYamlUtil} to handle course loading from YAML
 *  - Override {@link Course#getItemType}, that's how we find appropriate {@link com.jetbrains.edu.learning.configuration.EduConfigurator}
 */
public abstract class Course extends LessonContainer {
  transient private List<StepikUserInfo> authors = new ArrayList<>();
  private String description;

  private String myProgrammingLanguage = EduNames.PYTHON; // language and optional version in form "Language Version" (as "Python 3.7")
  private String myLanguageCode = "en";

  @NotNull private String myEnvironment = EduNames.DEFAULT_ENVIRONMENT;
  protected String courseMode = EduNames.STUDY; //this field is used to distinguish study and course creator modes

  protected CourseVisibility myVisibility = CourseVisibility.LocalVisibility.INSTANCE;
  protected CourseCompatibility myCompatibility = CourseCompatibility.COMPATIBLE;

  @Transient protected List<TaskFile> additionalFiles = new ArrayList<>();

  public void init(@Nullable Course course, @Nullable StudyItem parentItem, boolean isRestarted) {
    for (int i = 0; i < items.size(); i++) {
      StudyItem item = items.get(i);
      item.setIndex(i + 1);
      item.init(this, this, isRestarted);
    }
  }

  @Transient
  public List<TaskFile> getAdditionalFiles() {
    return additionalFiles;
  }

  @Transient
  public void setAdditionalFiles(@NotNull List<TaskFile> additionalFiles) {
    this.additionalFiles = additionalFiles;
  }

  /**
   * Returns lessons copy.
   */
  @NotNull
  @Override
  public List<Lesson> getLessons() {
    return items.stream().filter(Lesson.class::isInstance).map(Lesson.class::cast).collect(Collectors.toList());
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

  @NotNull
  public List<StepikUserInfo> getAuthors() {
    return authors;
  }

  @NotNull
  public String getEnvironment() {
    return myEnvironment;
  }

  public void setEnvironment(@NotNull String environment) {
    myEnvironment = environment;
  }

  public static String getAuthorsString(@NotNull List<StepikUserInfo> authors) {
    return StringUtil.join(authors, StepikUserInfo::getName, ", ");
  }

  @Transient
  public void setAuthorsAsString(String[] authors) {
    this.authors = new ArrayList<>();
    for (String name : authors) {
      final List<String> firstLast = StringUtil.split(name, " ");
      if (!firstLast.isEmpty()) {
        final StepikUserInfo user = StepikUserInfo.createEmptyUser();
        user.setFirstName(firstLast.remove(0));
        if (firstLast.size() > 0) {
          user.setLastName(StringUtil.join(firstLast, " "));
        }
        this.authors.add(user);
      }
    }
  }

  @Override
  public int getId() {
    return 0;
  }

  @Override
  @NotNull
  public VirtualFile getDir(@NotNull Project project) {
    return OpenApiExtKt.getCourseDir(project);
  }

  @NotNull
  @Override
  public Course getCourse() {
    return this;
  }

  @NotNull
  @Override
  public StudyItem getParent() {
    return this;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Language getLanguageById() {
    return Language.findLanguageByID(getLanguageID());
  }

  /**
   * This method is needed to serialize language and its version as one property
   * Consider using {@link #getLanguageID()} and {@link #getLanguageVersion()} methods instead
   */
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
    if (!myProgrammingLanguage.contains(" ")) {
      return null;
    }
    int languageVersionStartIndex = myProgrammingLanguage.indexOf(" ");
    if (languageVersionStartIndex == myProgrammingLanguage.length() - 1) {
      return null;
    }
    return myProgrammingLanguage.substring(languageVersionStartIndex + 1);
  }

  public void setAuthors(List<StepikUserInfo> authors) {
    this.authors = authors;
  }

  @NotNull
  public String getItemType() {
    return EduNames.PYCHARM;  //"PyCharm" is used here for historical reasons
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
    tags.add(new HumanLanguageTag(getHumanLanguage()));
    return tags;
  }

  public String getHumanLanguage() {
    Locale loc = new Locale(myLanguageCode);
    return loc.getDisplayName();
  }

  @SuppressWarnings("unused") // used for serialization
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

  @Transient
  @NotNull
  public CourseCompatibility getCompatibility() {
    return myCompatibility;
  }

  public void addItem(@NotNull StudyItem item, int index) {
    items.add(index, item);
  }
}
