package com.jetbrains.edu.coursecreator.actions.sections;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;

public class CCAddSection extends DumbAwareAction {
  protected static final Logger LOG = Logger.getInstance(CCAddSection.class);

  public static final String TITLE = "Wrap With Section";
  @NonNls private static final String SECTION = "Section";

  public CCAddSection() {
    super(TITLE, TITLE, null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
    if (project == null || virtualFiles == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }

    final ArrayList<Lesson> lessonsToWrap = getLessonsToWrap(virtualFiles, course);
    if (lessonsToWrap.isEmpty()) {
      return;
    }

    final int sectionIndex = course.getSections().size() + 1;
    final String sectionName = Messages.showInputDialog("Enter Section Name", SECTION, null,
                                                        SECTION.toLowerCase() + sectionIndex, new NonEmptyInputValidator());
    if (sectionName == null) {
      return;
    }

    lessonsToWrap.sort(EduUtils.INDEX_COMPARATOR);
    final int minIndex = lessonsToWrap.get(0).getIndex();
    final int maxIndex = lessonsToWrap.get(lessonsToWrap.size() - 1).getIndex();

    final VirtualFile sectionDir = createSectionDir(project, sectionName);
    if (sectionDir == null) {
      return;
    }

    final Section section = createSection(lessonsToWrap, sectionName, minIndex);

    for (int i = 0; i < lessonsToWrap.size(); i++) {
      Lesson lesson = lessonsToWrap.get(i);
      final VirtualFile lessonDir = lesson.getLessonDir(project);
      if (lessonDir != null) {
        moveLesson(lessonDir, sectionDir);
        lesson.setIndex(i + 1);
        lesson.setSection(section);
      }
      course.removeLesson(lesson);
    }

    int delta = -lessonsToWrap.size() + 1;
    CCUtils.updateHigherElements(EduUtils.getCourseDir(project).getChildren(), file -> course.getItem(file.getName()), maxIndex, delta);
    course.addItem(section, section.getIndex() - 1);
    ProjectView.getInstance(project).refresh();
  }

  @NotNull
  private static Section createSection(@NotNull ArrayList<Lesson> lessonsToWrap, @NotNull String sectionName, int index) {
    final Section section = new Section();
    section.setIndex(index);
    section.setName(sectionName);
    section.addLessons(lessonsToWrap);
    return section;
  }

  @NotNull
  private static ArrayList<Lesson> getLessonsToWrap(@NotNull VirtualFile[] virtualFiles, @NotNull Course course) {
    final ArrayList<Lesson> lessonsToWrap = new ArrayList<>();
    for (VirtualFile file : virtualFiles) {
      final Lesson lesson = course.getLesson(file.getName());
      if (lesson != null) {
        lessonsToWrap.add(lesson);
      }
    }
    return lessonsToWrap;
  }

  private void moveLesson(@NotNull final VirtualFile lessonDir, @NotNull final VirtualFile sectionDir) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          lessonDir.move(this, sectionDir);
        }
        catch (IOException e1) {
          LOG.error("Failed to move lesson " + lessonDir.getName() + "to the new section " + sectionDir.getName());
        }
      }
    });
  }

  @Nullable
  private static VirtualFile createSectionDir(@NotNull Project project, @NotNull String sectionName) {
    return ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
      @Override
      public VirtualFile compute() {
        try {
          return VfsUtil.createDirectoryIfMissing(EduUtils.getCourseDir(project), sectionName);
        }
        catch (IOException e1) {
          LOG.error("Failed to create directory for section " + sectionName);
        }
        return null;
      }
    });
  }

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getProject();
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    if (project == null || !CCUtils.isCourseCreator(project)) {
      return;
    }
    final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
    if (virtualFiles == null || virtualFiles.length == 0) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final ArrayList<Lesson> lessonsToWrap = getLessonsToWrap(virtualFiles, course);
    if (!lessonsToWrap.isEmpty()) {
      presentation.setEnabledAndVisible(true);
    }
  }
}