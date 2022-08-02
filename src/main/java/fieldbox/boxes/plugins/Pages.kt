package fieldbox.boxes.plugins

import field.utility.Dict
import field.utility.Pair
import fieldbox.boxes.Box
import fieldbox.boxes.Drawing
import fieldbox.io.IO
import fielded.Commands
import java.util.function.Supplier
import kotlin.jvm.optionals.getOrNull

/**
 * Plugin for rendering the stuff _above_ root visible and editable. Perhaps this replaces "disconnected" ?
 */
@OptIn(ExperimentalStdlibApi::class)
class Pages(val root: Box) : Box() {

    var currentPage = -1

    companion object {
        @JvmStatic
        var _pages = Dict.Prop<Pages>("_pages").toCanon<Pages>()
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
            ""
        })
        root.find(StatusBar.statusBar, root.upwards()).findFirst().getOrNull()?.update()
    }


    fun allPage() {
        currentPage = -1
        Drawing.dirty(root)
        this.properties.putToMap(StatusBar.statuses, "page", Supplier {
            "Viewing all pages"
        })
        root.find(StatusBar.statusBar, root.both()).findFirst().getOrNull()?.update()
    }

    fun nextPage() {
        currentPage = Math.max(1, currentPage + 1)
        Drawing.dirty(root)
        this.properties.putToMap(StatusBar.statuses, "page", Supplier {
            "Viewing Page $currentPage"
        })
        root.find(StatusBar.statusBar, root.both()).findFirst().getOrNull()?.update()
    }

    fun previousPage() {
        currentPage = Math.max(1, currentPage - 1)
        Drawing.dirty(root)
        this.properties.putToMap(StatusBar.statuses, "page", Supplier {
            "Viewing Page $currentPage"
        })
        root.find(StatusBar.statusBar, root.both()).findFirst().getOrNull()?.update()
    }

    fun moveToCurrentPage(b: Box) {
        if (currentPage>0) {
            b.properties.put(Planes.plane, "__page_${currentPage}__")
        }
        else
            b.properties.remove(Planes.plane)
        Drawing.dirty(root)
    }

    fun nextPageName(): String {
        return "Page ${Math.max(1, currentPage + 1)}"
    }

    fun prevPageName(): String? {
        if (currentPage <= 1) return null
        return "Page ${Math.max(1, currentPage - 1)}"
    }


}