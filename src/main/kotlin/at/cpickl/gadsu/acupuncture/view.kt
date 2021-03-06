package at.cpickl.gadsu.acupuncture

import at.cpickl.gadsu.service.SearchableList
import at.cpickl.gadsu.tcm.model.Element
import at.cpickl.gadsu.view.ViewNames
import at.cpickl.gadsu.view.components.DefaultCellView
import at.cpickl.gadsu.view.components.MyFrame
import at.cpickl.gadsu.view.components.MyList
import at.cpickl.gadsu.view.components.MyListCellRenderer
import at.cpickl.gadsu.view.components.MyListModel
import at.cpickl.gadsu.view.components.inputs.HtmlEditorPane
import at.cpickl.gadsu.view.components.inputs.SearchTextField
import at.cpickl.gadsu.view.components.panels.GridPanel
import at.cpickl.gadsu.view.swing.ClosableWindow
import at.cpickl.gadsu.view.swing.Pad
import at.cpickl.gadsu.view.swing.bold
import at.cpickl.gadsu.view.swing.enforceSize
import at.cpickl.gadsu.view.swing.enforceWidth
import at.cpickl.gadsu.view.swing.opaque
import at.cpickl.gadsu.view.swing.registerCloseOnEscapeOrShortcutW
import at.cpickl.gadsu.view.swing.scrolled
import at.cpickl.gadsu.view.swing.transparent
import at.cpickl.gadsu.view.toHexString
import com.google.common.eventbus.EventBus
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import javax.inject.Inject
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class AcupunctureFrame @Inject constructor(
        val list: AcupunctureList
) : MyFrame("Akupunkturpunkte"), ClosableWindow {

    val inpSearch = SearchTextField()

    private val log = LoggerFactory.getLogger(javaClass)
    private val bigText = HtmlEditorPane()

    init {
        val panel = GridPanel()
        registerCloseOnEscapeOrShortcutW()

        inpSearch.enforceWidth(200)
        // NOOOOO! list.enforceWidth(200)

        // SEARCH
        panel.c.weightx = 0.0
        panel.c.weighty = 0.0
        panel.c.fill = GridBagConstraints.HORIZONTAL
        panel.add(inpSearch)

        // DETAIL
        panel.c.gridx++
        panel.c.insets = Pad.LEFT
        panel.c.gridheight = 2
        panel.c.weightx = 1.0
        panel.c.weighty = 1.0
        panel.c.fill = GridBagConstraints.BOTH
        panel.add(bigText)

        // MASTER LIST
        panel.c.insets = Pad.NONE
        panel.c.gridx = 0
        panel.c.gridy++
        panel.c.gridheight = 1
        panel.c.weightx = 0.0
        panel.c.weighty = 1.0
        panel.c.fill = GridBagConstraints.BOTH
//        val scroll = JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        panel.add(list.scrolled())


        panel.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        contentPane.layout = BorderLayout()
        contentPane.add(panel, BorderLayout.CENTER)

        size = Dimension(650, 500)
    }

    fun clearAcupunct() {
        bigText.text = "Bitte einen Akupunkturpunkt ausw\u00e4hlen."
    }

    fun changeAcupunct(punct: Acupunct) {
        log.debug("changeAcupunct(newAcupunct={})", punct)
        val meridian = punct.meridian
        val element = meridian.element

        bigText.text = """
        <h1>${punct.titleShort} - ${punct.germanName} - <i>${punct.chineseName}</i></h1>
        <p><b>Element</b>: <span color="#${element.color.toHexString()}">${element.label}</span></p>
        ${flattenFlags(punct.flags)}
        <p><b>Extremit${"\u00e4"}t</b>: ${meridian.extremity.label}</p>
        <br/>
        <p><b>Lokalisierung</b>: ${punct.localisation}</p>
        <p><b>Indikationen</b>: ${punct.indications.joinToString()}</p>
        <br/>
        ${punct.note}
        """
    }

    private fun flattenFlags(flags: List<AcupunctFlag>): String {
        if (flags.isEmpty()) {
            return ""
        }
        return "<p><b>Auszeichnungen</b>: " + flags.map { it.onFlagType(AcupunctFlagCallback.LABELIZE) }.joinToString() + "</p>"
    }

    // MINOR @REFACTOR UI - make reusable in MyFrame for others (controller should take over some more functionality)
    fun start() {
        isVisible = true
    }

    override fun closeWindow() {
        isVisible = false
    }

    fun destroy() {
        closeWindow()
        dispose()
    }

}


private class ElementPanel(element: Element) : JPanel() {
    init {
        enforceSize(15, 15)
        opaque()
        background = element.color
    }
}

class AcupunctCell(punct: Acupunct) : DefaultCellView<Acupunct>(punct) {

    private val txtTitle = JLabel(punct.titleShort + (if (punct.isMarinaportant) "!!!" else "")).bold()

    private val txtFlags = JLabel(flagsToString(punct.flags))

    override val applicableForegrounds: Array<JComponent> = arrayOf(txtTitle, txtFlags)

    init {
        c.anchor = GridBagConstraints.NORTHWEST
        c.weightx = 1.0
        c.fill = GridBagConstraints.HORIZONTAL
        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            transparent()
            add(txtTitle)
            punct.elementFlag?.let { flag -> add(ElementPanel(flag.element)) }
        })

        c.gridy++
        add(txtFlags)
    }

    private fun flagsToString(flags: List<AcupunctFlag>): String {
        return flags
                .filter { it.renderShortLabel }
                .map { it.onFlagType(AcupunctFlagCallback.LABELIZE_SHORT) }
                .joinToString()
    }

}

class AcupunctureList @Inject constructor(
        bus: EventBus
) : MyList<Acupunct>(
        ViewNames.Acupunct.List,
        MyListModel<Acupunct>(),
        bus,
        object : MyListCellRenderer<Acupunct>() {
            override fun newCell(value: Acupunct) = AcupunctCell(value)
        }
), SearchableList<Acupunct> {
    init {
        //        initSinglePopup("L\u00f6schen", { DeleteTreatmentEvent(it) })
        //        initDoubleClicked { OpenTreatmentEvent(it) }
    }

}
