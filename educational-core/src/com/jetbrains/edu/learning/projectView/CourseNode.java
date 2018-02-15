package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.jetbrains.edu.learning.projectView.CourseViewPane.HIDE_SOLVED_LESSONS;


public class CourseNode extends EduNode {
  @NotNull protected final Project myProject;
  protected final ViewSettings myViewSettings;
  protected final Course myCourse;

  public CourseNode(@NotNull Project project,
                    PsiDirectory value,
                    ViewSettings viewSettings,
                    @NotNull Course course) {
    super(project, value, viewSettings);
    myProject = project;
    myViewSettings = viewSettings;
    myCourse = course;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    setPresentation(data, myCourse.getName(), EducationalCoreIcons.Course);
  }

  @Nullable
  public AbstractTreeNode modifyChildNode(AbstractTreeNode childNode) {
    Object value = childNode.getValue();
    if (value instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)value;
      Lesson lesson = myCourse.getLesson(directory.getName());
      if (lesson != null) {
        final CheckStatus status = lesson.getStatus();
        if (status.equals(CheckStatus.Solved) && PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)) {
          return null;
        }
      }
      return lesson != null ? createChildDirectoryNode(lesson, directory) : null;
    }
    return null;
  }

  @Override
  public PsiDirectoryNode createChildDirectoryNode(StudyItem item, PsiDirectory directory) {
    final List<Lesson> lessons = myCourse.getLessons();
    final Lesson lesson = (Lesson)item;
    if (directory.getChildren().length > 0 && lessons.size() == 1) {
      final List<Task> tasks = lesson.getTaskList();
      if (tasks.size() == 1) {
        PsiDirectory taskDirectory = (PsiDirectory)directory.getChildren()[0];
        String sourceDir = TaskExt.getSourceDir(tasks.get(0));
        if (StringUtil.isNotEmpty(sourceDir)) {
          PsiDirectory srcDir = taskDirectory.findSubdirectory(sourceDir);
          if (srcDir != null) {
            taskDirectory = srcDir;
          }
        }
        return new TaskNode(myProject, taskDirectory, myViewSettings, tasks.get(0));
      }
    }
    return new LessonNode(myProject, directory, myViewSettings, lesson);
  }
}
