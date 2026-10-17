package com.belge.goruntuleyici

class SlideRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val size: Float = 0f,
    val color: Int = 0
)

class SlidePara(
    val runs: List<SlideRun>,
    val align: Int = 0,
    val bullet: Boolean = false
) {
    val plain: String = runs.joinToString("") { it.text }
}

class SlideShape(
    val x: Long,
    val y: Long,
    val w: Long,
    val h: Long,
    val fill: Int,
    val paras: List<SlidePara>,
    val image: String?,
    val table: List<List<List<SlidePara>>>?,
    val title: Boolean
) {
    fun text(): String {
        val a = paras.joinToString("\n") { it.plain }
        val t = table?.joinToString("\n") { r ->
            r.joinToString(" ") { c -> c.joinToString(" ") { p -> p.plain } }
        } ?: ""
        return a + "\n" + t
    }
}

class SlideData(val shapes: List<SlideShape>, val bg: Int) {
    val text: String = shapes.joinToString("\n") { it.text() }
}

class Deck(
    val path: String,
    val widthEmu: Long,
    val heightEmu: Long,
    val slides: List<SlideData>
)

object DeckLoader {

    fun load(path: String, ext: String): Deck {
        return try {
            when (ext) {
                "pptx", "pptm", "ppsx", "potx" -> PptxPackage.load(path)
                "odp" -> OdpParser.parse(path)
                else -> errorDeck(path, "Bu tür desteklenmiyor")
            }
        } catch (t: Throwable) {
            errorDeck(path, "Hata: " + t.javaClass.simpleName + " - " + t.message)
        }
    }

    private fun errorDeck(path: String, msg: String): Deck {
        val para = SlidePara(listOf(SlideRun(msg, size = 18f)))
        val shape = SlideShape(
            457200L, 457200L, 11277600L, 1000000L, 0,
            listOf(para), null, null, false
        )
        return Deck(path, 12192000L, 6858000L, listOf(SlideData(listOf(shape), 0)))
    }
}
