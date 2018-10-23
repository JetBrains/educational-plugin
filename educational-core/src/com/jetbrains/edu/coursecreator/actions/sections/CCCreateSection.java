package com.jetbrains.edu.coursecreator.actions.sections;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.actions.StudyItemType;
import com.jetbrains.edu.coursecreator.actions.CCCreateStudyItemActionBase;
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer;
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CCCreateSection extends CCCreateStudyItemActionBase<Section> {

  public CCCreateSection() {
    super(StudyItemType.SECTION, EducationalCoreIcons.Section);
  }

  @Override
  protected void addItem(@NotNull Course course, @NotNull Section section) {
    course.addSection(section);
    StepikCourseChangeHandler.contentChanged(course);
  }

  @Override
  protected Function<VirtualFile, ? extends StudyItem> getStudyOrderable(@NotNull final StudyItem item,
                                                                         @NotNull Course course) {
    return file -> course.getItem(file.getName());
  }

  @Override
  @Nullable
  protected VirtualFile createItemDir(@NotNull final Project project, @NotNull final Section section,
                                      @NotNull final VirtualFile parentDirectory, @NotNull final Course course) {
    EduConfigurator configurator = CourseExt.getConfigurator(course);
    if (configurator == null) {
      LOG.info("Failed to get configurator for " + course.getLanguageID());
      return null;
    }
    VirtualFile sectionDir = CCUtils.createSectionDir(project, section.getName());
    YamlFormatSynchronizer.saveItem(section);
    YamlFormatSynchronizer.saveItem(course);
    return sectionDir;
  }

  @Override
  protected int getSiblingsSize(@NotNull Course course, @Nullable StudyItem parentItem) {
    return course.getItems().size();
  }

  @Nullable
  @Override
  protected StudyItem getParentItem(@NotNull Course course, @NotNull VirtualFile directory) {
    return course;
  }

  @Nullable
  @Override
  protected StudyItem getThresholdItem(@NotNull final Course course, @NotNull final VirtualFile sourceDirectory) {
    return course.getItem(sourceDirectory.getName());
  }

  @Override
  protected boolean isAddedAsLast(@NotNull VirtualFile sourceDirectory,
                                  @NotNull Project project,
                                  @NotNull Course course) {
    return sourceDirectory.equals(EduUtils.getCourseDir(project));
  }

  @Override
  protected void sortSiblings(@NotNull Course course, @Nullable StudyItem parentItem) {
    course.sortItems();
  }

  @Override
  public Section createAndInitItem(@NotNull Project project, @NotNull Course course,
                                   @Nullable StudyItem parentItem, @NotNull NewStudyItemInfo info) {
    final Section section = new Section();
    section.setCourse(course);
    section.setName(info.getName());
    section.setIndex(info.getIndex());
    return section;
  }
}