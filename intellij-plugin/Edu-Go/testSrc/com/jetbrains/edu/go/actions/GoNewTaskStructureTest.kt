package com.jetbrains.edu.go.actions

import com.goide.GoLanguage
import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import org.junit.Test

class GoNewTaskStructureTest : CCNewTaskStructureTestBase() {

  override val language: Language get() = GoLanguage.INSTANCE

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("go.mod")
      dir("main") {
        file("main.go", """
            package main
            
            import (
            	"fmt"
            	task "task1"
            )
            
            func main() {
            	fmt.Println(task.Sum(2, 3))
            }
          
        """)
      }
      file("task.go")
      dir("test") {
        file("task_test.go")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("task_test.go")
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("go.mod")
      file("main.go", """
        package main

        import (
        	"fmt"
        )

        func main() {
        	// Write your solution here
        	fmt.Println()
        }
        
      """)
      dir("test") {
        file("output.txt")
        file("input.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("output.txt")
        file("input.txt")
      }
    }
  )

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("go.mod")
      file("main.go", """
        package main

        import (
        	"fmt"
        )

        func main() {
        	// Write your solution here
        	fmt.Println()
        }
        
      """)
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("go.mod")
      file("main.go", """
        package main

        import (
        	"fmt"
        )

        func main() {
        	// Write your solution here
        	fmt.Println()
        }
        
      """)
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("go.mod")
      file("main.go", """
        package main

        import (
        	"fmt"
        )

        func main() {
        	// Write your solution here
        	fmt.Println()
        }
        
      """)
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
