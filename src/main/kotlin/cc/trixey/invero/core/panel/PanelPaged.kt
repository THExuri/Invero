package cc.trixey.invero.core.panel

import cc.trixey.invero.core.AgentPanel
import cc.trixey.invero.core.Layout
import cc.trixey.invero.core.Session
import cc.trixey.invero.core.icon.IconElement
import cc.trixey.invero.core.serialize.ListAgentPanelSerializer
import cc.trixey.invero.core.serialize.PosSerializer
import cc.trixey.invero.core.serialize.ScaleSerializer
import cc.trixey.invero.ui.bukkit.PanelContainer
import cc.trixey.invero.ui.bukkit.api.dsl.pagedNetesed
import cc.trixey.invero.ui.common.Pos
import cc.trixey.invero.ui.common.Scale
import cc.trixey.invero.ui.common.panel.ElementalPanel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import taboolib.common.platform.function.submitAsync

/**
 * Invero
 * cc.trixey.invero.core.panel.PanelPaged
 *
 * @author Arasple
 * @since 2023/1/19 16:56
 */
@ExperimentalSerializationApi
@Serializable
class PanelPaged(
    @Serializable(with = ScaleSerializer::class)
    override val scale: Scale,
    @Serializable(with = PosSerializer::class)
    override val locate: Pos?,
    @SerialName("default-page")
    @JsonNames("default", "def-page")
    val defaultPage: Int = 1,
    @Serializable(with = ListAgentPanelSerializer::class)
    val pages: List<AgentPanel>
) : AgentPanel() {

    override val layout: Layout? = null

    private val defaultPageIndex: Int = if (defaultPage <= 0) 0 else defaultPage - 1

    init {
        require(defaultPageIndex in pages.indices) {
            "Default Page ($defaultPage) is out of page range 1..${pages.size}"
        }
    }

    override fun invoke(parent: PanelContainer, session: Session) =
        parent.pagedNetesed(scale.pair, parent.locate(), defaultPage = defaultPageIndex) {
            pages.map { it.invoke(this, session) }

            // 临时补丁：页码变量初始化
            submitAsync {
                if (parent.isPanelValid(this@pagedNetesed)) {
                    (currentPanel as? ElementalPanel)
                        ?.elements
                        ?.forEach { (this as IconElement).renderItem() }
                }
            }
        }

}
