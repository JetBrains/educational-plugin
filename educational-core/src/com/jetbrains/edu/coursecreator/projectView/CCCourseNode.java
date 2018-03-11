package com.jetbrains.edu.coursecreator.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.projectView.CourseNode;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class CCCourseNode extends CourseNode {
  private static final Collection<String> NAMES_TO_IGNORE = ContainerUtil.newHashSet(
    "settings.gradle", "local.properties", "gradlew", "gradlew.bat", "build.gradle");

  public CCCourseNode(@NotNull Project project,
                      PsiDirectory value,
                      ViewSettings viewSettings,
                      @NotNull Course course) {
    super(project, value, viewSettings, course);
  }

  @NotNull
  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    final Collection<AbstractTreeNode> lessonNodes =
      getLessonNodes(myProject, myCourse, getValue(),
                     getSettings(), null, (lesson, lessonDir) -> new CCLessonNode(myProject, lessonDir, getSettings(), lesson));
    final ArrayList<AbstractTreeNode> result = new ArrayList<>(lessonNodes);

    final Collection<AbstractTreeNode> children =
      ProjectViewDirectoryHelper.getInstance(myProject).getDirectoryChildren(getValue(), getSettings(), true, getNoLessonFilter());

    for (AbstractTreeNode child : children) {
      final AbstractTreeNode node = modifyChildNode(child);
      if (node != null) {
        result.add(node);
      }
    }
    return result;
  }

  @NotNull
  private static PsiFileSystemItemFilter getNoLessonFilter() {
    return new PsiFileSystemItemFilter() {
      @Override
      public boolean shouldShow(@NotNull PsiFileSystemItem item) {
        return !item.getName().startsWith(EduNames.LESSON);
      }
    };
  }

  @Nullable
  @Override
  public AbstractTreeNode modifyChildNode(AbstractTreeNode childNode) {
    if (childNode instanceof PsiFileNode) {
      VirtualFile virtualFile = ((PsiFileNode)childNode).getVirtualFile();
      if (virtualFile == null) {
        return null;
      }
      if (NAMES_TO_IGNORE.contains(virtualFile.getName())) {
        return null;
      }
      if (FileUtilRt.getExtension(virtualFile.getName()).equals("iml")) {
        return null;
      }
      return new CCStudentInvisibleFileNode(myProject, ((PsiFileNode)childNode).getValue(), getSettings());
    }
    return null;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    updatePresentation(data, myCourse.getName(), JBColor.black, EducationalCoreIcons.Course, "Course Creation");
  }
}
