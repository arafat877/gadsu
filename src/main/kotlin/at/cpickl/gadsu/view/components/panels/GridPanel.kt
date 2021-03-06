package at.cpickl.gadsu.view.components.panels

import at.cpickl.gadsu.client.xprops.view.GridBagFill
import at.cpickl.gadsu.development.debugColor
import at.cpickl.gadsu.global.IS_OS_WIN
import at.cpickl.gadsu.view.swing.Pad
import at.cpickl.gadsu.view.swing.transparent
import java.awt.Color
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel



fun GridBagConstraints.fillAll() {
    weightx = 1.0
    weighty = 1.0
    fill = GridBagConstraints.BOTH
}

open class GridPanel(
        viewName: String? = null,
        _debugColor: Color? = null
) : JPanel() {

    val c = GridBagConstraints()

    init {
        transparent()
        if (viewName != null) name = viewName
        if (_debugColor != null) debugColor = _debugColor

        val gridBagLayout = GridBagLayout()
        layout = gridBagLayout
        gridBagLayout.setConstraints(this, c)

        c.gridx = 0
        c.gridy = 0
    }

    override fun add(comp: Component): Component {
        super.add(comp, c)
        return comp
    }


    fun addColumned(vararg columns: Pair<Double, JComponent>) {
        c.fill = GridBagConstraints.BOTH
        c.weighty = 1.0
        c.anchor = GridBagConstraints.NORTH
        var first = true
        columns.forEach {
            if (first) {
                first = false
            } else {
                c.insets = Pad.LEFT
            }

            c.weightx = it.first
            add(it.second)
            c.gridx++
        }
    }

    fun addLastRowFilled(fillType: GridBagFill = GridBagFill.Both) {
        c.fill = fillType.swingId
        c.weightx = if (fillType == GridBagFill.Both || fillType == GridBagFill.Horizontal) 1.0 else 0.0
        c.weighty = if (fillType == GridBagFill.Both || fillType == GridBagFill.Vertical) 1.0 else 0.0
        add(JPanel().transparent()) // some nice UI hack ;)
    }

}

class SingleButtonPanel(button: JButton) : GridPanel() {
    init {
        debugColor = Color.ORANGE
        c.weightx = 1.0
        c.fill = GridBagConstraints.HORIZONTAL
        if (IS_OS_WIN) c.insets = Insets(4, 0, 6, 0)
        add(button)
    }
}
