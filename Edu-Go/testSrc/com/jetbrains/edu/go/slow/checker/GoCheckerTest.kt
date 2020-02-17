package com.jetbrains.edu.go.slow.checker

import com.goide.GoLanguage
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course

class GoCheckerTest : GoCheckersTestBase() {
  override fun createCourse(): Course {
    return course(language = GoLanguage.INSTANCE) {
      lesson {
        eduTask("Edu") {
          goTaskFile("task.go", """
            package task

            // todo: replace this with an actual task
            func Sum(a, b int) int {
              return a + b
            }
          """)
          taskFile("go.mod", """
            module task1
          """)
          goTaskFile("test/task_test.go", """
            package test
            
            import (
            	task "task1"
            	"testing"
            )
            
            //todo: replace this with an actual test
            func TestSum(t *testing.T) {
            	type args struct {
            		a int
            		b int
            	}
            	tests := []struct {
            		name string
            		args args
            		want int
            	}{
            		{"1", args{1, 1}, 2},
            		{"2", args{1, 2}, 3},
            	}
            	for _, tt := range tests {
            		t.Run(tt.name, func(t *testing.T) {
            			if got := task.Sum(tt.args.a, tt.args.b); got != tt.want {
            				t.Errorf("Sum() = %v, want %v", got, tt.want)
            			}
            		})
            	}
            }

          """)
        }
        outputTask("Output") {
          goTaskFile("main.go", """
              package main
              import "fmt"

              func main() {
	              fmt.Println("Yay!")
              }
          """)
          taskFile("go.mod", """
            module task2
          """)
          taskFile("output.txt","Yay!")
        }
      }
    }
  }

  fun `test go course`() {
    CheckActionListener.expectedMessage { CheckUtils.CONGRATULATIONS }
    doTest()
  }
}