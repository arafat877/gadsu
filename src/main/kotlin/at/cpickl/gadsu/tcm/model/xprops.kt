package at.cpickl.gadsu.tcm.model

import at.cpickl.gadsu.client.xprops.model.XProp
import at.cpickl.gadsu.client.xprops.model.XPropEnumOpt
import at.cpickl.gadsu.client.xprops.model.XPropsFactory
import at.cpickl.gadsu.client.xprops.model.XPropsFactory.enum
import at.cpickl.gadsu.client.xprops.model.XPropsFactory.opt
import at.cpickl.gadsu.service.LogConfigurator


fun main(args: Array<String>) {
    LogConfigurator(true).configureLog()
    println("Sleep.options: " + XProps.Sleep.options)
    println("Hungry.options: " + XProps.Hungry.options)
}

interface XPropsFinder {
    fun findByKey(key: String): XProp
    fun findEnumValueByKey(key: String): XPropEnumOpt
}

object XProps : XPropsFinder by XPropsFactory {

    private val eager_Sleep = SleepOpts.TiredInMorning

    enum class SleepOpts(override val opt: XPropEnumOpt) : IsEnumOption {
        NeedLess (opt("Sleep_NeedLess", "Brauche wenig Schlaf")),
        NeedMuch (opt("Sleep_NeedMuch", "Brauche viel Schlaf")),
        TiredInMorning     (opt("Sleep_TiredInMorning", "Morgens müde")),
        TiredDuringDay (opt("Sleep_TiredDuringDay", "Tagesmüdigkeit")),
        ProblemsFallAsleep (opt("Sleep_ProblemsFallAsleep", "Problem einschlafen")),
        ProblemsSleepThrough (opt("Sleep_ProblemsSleepThrough", "Durchschlafen"))
    }

    val Sleep = enum("Sleep", "Schlaf")


    private val eager_Hungry = HungryOpts.BigHunger

    enum class HungryOpts(override val opt: XPropEnumOpt) : IsEnumOption {
        BigHunger    (opt("Hungry_BigHunger", "Großen Hunger")),
        LittleHunger (opt("Hungry_LittleHunger", "Kleinen Hunger")),
        DigestionSlow (opt("Hungry_DigestionSlow", "Verdauung träge")),
        DigestionFast (opt("Hungry_DigestionFast", "Verdauung schnell")),
        StoolHard (opt("Hungry_StoolHard", "Stuhl geformt, hart")),
        StoolSoft (opt("Hungry_StoolSoft", "Stuhl ungeformt, weich"))
    }

    val Hungry = enum("Hungry", "Essen") // Verdauung

    private val eager_Taste = TasteOpts.Sweet

    enum class TasteOpts(override val opt: XPropEnumOpt) : IsEnumOption {
        Sweet (opt("Taste_Sweet", "süß")),
        Salty (opt("Taste_Salty", "salzig")),
        Sour (opt("Taste_Sour", "sauer")),
        Hot (opt("Taste_Hot", "scharf")),
        Bitter (opt("Taste_Bitter", "bitter"))
    }

    val Taste = enum("Taste", "Geschmack")

    private val eager_Temperature = TemperatureOpts.FeelWarm

    enum class TemperatureOpts(override val opt: XPropEnumOpt) : IsEnumOption {
        FeelWarm (opt("Temperature_FeelWarm", "Meist warm")),
        FeelCold (opt("Temperature_FeelCold", "Meist kalt")), // got additional textfield describing where if cold: "TemperatureColdLocation"
        SweatEasily (opt("Temperature_SweatEasily", "Schwitze leicht")),
        SweatRarely (opt("Temperature_SweatRarely", "Schwitze selten")),
        SweatDuringNight (opt("Temperature_SweatDuringNight", "Nachtschweiß")),
        SweatWhenStressed (opt("Temperature_SweatWhenStressed", "Bei Stress"))
    }

    val Temperature = enum("Temperature", "Temperatur") // verhalten

    private val eager_Menstruation = MenstruationOpts.Strong

    enum class MenstruationOpts(override val opt: XPropEnumOpt) : IsEnumOption {
        // see text field: MenstruationTimes
        Strong (opt("Menstruation_Strong", "Kräftig")),
        Little (opt("Menstruation_Little", "Schwach")),
        BloodDark (opt("Menstruation_BloodDark", "Blut dunkel")),
        BloodBright (opt("Menstruation_BloodBright", "Blut hell, dünn")),
        PMS (opt("Menstruation_PMS", "PMS")),
        Pill (opt("Menstruation_Pill", "Pille, hormonell")),
        Menopause (opt("Menstruation_Menopause", "Wechseljahre")),
        Operations (opt("Menstruation_Operations", "Operationen"))
    }

    val Menstruation = enum("Menstruation", "Zyklus")


    private val eager_Tongue = TongueOpts.Thick

    enum class TongueOpts(override val opt: XPropEnumOpt) : IsEnumOption {
        Thick (opt("Tongue_Thick", "Dick")),
        Thin (opt("Tongue_Thin", "Dünn")),
        Long (opt("Tongue_Long", "Lang")),
        Short (opt("Tongue_Short", "Kurz")),
        Swollen (opt("Tongue_Swollen", "Geschwollen")),
        Red (opt("Tongue_Red", "Rot")),
        Bright (opt("Tongue_Bright", "Hell, blass")),
        YellowFilm (opt("Tongue_YellowFilm", "gelber Belag")),
        WhiteFilm (opt("Tongue_WhiteFilm", "weisser Belag")),
        TeethImprints (opt("Tongue_", "Zahnabdrücke")),
        Dry (opt("Tongue_Dry", "trocken")),
        Wet (opt("Tongue_Wet", "feucht")),
    }

    val Tongue = enum("Tongue", "Zunge")

    private val eager_Pulse = PulseOpts.Superficial

    enum class PulseOpts(override val opt: XPropEnumOpt) : IsEnumOption {
        Superficial (opt("Pulse_Superficial", "oberflächlich")),
        Deep (opt("Pulse_Deep", "tief")),
        Fast (opt("Pulse_Fast", "schnell")),
        Slow (opt("Pulse_Slow", "langsam")),
        Full (opt("Pulse_Full", "voll")),
        Empty (opt("Pulse_Empty", "leer"))
    }

    val Pulse = enum("Pulse", "Puls")

    private val eager_ChiLocation = ChiLocationOpts.Top

    enum class ChiLocationOpts(override val opt: XPropEnumOpt) : IsEnumOption {
        Top (opt("ChiLocation_Top", "oben")),
        Bottom (opt("ChiLocation_Bottom", "unten")),
        Weak (opt("ChiLocation_Weak", "leicht")),
        Strong (opt("ChiLocation_Strong", "schwer")),
        Outside (opt("ChiLocation_Outside", "nach aussen")),
        Inside (opt("ChiLocation_Inside", "nach innen"))
    }

    val ChiLocation = enum("ChiLocation", "Qi Status") // Energiekonzentration

    private val eager_BodyConception = BodyConceptionOpts.TissueTight

    enum class BodyConceptionOpts(override val opt: XPropEnumOpt) : IsEnumOption {
        TissueTight (opt("BodyConception_TissueTight", "Gewebe fest")),
        TissueSoft (opt("BodyConception_TissueSoft", "Gewebe weich")),
        Muscular (opt("BodyConception_Muscular", "muskulös")),
        Gentle (opt("BodyConception_Gentle", "zart")),
        Fat (opt("BodyConception_Fat", "massig")),
        Slim (opt("BodyConception_Slim", "schlank")),
        JointsStiff (opt("BodyConception_JointsStiff", "Gelenke steif")),
        JointsFlexible (opt("BodyConception_JointsFlexible", "Gelenke flexibel"))
    }

    val BodyConception = enum("BodyConception", "Körperbild")

}

interface IsEnumOption {
    val opt: XPropEnumOpt
}
