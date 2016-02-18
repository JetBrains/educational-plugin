package com.jetbrains.edu.kotlin;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.EduNames;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.projectView.StudyDirectoryNode;
import com.jetbrains.edu.learning.projectView.StudyTreeStructureProvider;
import com.jetbrains.edu.utils.EduIntelliJNames;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class KotlinStudyTreeStructureProvider extends StudyTreeStructureProvider {
    private static final String UTIL_DIR = "util";
    private static final String OUT_DIR = "out";

    @NotNull
    @Override
    public Collection<AbstractTreeNode> modify(@NotNull AbstractTreeNode parent,
                                               @NotNull Collection<AbstractTreeNode> children,
                                               ViewSettings settings) {
        if (parent instanceof KotlinDirectoryNode) {
            //this it task and we need to delete src folder etc
            for (AbstractTreeNode child : children) {
                if (child instanceof PsiDirectoryNode) {
                    String name = ((PsiDirectoryNode) child).getValue().getName();
                    if (EduIntelliJNames.SRC.equals(name)) {
                        return super.modify(child, child.getChildren(), settings);
                    }
                }
            }
        }
        Collection<AbstractTreeNode> oldNodes = super.modify(parent, children, settings);
        Project project = parent.getProject();
        if (project == null) {
            return oldNodes;
        }
        Course course = StudyTaskManager.getInstance(project).getCourse();
        if (course == null) {
            return oldNodes;
        }
        Collection<AbstractTreeNode> nodes = new ArrayList<AbstractTreeNode>();
        for (AbstractTreeNode node : oldNodes) {
            if (isTaskNode(node, course)) {
                nodes.add(new KotlinDirectoryNode(project, (PsiDirectory) node.getValue(), settings));
                continue;
            }

            if (node.getValue() instanceof PsiDirectory) {
                String name = ((PsiDirectory) node.getValue()).getName();
                if (name.equals(UTIL_DIR) || name.equals(OUT_DIR)) {
                    continue;
                }
            }

            nodes.add(node);
        }
        return nodes;
    }

    private boolean isTaskNode(AbstractTreeNode node, Course course) {
        if (node instanceof StudyDirectoryNode) {
            PsiDirectory value = ((StudyDirectoryNode) node).getValue();
            if (!value.getName().contains(EduNames.TASK)) {
                return false;
            }
            VirtualFile virtualFile = ((StudyDirectoryNode) node).getVirtualFile();
            if (virtualFile == null) {
                return false;
            }
            VirtualFile lessonFile = virtualFile.getParent();
            if (lessonFile != null) {
                Lesson lesson = course.getLesson(lessonFile.getName());
                return lesson != null && lesson.getTask(virtualFile.getName()) != null;
            }
        }
        return false;
    }
}
