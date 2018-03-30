package com.jetbrains.edu.coursecreator.actions.sections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NonNls;

public class CCAddSection extends DumbAwareAction {
  public static final String TITLE = "Wrap With Section";
  @NonNls private static final String SECTION = "Section";

  public CCAddSection() {
    super(TITLE, TITLE, null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
    if (virtualFiles == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    //final List<Section> sections = course.getSections();
    //final List<Integer> lessonsInSections =
    //  sections.stream().map(section -> section.lessonIndexes).flatMap(lessonIndexes -> lessonIndexes.stream()).collect(Collectors.toList());
    //final ArrayList<Integer> lessonsToWrap = new ArrayList<>();
    //for (VirtualFile file : virtualFiles) {
    //  final Lesson lesson = course.getLesson(file.getName());
    //  if (lesson != null && !lessonsInSections.contains(lesson.getIndex())) {
    //    lessonsToWrap.add(lesson.getIndex());
    //  }
    //}
    //if (lessonsToWrap.isEmpty()) {
    //  return;
    //}
    //final int index = sections.size() + 1;
    //final String sectionName = Messages.showInputDialog("Enter Section Name", SECTION, null,
    //                                                    SECTION.toLowerCase() + index, new NonEmptyInputValidator());
    //if (sectionName != null) {
    //  final Section section = new Section();
    //  section.setTitle(sectionName);
    //  section.lessonIndexes.addAll(lessonsToWrap);
    //  course.addSections(Collections.singletonList(section));
    //  ProjectView.getInstance(project).refresh();
    //}
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
    //final List<Section> sections = course.getSections();
    //final List<Integer> lessonsInSections =
    //  sections.stream().map(section -> section.lessonIndexes).flatMap(lessonIndexes -> lessonIndexes.stream()).collect(Collectors.toList());
    //final Object[] selectedItems = PlatformDataKeys.SELECTED_ITEMS.getData(e.getDataContext());
    //if (selectedItems != null) {
    //  for (Object item : selectedItems) {
    //    if (item instanceof Section) {
    //      return;
    //    }
    //    else if (item instanceof PsiDirectory) {
    //      final Lesson lesson = course.getLesson(((PsiDirectory)item).getName());
    //      if (lesson != null) {
    //        if (lessonsInSections.contains(lesson.getIndex())) {
    //          return;
    //        }
    //        presentation.setEnabledAndVisible(true);
    //      }
    //    }
    //  }
    //}
  }
}