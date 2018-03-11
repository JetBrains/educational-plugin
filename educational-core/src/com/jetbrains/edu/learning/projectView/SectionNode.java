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
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.projectView.CCLessonNode;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;


public class SectionNode extends ProjectViewNode<Section> {
  private final PsiDirectory myCourseDir;

  public SectionNode(@NotNull Project project, ViewSettings viewSettings, @NotNull Section section, PsiDirectory courseDir) {
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
    data.addText(getValue().getTitle(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    data.setIcon(AllIcons.Ide.Error);
  }
}
