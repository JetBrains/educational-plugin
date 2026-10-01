package com.jetbrains.edu.socialMedia

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.EduTestAware
import org.jetbrains.annotations.TestOnly

/**
 * Stores course ids which were already asked to post in social media
 */
@Service(Service.Level.APP)
@State(name = "SocialMediaPostManager", storages = [Storage("other.xml")])
class SocialMediaPostManager : SimplePersistentStateComponent<SocialMediaPostManager.State>(State()), EduTestAware {

  @TestOnly
  override fun cleanUpState() {
    state.askedToPost.clear()
  }

  companion object {
    fun getInstance(): SocialMediaPostManager = service()

    fun needToAskedToPost(courseId: Int): Boolean = courseId != 0 && courseId !in getInstance().state.askedToPost

    fun setAskedToPost(courseId: Int) {
      val service = getInstance()
      // All local courses have the same id - 0.
      // So, it doesn't make sense to store it
      if (courseId == 0 || courseId in service.state.askedToPost) return
      service.state.askedToPost += courseId
    }
  }

  class State : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    val askedToPost: MutableList<Int> by list()
  }
}
