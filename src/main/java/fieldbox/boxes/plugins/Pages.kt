package fieldbox.boxes.plugins

import field.linalg.Vec2
import field.utility.Dict
import field.utility.Pair
import fieldbox.boxes.*
import fieldbox.boxes.MarkingMenus.MenuSpecification
import fieldbox.io.IO
import fielded.Commands
import java.util.*
import java.util.function.Supplier
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

private fun <T> Optional<T>.getOrNull(): T? {
    if (this.isEmpty) return null
    return this.get()
}


/**
 * Plugin for rendering the stuff _above_ root visible and editable. Perhaps this replaces "disconnected" ?
 */
@OptIn(ExperimentalStdlibApi::class)
class Pages(val root: Box) : Box() {

    var currentPage = -1

    var notedSelections = mutableMapOf<Int, Set<String>>()

    companion object {
        @JvmStatic
        var _pages = Dict.Prop<Pages>("pages").toCanon<Pages>()
            .doc<Pages>("the pages plugin")
    }

    init {
        properties.put(_pages, this)
        IO.persist(Planes.plane)

        properties.put(Commands.commands,
            Supplier {
                val m: MutableMap<Pair<String, String>, Runnable> = LinkedHashMap()

                m[Pair(
                    "Next page '${nextPageName()}'",
                    "Flips to the next 'page' of this document (creating a new page if needed)"
                )] = Runnable {
                    install()
                    nextPage()
                }


                if (selection().size != 0) {
                    m[Pair(
                        "Move selection to page '${nextPageName()}'",
                        "Moves the currently selected boxes to the next page"
                    )] = Runnable {
                        install()
                        selectedBoxes().forEach { moveToNextPage(it) }
                    }
                }

                if (selection().size != 0) {
                    m[Pair(
                        "Move selection to no page",
                        "Sets the selection to have the default page"
                    )] = Runnable {
                        install()
                        selectedBoxes().forEach { moveToNoPage(it) }
                    }
                }


                if (prevPageName() != null)
                    m[Pair(
                        "Previous page '${prevPageName()}'",
                        "Flips to the previous 'page' of this document"
                    )] = Runnable {
                        install()
                        previousPage()
                    }

                if (currentPage != -1)
                    m[Pair(
                        "View all pages together",
                        "Shows every box at the same time, regardless of what page it's on"
                    )] = Runnable {
                        install()
                        allPage()
                    }

                if (currentPage != 0)
                    m[Pair(
                        "View boxes with unset page",
                        "Shows all boxes that aren't on a specific page"
                    )] = Runnable {
                        install()
                        noPage()
                    }

                m
            })

    }

    fun install() {

        root.properties.put(Planes.selectPlane, java.util.function.Function { box ->
            val p = box.properties.get(Planes.plane)
            if (p == null) {
                if (currentPage == 0) {
                    if (box.properties.has(frame) && box.properties.has(name))
                        return@Function 1
                    return@Function 0
                }
                if (currentPage == -1) {
                    if (box.properties.has(frame) && box.properties.has(name))
                        return@Function 1
                    return@Function 0
                }
                return@Function 0
            } else if (p.contains("__always__")) {
                return@Function 1
            } else if (currentPage == -1) {
                return@Function 1
            } else if (p.contains("__page_${currentPage}__")) {
                return@Function 1
            }
            return@Function 0
        })

    }


    fun noPage() {
        currentPage = 0
        Drawing.dirty(root)

        this.properties.putToMap(StatusBar.statuses, "page", Supplier {
            "No Page"
        })
        root.find(StatusBar.statusBar, root.upwards()).findFirst().getOrNull()?.update()
    }


    fun allPage() {
        store()
        currentPage = -1
        Drawing.dirty(root)
        this.properties.putToMap(StatusBar.statuses, "page", Supplier {
            "Viewing all pages"
        })
        root.find(StatusBar.statusBar, root.both()).findFirst().getOrNull()?.update()
        restore()
    }

    fun nextPage() {
        store()
        install()
        currentPage = Math.max(1, currentPage + 1)
        Drawing.dirty(root)
        this.properties.putToMap(StatusBar.statuses, "page", Supplier {
            "Viewing Page $currentPage"
        })
        root.find(StatusBar.statusBar, root.both()).findFirst().getOrNull()?.update()
        restore()
    }

    fun previousPage() {
        store()
        currentPage = Math.max(0, currentPage - 1)
        if (currentPage == 0) {
            noPage()
        } else {
            Drawing.dirty(root)
            this.properties.putToMap(StatusBar.statuses, "page", Supplier {
                "Viewing Page $currentPage"
            })
            root.find(StatusBar.statusBar, root.both()).findFirst().getOrNull()?.update()
        }
        restore()
    }

    fun moveToCurrentPage(b: Box) {
        if (currentPage > 0) {
            b.properties.put(Planes.plane, "__page_${currentPage}__")
        } else
            b.properties.remove(Planes.plane)
        Drawing.dirty(root)
        store()
    }

    fun moveToNoPage(b: Box) {
        b.properties.remove(Planes.plane)
        Drawing.dirty(root)
        store()
    }

    fun moveToNextPage(b: Box) {
        b.properties.put(Planes.plane, "__page_${Math.max(1, currentPage + 1)}__")
        Drawing.dirty(root)
        store()
    }

    fun nextPageName(): String {
        return "Page ${Math.max(1, currentPage + 1)}"
    }

    fun prevPageName(): String? {
        if (currentPage <= 1) return null
        return "Page ${Math.max(1, currentPage - 1)}"
    }


    fun store() {
        notedSelections.put(currentPage, selection().toSet())
    }

    fun restore() {
        select(notedSelections.getOrDefault(currentPage, emptySet()).toList())
    }

    fun selection(): List<String> {
        return root.breadthFirst(root.both())
            .filter { x: Box ->
                x.properties.isTrue(
                    Mouse.isSelected,
                    false
                ) && !x.properties.isTrue(Mouse.isSticky, false)
            }.map {
                it.properties.get(IO.id)
            }.toList()
    }

    fun selectedBoxes(): List<Box> {
        return root.breadthFirst(root.both())
            .filter { x: Box ->
                x.properties.isTrue(
                    Mouse.isSelected,
                    false
                ) && !x.properties.isTrue(Mouse.isSticky, false)
            }.toList()
    }

    fun select(l: List<String>) {
        var m = selection()
        var a = LinkedHashSet(m)
        var b = LinkedHashSet(l)

        a.removeAll(l)
        b.removeAll(m)

        a.forEach { id ->
            root.breadthFirst(root.downwards()).filter { it.properties.has(IO.id) && it.properties.get(IO.id) == id }
                .findFirst().getOrNull()?.let {
                    Callbacks.transition(it, Mouse.isSelected, false, false, Callbacks.onSelect, Callbacks.onDeselect)
                }
        }
        b.forEach { id ->
            root.breadthFirst(root.downwards()).filter { it.properties.has(IO.id) && it.properties.get(IO.id) == id }
                .findFirst().getOrNull()?.let {
                    Callbacks.transition(it, Mouse.isSelected, true, false, Callbacks.onSelect, Callbacks.onDeselect)
                }
        }
    }

}


