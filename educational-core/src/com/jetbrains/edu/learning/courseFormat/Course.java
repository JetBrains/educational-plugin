package com.jetbrains.edu.learning.courseFormat;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.XCollection;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.UserInfo;
import com.jetbrains.edu.learning.actions.CheckAction;
import com.jetbrains.edu.learning.compatibility.CourseCompatibility;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.marketplace.api.MarketplaceUserInfo;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.StepikUserInfo;
import icons.EducationalCoreIcons;
import one.util.streamex.StreamEx;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * To introduce new course it's required to:
 * - Extend Course class
 * - Update {@link SerializationUtils.Xml#COURSE_ELEMENT_TYPES} to handle xml migrations and deserialization
 * - Update CourseBuilder#build() in {@link com.jetbrains.edu.learning.yaml.format.CourseYamlUtil} to handle course loading from YAML
 * - Override {@link Course#getItemType}, that's how we find appropriate {@link com.jetbrains.edu.learning.configuration.EduConfigurator}
 */
public abstract class Course extends LessonContainer {
  @XCollection(elementTypes = {
    MarketplaceUserInfo.class,
    StepikUserInfo.class
  })
  @Transient private List<UserInfo> authors = new ArrayList<>();
  private String description;

  private String myProgrammingLanguage = EduNames.PYTHON; // language and optional version in form "Language Version" (as "Python 3.7")
  private String myLanguageCode = "en";

  @NotNull protected String myEnvironment = EduNames.DEFAULT_ENVIRONMENT;
  private String courseMode = EduNames.STUDY; //this field is used to distinguish study and course creator modes

  private boolean solutionsHidden;

  protected CourseVisibility myVisibility = CourseVisibility.LocalVisibility.INSTANCE;

  // Marketplace:
  private boolean isMarketplace = false;
  private Vendor myVendor;
  private int myMarketplaceCourseVersion = 0;
  private int myMarketplaceId = 0;
  @Nullable private String organization;

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
  public List<UserInfo> getAuthors() {
    return authors;
  }

  @NotNull
  public String getEnvironment() {
    return myEnvironment;
  }

  public void setEnvironment(@NotNull String environment) {
    myEnvironment = environment;
  }

  @Override
  public int getId() {
    return 0;
  }

  @Override
  @NotNull
  public VirtualFile getDir(@NotNull final VirtualFile baseDir) {
    return baseDir;
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

  @Nullable
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

  public boolean getSolutionsHidden() {
    return solutionsHidden;
  }

  public void setSolutionsHidden(boolean solutionsHidden) {
    this.solutionsHidden = solutionsHidden;
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

  public void setAuthors(List<UserInfo> authors) {
    this.authors = authors;
  }

  @NotNull
  public String getItemType() {
    return EduNames.PYCHARM;  //"PyCharm" is used here for historical reasons
  }

  @NotNull
  public CheckAction getCheckAction() {
    return new CheckAction();
  }

  public String getCourseMode() {
    return courseMode;
  }

  public void setCourseMode(String courseMode) {
    this.courseMode = courseMode;
  }

  public Course copy() {
    return copyAs(getClass());
  }

  public <T extends Course> T copyAs(Class<T> clazz) {
    Element element = XmlSerializer.serialize(this);
    T copy = XmlSerializer.deserialize(element, clazz);
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
    if (organization != null) return Collections.singletonList(organization);
    return authors.stream()
      .map(user -> user.getFullName())
      .collect(Collectors.toList());
  }

  @NotNull
  public List<Tag> getTags() {
    List<Tag> tags = new ArrayList<>();

    String technologyName = CourseExt.getTechnologyName(this);
    if (technologyName != null) {
      tags.add(new ProgrammingLanguageTag(technologyName));
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

  @Transient
  @NotNull
  public CourseCompatibility getCompatibility() {
    return CourseCompatibility.forCourse(this);
  }

  public void addItem(@NotNull StudyItem item, int index) {
    items.add(index, item);
  }

  @NotNull
  public Icon getIcon() {
    return EducationalCoreIcons.CourseTree;
  }

  public boolean isViewAsEducatorEnabled() {
    return ApplicationManager.getApplication().isInternal();
  }

  public Vendor getVendor() {
    return myVendor;
  }

  public void setVendor(Vendor vendor) {
    myVendor = vendor;
  }

  public boolean isMarketplace() {
    return isMarketplace;
  }

  public void setMarketplace(boolean isMarketplace) {
    this.isMarketplace = isMarketplace;
  }

  public int getMarketplaceCourseVersion() {
    return myMarketplaceCourseVersion;
  }

  public void setMarketplaceCourseVersion(int marketplaceCourseVersion) {
    myMarketplaceCourseVersion = marketplaceCourseVersion;
  }

  @Nullable
  public String getOrganization() {
    return organization;
  }

  public void setOrganization(@Nullable String organization) {
    this.organization = organization;
  }

  public void incrementMarketplaceCourseVersion(int remoteCourseVersion) {
    myMarketplaceCourseVersion = ++remoteCourseVersion;
  }

  public int getMarketplaceId() { return myMarketplaceId; }

  public void setMarketplaceId(int marketplaceId) { myMarketplaceId = marketplaceId; }
}
