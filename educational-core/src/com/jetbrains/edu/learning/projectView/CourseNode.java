package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static com.jetbrains.edu.learning.EduUtils.getFirst;
import static com.jetbrains.edu.learning.projectView.CourseViewPane.HIDE_SOLVED_LESSONS;


public class CourseNode extends EduNode {
  protected final Course myCourse;

  public CourseNode(@NotNull Project project,
                    PsiDirectory value,
                    ViewSettings viewSettings,
                    @NotNull Course course) {
    super(project, value, viewSettings);
    myCourse = course;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    Pair<Integer, Integer> progress = ProgressUtil.INSTANCE.countProgressAsOneTaskWithSubtasks(myCourse.getLessons());
    if (progress == null) {
      progress = ProgressUtil.INSTANCE.countProgressWithoutSubtasks(myCourse.getLessons());
    }

    final Integer tasksSolved = progress.getFirst();
    final Integer tasksTotal = progress.getSecond();
    data.clearText();
    data.addText(myCourse.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    data.setIcon(EducationalCoreIcons.Course);
    data.addText("  " + tasksSolved.toString() + "/" + tasksTotal.toString(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode> getChildrenImpl() {
    final ArrayList<AbstractTreeNode> result = new ArrayList<>();

    if (hasVisibleLessons()) {
      result.addAll(getLessonNodes(myProject, getValue(), getSettings(), (lesson -> true), createLessonFunction()));

      final Collection<AbstractTreeNode> children =
        ProjectViewDirectoryHelper.getInstance(myProject).getDirectoryChildren(getValue(), getSettings(), true,
                                                                               getNoLessonFilter());

      for (AbstractTreeNode child : children) {
        final AbstractTreeNode node = modifyChildNode(child);
        if (node != null) {
          result.add(node);
        }
      }
      return result;
    }
    return getTaskNodes();
  }

  @NotNull
  protected BiFunction<Lesson, PsiDirectory, LessonNode> createLessonFunction() {
    return (lesson, lessonDir) -> new LessonNode(myProject, lessonDir, getSettings(), lesson);
  }

  protected AbstractTreeNode modifyChildNode(AbstractTreeNode child) {
    return null;
  }

  @NotNull
  private static PsiFileSystemItemFilter getNoLessonFilter() {
    return new PsiFileSystemItemFilter() {
      @Override
      public boolean shouldShow(@NotNull PsiFileSystemItem item) {
        return !(item instanceof PsiDirectory && item.getName().startsWith(EduNames.LESSON));
      }
    };
  }

  @NotNull
  private Collection<AbstractTreeNode> getTaskNodes() {
    final Lesson lesson = getFirst(myCourse.getLessons());
    if (lesson == null) {
      return Collections.emptyList();
    }
    final Task task = getFirst(lesson.getTaskList());
    if (task == null) {
      return Collections.emptyList();
    }
    final VirtualFile taskDirectoryVF = task.getTaskDir(myProject);
    if (taskDirectoryVF == null) {
      return Collections.emptyList();
    }
    PsiDirectory taskDirectory = PsiManager.getInstance(myProject).findDirectory(taskDirectoryVF);
    if (taskDirectory == null) {
      return Collections.emptyList();
    }
    String sourceDir = TaskExt.getSourceDir(task);
    if (StringUtil.isNotEmpty(sourceDir)) {
      PsiDirectory srcDir = taskDirectory.findSubdirectory(sourceDir);
      if (srcDir != null) {
        taskDirectory = srcDir;
      }
    }
    return Collections.singleton(new TaskNode(myProject, taskDirectory, getSettings(), task));
  }

  protected boolean hasVisibleLessons() {
    return myCourse.getLessons().size() != 1 || myCourse.getLessons().get(0).getTaskList().size() != 1;
  }

  public static Collection<AbstractTreeNode> getLessonNodes(@NotNull final Project project,
                                                            @NotNull final PsiDirectory directory,
                                                            @NotNull final ViewSettings settings,
                                                            @Nullable Predicate<Lesson> filter,
                                                            @NotNull BiFunction<Lesson, PsiDirectory, LessonNode> createLessonNode) {
    final ArrayList<AbstractTreeNode> result = new ArrayList<>();
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return result;
    }

    final Collection<AbstractTreeNode> children =
      ProjectViewDirectoryHelper.getInstance(project).getDirectoryChildren(directory, settings, true, null);

    for (AbstractTreeNode child : children) {
      final Object value = child.getValue();
      if (value instanceof PsiDirectory) {
        final PsiDirectory lessonDirectory = (PsiDirectory)value;
        final Lesson lesson = course.getLesson(lessonDirectory.getName());
        if (lesson != null) {
          final CheckStatus status = lesson.getStatus();
          if (status.equals(CheckStatus.Solved) && PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)) {
            continue;
          }
          if (filter == null || filter.test(lesson)) {
            result.add(createLessonNode.apply(lesson, lessonDirectory));
          }
        }
      }
    }
    return result;
  }
}
