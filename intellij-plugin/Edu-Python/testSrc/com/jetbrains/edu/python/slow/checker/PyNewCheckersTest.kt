package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.python.PythonLanguage
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyNewCheckersTest : PyCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = PythonLanguage.INSTANCE, environment = "unittest") {
      lesson {
        eduTask("Edu") {
          pythonTaskFile("task.py", """
            def sum(a, b):
                return a + b
            """)
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", """
              import unittest
              from task import sum
              class TestCase(unittest.TestCase):
                  def test_add(self):
                      self.assertEqual(sum(1, 2), 3, msg="error")
              """)
          }
        }
        eduTask("EduWithIgnoredTest") {
          pythonTaskFile("task.py", """
            def sum(a, b):
                return a + b
            """)
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", """
              import unittest
              from task import sum
              class TestCase(unittest.TestCase):
                  def test_add(self):
                      self.assertEqual(sum(1, 2), 3, msg="error")
                     
                  @unittest.skip   
                  def test_ignored(self):
                      self.assertEqual(sum(1, 2), 4, msg="error")    
              """)
          }
        }
        eduTask("EduWithCustomRunConfiguration") {
          pythonTaskFile("task.py", """
            import os
            
            
            def hello():
                return os.getenv("EXAMPLE_ENV")
            """)
          dir("tests") {
            taskFile("__init__.py")
            taskFile("tests.py", """
              import unittest
              
              from task import hello
              
              
              class TestCase(unittest.TestCase):
                  def test_hello(self):
                      self.assertEqual(hello(), "Hello!", msg="Message")
              """)
          }
          dir("runConfigurations") {
            xmlTaskFile("CustomCheck.run.xml", $$"""
              <component name="ProjectRunConfigurationManager">
                <configuration name="CustomCheck" type="tests" factoryName="Unittests">
                  <module name="Python Course14" />
                  <option name="INTERPRETER_OPTIONS" value="" />
                  <option name="PARENT_ENVS" value="true" />
                  <envs>
                    <env name="EXAMPLE_ENV" value="Hello!" />
                  </envs>
                  <option name="SDK_HOME" value="$PROJECT_DIR$/.idea/VirtualEnvironment/bin/python" />
                  <option name="WORKING_DIRECTORY" value="$TASK_DIR$" />
                  <option name="IS_MODULE_SDK" value="true" />
                  <option name="ADD_CONTENT_ROOTS" value="true" />
                  <option name="ADD_SOURCE_ROOTS" value="true" />
                  <EXTENSION ID="PythonCoverageRunConfigurationExtension" runner="coverage.py" />
                  <option name="_new_pattern" value="&quot;&quot;" />
                  <option name="_new_additionalArguments" value="&quot;&quot;" />
                  <option name="_new_target" value="&quot;tests.TestCase&quot;" />
                  <option name="_new_targetType" value="&quot;PYTHON&quot;" />
                  <method v="2" />
                </configuration>
              </component>            
            """)
          }
        }
        outputTask("Output") {
          pythonTaskFile("hello_world.py", """print("Hello, World!")""")
          taskFile("output.txt") {
            withText("Hello, World!\n")
          }
        }
      }
    }
  }

  @Test
  fun `test python course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        else -> null
      }
    }
    doTest()
  }
}
