package com.lwbrowser

class TabsManager {
    private val tabs = mutableListOf<Tab>()
    private var currentIndex = -1

    val count: Int get() = tabs.size
    val all: List<Tab> get() = tabs
    val current: Tab?
        get() = if (currentIndex in tabs.indices) tabs[currentIndex] else null

    fun add(tab: Tab): Int {
        tabs.add(tab)
        currentIndex = tabs.size - 1
        return currentIndex
    }

    fun newTab(url: String = Prefs.startPage): Tab {
        val tab = Tab(url = url)
        add(tab)
        return tab
    }

    fun select(index: Int): Tab? {
        if (index !in tabs.indices) return null
        currentIndex = index
        return tabs[currentIndex]
    }

    fun indexOf(tab: Tab): Int = tabs.indexOf(tab)

    fun closeAt(index: Int): Tab? {
        if (index !in tabs.indices) return null
        tabs.removeAt(index).destroy()
        if (tabs.isEmpty()) {
            currentIndex = -1
            return null
        }
        if (currentIndex >= tabs.size) currentIndex = tabs.size - 1
        else if (currentIndex > index) currentIndex--
        return tabs[currentIndex]
    }

    fun close(tab: Tab): Tab? {
        val i = tabs.indexOf(tab)
        if (i < 0) return current
        return closeAt(i)
    }

    fun closeAll() {
        tabs.forEach { it.destroy() }
        tabs.clear()
        currentIndex = -1
    }

    fun move(from: Int, to: Int) {
        if (from !in tabs.indices || to !in tabs.indices) return
        val item = tabs.removeAt(from)
        tabs.add(to, item)
        currentIndex = to
    }
}
