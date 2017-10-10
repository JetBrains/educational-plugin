package com.jetbrains.edu.coursecreator.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CCCreateLesson extends CCCreateStudyItemActionBase {
  public static final String TITLE = "Create New " + EduNames.LESSON_TITLED;

  public CCCreateLesson() {
    super(EduNames.LESSON_TITLED, TITLE, EducationalCoreIcons.Lesson);
  }

  @Nullable
  @Override
  protected VirtualFile getParentDir(@NotNull Project project, @NotNull Course course, @NotNull VirtualFile directory) {
    return project.getBaseDir();
  }

  @Override
  protected void addItem(@NotNull Course course, @NotNull StudyItem item) {
    course.addLesson(((Lesson)item));
  }

  @Override
  protected Function<VirtualFile, ? extends StudyItem> getStudyOrderable(@NotNull final StudyItem item) {
    return (Function<VirtualFile, StudyItem>)file -> {
      if (item instanceof Lesson) {
        return ((Lesson)item).getCourse().getLesson(file.getName());
      }
      return null;
    };
  }

  @Override
  @Nullable
  protected VirtualFile createItemDir(@NotNull final Project project, @NotNull final StudyItem item,
                                      @NotNull final VirtualFile parentDirectory, @NotNull final Course course) {
    EduPluginConfigurator configurator = EduPluginConfigurator.INSTANCE.forLanguage(course.getLanguageById());
    if (configurator == null) {
      return null;
    }
    return configurator.createLessonContent(project, (Lesson)item, parentDirectory);
  }

  @Override
  protected int getSiblingsSize(@NotNull Course course, @Nullable StudyItem item) {
    return course.getLessons().size();
  }

  @Nullable
  @Override
  protected StudyItem getParentItem(@NotNull Course course, @NotNull VirtualFile directory) {
    return null;
  }

  @Nullable
  @Override
  protected StudyItem getThresholdItem(@NotNull final Course course, @NotNull final VirtualFile sourceDirectory) {
    return course.getLesson(sourceDirectory.getName());
  }

  @Override
  protected boolean isAddedAsLast(@NotNull VirtualFile sourceDirectory,
                                  @NotNull Project project,
                                  @NotNull Course course) {
    return sourceDirectory.equals(project.getBaseDir());
  }

  @Override
  protected void sortSiblings(@NotNull Course course, @Nullable StudyItem parentItem) {
    course.sortLessons();
  }

  @Override
  protected String getItemName() {
    return EduNames.LESSON;
  }

  @Override
  public StudyItem createAndInitItem(@NotNull Course course, @Nullable StudyItem parentItem, String name, int index) {
    Lesson lesson = new Lesson();
    lesson.setName(name);
    lesson.setCourse(course);
    lesson.setIndex(index);
    return lesson;
  }
}