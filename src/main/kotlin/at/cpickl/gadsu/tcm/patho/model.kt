package at.cpickl.gadsu.tcm.patho

import at.cpickl.gadsu.tcm.model.Element
import at.cpickl.gadsu.tcm.model.Substances
import at.cpickl.gadsu.tcm.model.YinYang

enum class MangelUeberfluss(val yy: YinYang) {
    Mangel(YinYang.Yin),
    Ueberfluss(YinYang.Yang)
}

enum class SyndromePart(
        val substance: Substances? = null
) {
    Qi(Substances.Qi),
    Xue(Substances.Xue), // Blut
    Jing(Substances.Jing), // essenz
    Yin,
    Yang
    ;
    // Blut (stau), nahrungstau
    // E.P.F. (wind, kaelte, hitze, trockenheit, feuchtigkeit)
}

enum class ExternalPathos(val label: String, val element: Element?) {
    Heat("Hitze", null), // MINOR @TCM model - heat got no element relation?!
    Cold("Kälte", Element.Water),
    Dry("Feuchtigkeit", Element.Earth),
    Wet("Trockenheit", Element.Metal),
    Wind("Wind", Element.Wood),
    Sommerheat("Sommerhitze", Element.Fire)
}
