package at.cpickl.gadsu.view.logic

import at.cpickl.gadsu.global.GadsuException
import at.cpickl.gadsu.view.components.MyTable
import at.cpickl.gadsu.view.swing.elementAtPoint
import at.cpickl.gadsu.view.swing.log
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JList


fun <E> JList<E>.registerDoubleClicked(listener: (Int, E) -> Unit) {
    val list = this
    addMouseListener(object : DoubleClickMouseListener() {
        override fun mouseDoubleClicked(event: MouseEvent) {
            val (row, entity) = elementAtPoint(event.point) ?: throw GadsuException("Impossible view state! Could not find element at point: ${event.point} (this: $list)")
            log.debug("List row at index {} double clicked: {}", row, entity)
            listener.invoke(row, entity)
        }
    })
}

fun <E> MyTable<E>.registerDoubleClicked(listener: (Int, E) -> Unit) {
    val table = this
    addMouseListener(object : DoubleClickMouseListener() {
        override fun mouseDoubleClicked(event: MouseEvent) {
            val (row, entity) = elementAtPoint(event.point) ?: throw GadsuException("Impossible view state! Could not find element at point: ${event.point} (this: $table)")
            log.debug("Table row at index {} double clicked: {}", row, entity)
            listener.invoke(row, entity)
        }
    })
}

abstract class DoubleClickMouseListener() : MouseAdapter() {

    private var previousWasRight = false

    abstract fun mouseDoubleClicked(event: MouseEvent)

    override fun mousePressed(event: MouseEvent) {
        if (!isProperDoubleClick(event)) return
        mouseDoubleClicked(event)
    }

    private fun isProperDoubleClick(event: MouseEvent): Boolean {
        val isRightButton = (event.modifiers and InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK
        val result = event.clickCount == 2 && !previousWasRight && !isRightButton
        previousWasRight = isRightButton
        return result
    }
}
