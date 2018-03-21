package com.jetbrains.edu.learning.projectView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.projectView.CCLessonNode;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static com.jetbrains.edu.learning.projectView.EduNode.LIGHT_GREEN;


public class SectionNode extends ProjectViewNode<Section> {
  private final PsiDirectory myCourseDir;

  public SectionNode(@NotNull Project project, @NotNull ViewSettings viewSettings, @NotNull Section section, @Nullable PsiDirectory courseDir) {
    super(project, section, viewSettings);
    myCourseDir = courseDir;
  }

  @Override
  public boolean contains(@NotNull final VirtualFile file) {
    if (myCourseDir == null || !myCourseDir.isValid()) {
      return false;
    }

    final PsiFile containingFile = myCourseDir.getContainingFile();
    if (containingFile == null) {
      return false;
    }
    final VirtualFile valueFile = containingFile.getVirtualFile();
    return file.equals(valueFile);
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    if (myCourseDir == null) {
      return Collections.emptyList();
    }
    final BiFunction<Lesson, PsiDirectory, LessonNode> createLessonNode =
      (lesson, lessonDir) -> CCUtils.isCourseCreator(myProject) ? new CCLessonNode(myProject, lessonDir, getSettings(), lesson) :
                             new LessonNode(myProject, lessonDir, getSettings(), lesson);

    return CourseNode.getLessonNodes(myProject, myCourseDir,
                                     getSettings(), lesson -> getValue().lessonIndexes.contains(lesson.getIndex()),
                                     createLessonNode);
  }

  @Override
  public int getWeight() {
    final List<Integer> lessonIndexes = getValue().lessonIndexes;
    if (!lessonIndexes.isEmpty()) {
      return lessonIndexes.get(lessonIndexes.size() - 1);
    }
    return 0;
  }

  @Override
  protected void update(PresentationData data) {
    boolean allSolved = isSolved();
    JBColor color = allSolved ? LIGHT_GREEN : JBColor.BLACK;
    Icon icon = allSolved ? AllIcons.Nodes.Package : AllIcons.Nodes.Folder; //TODO: use proper icons
    final SimpleTextAttributes textAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
    data.addText(getValue().getTitle(), textAttributes);
    data.setIcon(icon);
  }

  private boolean isSolved() {
    final Course course = StudyTaskManager.getInstance(myProject).getCourse();
    boolean allSolved = true;
    if (course != null) {
      for (Integer lessonIndex : getValue().lessonIndexes) {
        final Lesson lesson = course.getLessons().get(lessonIndex - 1);
        if (lesson != null) {
          CheckStatus status = lesson.getStatus();
          boolean isSolved = status == CheckStatus.Solved;
          if (!isSolved) {
            allSolved = false;
          }
        }
      }
    }
    return allSolved;
  }

  @Override
  public String getTestPresentation() {
    return getValue().getTitle();
  }
}
