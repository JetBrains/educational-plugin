package com.jetbrains.edu.learning.courseFormat

abstract class ItemContainer : StudyItem() {
  var items: List<StudyItem>
    get() = _items
    set(value) {
      _items = value.toMutableList()
    }

  private var _items = mutableListOf<StudyItem>()
  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    parent = parentItem
    for ((i, item) in items.withIndex()) {
      item.index = i + 1
      item.init(this, isRestarted)
    }
  }

  fun getItem(name: String): StudyItem? {
    return items.firstOrNull { it.name == name }
  }

  fun addItem(item: StudyItem) {
    _items.add(item)
  }

  fun addItem(index: Int, item: StudyItem) {
    _items.add(index, item)
  }

  fun replaceItem(existingItem: StudyItem, newItem: StudyItem) {
    val index = _items.indexOf(existingItem)
    if (index < 0) return
    _items[index] = newItem
  }

  fun removeItem(item: StudyItem) {
    _items.remove(item)
  }

  open fun sortItems() {
    _items.sortBy { it.index }
  }
}
