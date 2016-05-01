package at.cpickl.gadsu.client.xprops.view

import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.client.xprops.model.CPropEnum
import at.cpickl.gadsu.client.xprops.model.XPropEnum
import at.cpickl.gadsu.client.xprops.model.XPropEnumOpt
import at.cpickl.gadsu.view.ElField
import at.cpickl.gadsu.view.components.DefaultCellView
import at.cpickl.gadsu.view.components.ModificationChecker
import at.cpickl.gadsu.view.components.MyList
import at.cpickl.gadsu.view.components.MyListCellRenderer
import at.cpickl.gadsu.view.components.MyListModel
import at.cpickl.gadsu.view.components.scrolled
import com.google.common.eventbus.EventBus
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.ListSelectionModel

interface ElFieldForProps<V> : ElField<V> {
    fun enableFor(modifications: ModificationChecker)
}

class CPropEnumView(
        private val xprop: XPropEnum,
        bus: EventBus
): CPropView, ElFieldForProps<Client> {

    private val list: MyList<XPropEnumOpt>

    override val formLabel = xprop.label
    override val fillType = GridBagFill.Both

    init {
        val model = MyListModel<XPropEnumOpt>()
        model.resetData(xprop.options)
        list = MyList<XPropEnumOpt>("FIXME_VIEWNAME", model, bus, object: MyListCellRenderer<XPropEnumOpt>() {
            override fun newCell(value: XPropEnumOpt) = XPropEnumCell(value)
        })
        list.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        list.visibleRowCount = 4
    }

    override fun updateValue(client: Client) {
        list.clearSelection()
        val cprop = client.cprops.findOrNull(xprop) ?: return
        list.addSelectedValues((cprop as CPropEnum).clientValue)
    }

    override fun toCProp() = CPropEnum(xprop, list.selectedValuesList)

    override fun toComponent() = list.scrolled()

    override fun isModified(value: Client): Boolean {
        val selected = list.selectedValuesList
        val cprop = value.cprops.findOrNull(xprop) ?: return selected.isNotEmpty()

        if (selected.isEmpty()) {
            return false
        }

        val enumProp = cprop as CPropEnum
        return !enumProp.clientValue.containsAll(selected) ||
               !selected.containsAll(enumProp.clientValue)
    }

    override fun enableFor(modifications: ModificationChecker) {
        modifications.enableChangeListener(list)
    }
}

class XPropEnumCell(val xprop: XPropEnumOpt) : DefaultCellView<XPropEnumOpt>(xprop) {
    private val txtLabel = JLabel(xprop.label)
    override val applicableForegrounds: Array<JComponent> = arrayOf(txtLabel)

    init {
//        c.anchor = GridBagConstraints.NORTHWEST
//        c.weightx = 1.0
//        c.fill = GridBagConstraints.HORIZONTAL
//        add(txtTitle)
//
//        c.gridy++
        add(txtLabel)
    }
}
