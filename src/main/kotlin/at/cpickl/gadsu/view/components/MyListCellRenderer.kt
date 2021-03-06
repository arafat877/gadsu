package at.cpickl.gadsu.view.components

import at.cpickl.gadsu.view.Colors
import at.cpickl.gadsu.view.components.panels.GridPanel
import at.cpickl.gadsu.view.swing.CurrentHoverIndexHolder
import at.cpickl.gadsu.view.swing.opaque
import at.cpickl.gadsu.view.swing.transparent
import java.awt.Color
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListCellRenderer


interface CellView {
    fun changeForeground(foreground: Color)
    fun changeBackground(background: Color)
    fun changeToTransparent()
    fun asComponent(): JComponent
}

abstract class DefaultCellView<T>(protected val value: T): GridPanel(), CellView {

    protected abstract val applicableForegrounds: Array<JComponent>

    init {
        opaque()
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }

    override final fun changeForeground(foreground: Color) {
        applicableForegrounds.forEach { it.foreground = foreground }
        onChangeForeground(foreground)
    }

    protected open fun onChangeForeground(foreground: Color) {
        // can be overridden
    }

    override final fun changeBackground(background: Color) {
        this.background = background
    }

    override final fun changeToTransparent() {
        asComponent().transparent()
    }

    override final fun asComponent() = this
}


abstract class MyListCellRenderer<T>(
        private val shouldHoverChangeSelectedBg: Boolean = false
) : ListCellRenderer<T>, CurrentHoverIndexHolder {

    override var currentHoverIndex: Int = -1

    protected abstract fun newCell(value: T): CellView

    override fun getListCellRendererComponent(list: JList<out T>, value: T, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        // MINOR @UI - improve performance (memory) by reusing existing cells and simply change its state
        val isHovered = index == currentHoverIndex
        val cell = newCell(value)

        if (isSelected) {
            cell.changeForeground(Colors.SELECTED_FG)
            if (isHovered && shouldHoverChangeSelectedBg) {
                cell.changeBackground(Colors.SELECTED_AND_HOVERED_BG)
            } else {
                cell.changeBackground(Colors.SELECTED_BG)
            }
        } else if (isHovered) {
            cell.changeBackground(Colors.BG_COLOR_HOVER)
        } else if (index % 2 == 1) {
            cell.changeBackground(Colors.BG_ALTERNATE)
        } else {
            cell.changeToTransparent()
        }

        return cell.asComponent()
    }
}
