package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import org.junit.Test

class CoursesInfosStorageSerializationTest : EduSettingsServiceTestBase() {

  @Test
  fun `test deserialize first version courses storage`() {
    val coursesStorage = CoursesStorage()
    coursesStorage.loadStateAndCheck($$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="courseMode" value="Study" />
            <option name="description" value="Introduction course to Python." />
            <option name="humanLanguage" value="English" />
            <option name="id" value="238" />
            <option name="location" value="$USER_HOME$/IdeaProjects/Introduction to Python" />
            <option name="name" value="Introduction to Python" />
            <option name="programmingLanguageId" value="Python" />
            <option name="programmingLanguageVersion" value="2.7" />
            <option name="type" value="PyCharm" />
          </course>
        </courses>
      </UserCoursesState>
    """, $$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="Introduction course to Python." />
            <option name="id" value="238" />
            <option name="location" value="$USER_HOME$/IdeaProjects/Introduction to Python" />
            <option name="name" value="Introduction to Python" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguageVersion" value="2.7" />
            <option name="programmingLanguageId" value="Python" />
          </course>
        </courses>
      </UserCoursesState>
    """)
  }

  @Test
  fun `test deserialize course with default parameters`() {
    val coursesStorage = CoursesStorage()
    coursesStorage.loadStateAndCheck($$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="The examples and exercises accompanying the AtomicKotlin book" />
            <option name="id" value="20403" />
            <option name="location" value="$USER_HOME$/IdeaProjects/AtomicKotlin" />
            <option name="name" value="AtomicKotlin" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguageId" value="kotlin" />
          </course>
        </courses>
      </UserCoursesState>
    """)
  }

  @Test
  fun `test deserialize old language version`() {
    val coursesStorage = CoursesStorage()
    coursesStorage.loadStateAndCheck($$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="The examples and exercises accompanying the AtomicKotlin book" />
            <option name="id" value="20403" />
            <option name="location" value="$USER_HOME$/IdeaProjects/AtomicKotlin" />
            <option name="name" value="AtomicKotlin" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguage" value="Python 3.7" />
          </course>
        </courses>
      </UserCoursesState>
    """, $$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="The examples and exercises accompanying the AtomicKotlin book" />
            <option name="id" value="20403" />
            <option name="location" value="$USER_HOME$/IdeaProjects/AtomicKotlin" />
            <option name="name" value="AtomicKotlin" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguageVersion" value="3.7" />
            <option name="programmingLanguageId" value="Python" />
          </course>
        </courses>
      </UserCoursesState>
    """)
  }

  @Test
  fun `test deserialize new language version`() {
    val coursesStorage = CoursesStorage()
    coursesStorage.loadStateAndCheck($$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="The examples and exercises accompanying the AtomicKotlin book" />
            <option name="id" value="20403" />
            <option name="location" value="$USER_HOME$/IdeaProjects/AtomicKotlin" />
            <option name="name" value="AtomicKotlin" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguageVersion" value="3.7" />
            <option name="programmingLanguageId" value="Python" />
          </course>
        </courses>
      </UserCoursesState>
    """)
  }

  @Test
  fun `test deserialize new and old language version`() {
    val coursesStorage = CoursesStorage()
    coursesStorage.loadStateAndCheck($$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="The examples and exercises accompanying the AtomicKotlin book" />
            <option name="id" value="20403" />
            <option name="location" value="$USER_HOME$/IdeaProjects/AtomicKotlin" />
            <option name="name" value="AtomicKotlin" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguage" value="Python 3.6" />
            <option name="programmingLanguageVersion" value="3.7" />
          </course>
        </courses>
      </UserCoursesState>
    """, $$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="The examples and exercises accompanying the AtomicKotlin book" />
            <option name="id" value="20403" />
            <option name="location" value="$USER_HOME$/IdeaProjects/AtomicKotlin" />
            <option name="name" value="AtomicKotlin" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguageVersion" value="3.7" />
            <option name="programmingLanguageId" value="Python" />
          </course>
        </courses>
      </UserCoursesState>
    """)
  }

  @Test
  fun `test serialize course with default parameters`() {
    val coursesStorage = CoursesStorage()
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book"
    ) { }.apply {
      id = 20403
      languageId = EduFormatNames.PYTHON
    }
    coursesStorage.addCourse(course, $$"$USER_HOME$/IdeaProjects/AtomicKotlin")

    coursesStorage.checkState($$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="The examples and exercises accompanying the AtomicKotlin book" />
            <option name="id" value="20403" />
            <option name="location" value="$USER_HOME$/IdeaProjects/AtomicKotlin" />
            <option name="marketplace" value="false" />
            <option name="name" value="AtomicKotlin" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguageId" value="Python" />
          </course>
        </courses>
      </UserCoursesState>
    """)
  }

  @Test
  fun `test serialize language version`() {
    val coursesStorage = CoursesStorage()
    val course = course(
      "AtomicKotlin",
      description = "The examples and exercises accompanying the AtomicKotlin book"
    ) { }.apply {
      id = 20403
      languageId = EduFormatNames.PYTHON
      languageVersion = "3.7"
    }
    coursesStorage.addCourse(course, $$"$USER_HOME$/IdeaProjects/AtomicKotlin")

    coursesStorage.checkState($$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="The examples and exercises accompanying the AtomicKotlin book" />
            <option name="id" value="20403" />
            <option name="location" value="$USER_HOME$/IdeaProjects/AtomicKotlin" />
            <option name="marketplace" value="false" />
            <option name="name" value="AtomicKotlin" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguageVersion" value="3.7" />
            <option name="programmingLanguageId" value="Python" />
          </course>
        </courses>
      </UserCoursesState>
    """)
  }

  @Test
  fun `test serialize hyperskill and stepik courses`() {
    val coursesStorage = CoursesStorage()
    coursesStorage.loadStateAndCheck($$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="A regular Edu course" />
            <option name="id" value="100" />
            <option name="location" value="$USER_HOME$/IdeaProjects/EduCourse" />
            <option name="name" value="EduCourse" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguageId" value="Python" />
          </course>
          <course>
            <option name="description" value="A Hyperskill course" />
            <option name="id" value="200" />
            <option name="location" value="$USER_HOME$/IdeaProjects/HyperskillCourse" />
            <option name="name" value="HyperskillCourse" />
            <option name="type" value="Hyperskill" />
            <option name="programmingLanguageId" value="Python" />
          </course>
          <course>
            <option name="description" value="A Stepik course" />
            <option name="id" value="300" />
            <option name="location" value="$USER_HOME$/IdeaProjects/StepikCourse" />
            <option name="name" value="StepikCourse" />
            <option name="type" value="Stepik" />
            <option name="programmingLanguageId" value="Python" />
          </course>
          <course>
            <option name="description" value="A Marketplace course" />
            <option name="id" value="400" />
            <option name="location" value="$USER_HOME$/IdeaProjects/MarketplaceCourse" />
            <option name="marketplace" value="true" />
            <option name="name" value="MarketplaceCourse" />
            <option name="type" value="Marketplace" />
            <option name="programmingLanguageId" value="Python" />
          </course>
        </courses>
      </UserCoursesState>
    """, $$"""
      <UserCoursesState>
        <courses>
          <course>
            <option name="description" value="A regular Edu course" />
            <option name="id" value="100" />
            <option name="location" value="$USER_HOME$/IdeaProjects/EduCourse" />
            <option name="name" value="EduCourse" />
            <option name="type" value="PyCharm" />
            <option name="programmingLanguageId" value="Python" />
          </course>
          <course>
            <option name="description" value="A Hyperskill course" />
            <option name="id" value="200" />
            <option name="location" value="$USER_HOME$/IdeaProjects/HyperskillCourse" />
            <option name="name" value="HyperskillCourse" />
            <option name="type" value="Hyperskill" />
            <option name="programmingLanguageId" value="Python" />
          </course>
          <course>
            <option name="description" value="A Stepik course" />
            <option name="id" value="300" />
            <option name="location" value="$USER_HOME$/IdeaProjects/StepikCourse" />
            <option name="name" value="StepikCourse" />
            <option name="type" value="Stepik" />
            <option name="programmingLanguageId" value="Python" />
          </course>
          <course>
            <option name="description" value="A Marketplace course" />
            <option name="id" value="400" />
            <option name="location" value="$USER_HOME$/IdeaProjects/MarketplaceCourse" />
            <option name="name" value="MarketplaceCourse" />
            <option name="type" value="Marketplace" />
            <option name="programmingLanguageId" value="Python" />
          </course>
        </courses>
      </UserCoursesState>
    """)
  }
}
