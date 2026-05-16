package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.courseFormat.StudyItem

sealed interface ItemUpdater<T : StudyItem> {
  fun T.isOutdated(remoteItem: T): Boolean
}

interface MarketplaceItemUpdater<T : StudyItem> : ItemUpdater<T> {
  // For tasks in the marketplace, there is no supported updateDate
  override fun T.isOutdated(remoteItem: T): Boolean = false
}