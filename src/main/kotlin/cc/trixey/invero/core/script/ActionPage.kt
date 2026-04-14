package cc.trixey.invero.core.script

import cc.trixey.invero.core.script.PageOperator.*
import cc.trixey.invero.core.script.loader.InveroKetherParser
import cc.trixey.invero.ui.common.panel.PagedPanel
import taboolib.common5.cint
import taboolib.module.kether.combinationParser
import taboolib.module.kether.run

/**
 * Invero
 * cc.trixey.invero.core.script.kether.ActionPage
 *
 * @author Arasple
 * @since 2023/2/10 14:05
 */
object ActionPage {

    @InveroKetherParser(["page"])
    internal fun parserPage() = combinationParser {
        it.group(
            // next, previous, set, get, max, isFirst, isLast
            symbol(),
            // to,by
            command("to", "by", then = action()).option().defaultsTo(null)
        ).apply(it) { type, value ->
            now {
                val panel = findNearstPanelRecursively<PagedPanel>() ?: return@now ""
                when (val operator = PageOperator.of(type)) {
                    // Expose page number as 1-based to scripts/configs.
                    GET -> panel.pageIndex + 1
                    GET_MAX -> panel.maxPageIndex + 1
                    IS_FIRST_PAGE -> panel.pageIndex == 0
                    IS_LAST_PAGE -> panel.pageIndex == panel.maxPageIndex
                    else -> {
                        // Keep step count (next/previous) as-is, but map set target to 0-based index.
                        val target = value?.let { v -> run(v).getNow(0) }?.cint?.coerceAtLeast(1) ?: 1

                        when (operator) {
                            SET -> panel.pageIndex = target - 1
                            NEXT -> panel.nextPage(target)
                            PREVIOUS -> panel.previousPage(target)
                            else -> error("Unreachable")
                        }
                    }
                }
            }
        }
    }

}