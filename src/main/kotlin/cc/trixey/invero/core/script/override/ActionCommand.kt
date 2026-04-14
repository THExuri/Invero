package cc.trixey.invero.core.script.override

import cc.trixey.invero.core.script.loader.InveroKetherParser
import cc.trixey.invero.core.script.parse
import taboolib.common.platform.function.console
import taboolib.library.kether.ParsedAction
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

/**
 * @author IzzelAliz
 */
class ActionCommand(val command: ParsedAction<*>, private val type: Type) : ScriptAction<Void>() {

    companion object {

        private const val OUTSIDE_INLINE_LEFT_BRACKET = "__INVERO_OUTSIDE_INLINE_LEFT_BRACKET__"
        private const val OUTSIDE_INLINE_RIGHT_BRACKET = "__INVERO_OUTSIDE_INLINE_RIGHT_BRACKET__"

        /**
         * Protect selector brackets like @e[...] from inline parser side effects.
         * Keep brackets inside {{ ... }} intact so Kether expressions still work.
         */
        private fun protectOutsideInlineBrackets(source: String): String {
            if (source.indexOf('[') == -1 && source.indexOf(']') == -1) {
                return source
            }
            val builder = StringBuilder(source.length + 32)
            var index = 0
            var inlineDepth = 0
            while (index < source.length) {
                if (index + 1 < source.length && source[index] == '{' && source[index + 1] == '{') {
                    inlineDepth++
                    builder.append("{{")
                    index += 2
                    continue
                }
                if (inlineDepth > 0 && index + 1 < source.length && source[index] == '}' && source[index + 1] == '}') {
                    inlineDepth--
                    builder.append("}}")
                    index += 2
                    continue
                }
                when (source[index]) {
                    '[' -> {
                        if (inlineDepth == 0) {
                            builder.append(OUTSIDE_INLINE_LEFT_BRACKET)
                        } else {
                            builder.append('[')
                        }
                    }

                    ']' -> {
                        if (inlineDepth == 0) {
                            builder.append(OUTSIDE_INLINE_RIGHT_BRACKET)
                        } else {
                            builder.append(']')
                        }
                    }

                    else -> builder.append(source[index])
                }
                index++
            }
            return builder.toString()
        }

        private fun restoreOutsideInlineBrackets(source: String): String {
            return source
                .replace(OUTSIDE_INLINE_LEFT_BRACKET, "[")
                .replace(OUTSIDE_INLINE_RIGHT_BRACKET, "]")
        }
    }

    enum class Type {

        PLAYER, OPERATOR, CONSOLE
    }

    override fun run(frame: ScriptFrame): CompletableFuture<Void> {
        return frame.run(command).thenAcceptAsync({

            // 虽有损耗，可以接受
            val command = it.toString().trimIndent().let { content ->
                restoreOutsideInlineBrackets(
                    frame.parse(
                        protectOutsideInlineBrackets(content),
                        true
                    )
                )
            }

            when (type) {
                Type.PLAYER -> {
                    val viewer = frame.player()
                    viewer.performCommand(command.replace("@sender", viewer.name))
                }

                Type.OPERATOR -> {
                    val viewer = frame.player()
                    val isOp = viewer.isOp
                    viewer.isOp = true
                    try {
                        viewer.performCommand(command.replace("@sender", viewer.name))
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                    viewer.isOp = isOp
                }

                Type.CONSOLE -> {
                    console().performCommand(command.replace("@sender", "console"))
                }
            }
        }, frame.context().executor)
    }

    object Parser {

        @InveroKetherParser(["command"])
        fun parser() = scriptParser {
            val command = it.nextParsedAction()
            it.mark()
            val by = try {
                it.expects("by", "with", "as")
                when (val type = it.nextToken().lowercase()) {
                    "player" -> Type.PLAYER
                    "op", "operator" -> Type.OPERATOR
                    "console", "server" -> Type.CONSOLE
                    else -> throw KetherError.NOT_COMMAND_SENDER.create(type)
                }
            } catch (ignored: Exception) {
                it.reset()
                Type.PLAYER
            }
            ActionCommand(command, by)
        }
    }

}