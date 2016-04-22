package at.cpickl.gadsu.view.components

import at.cpickl.gadsu.service.Clock
import at.cpickl.gadsu.service.RealClock
import com.google.common.eventbus.EventBus
import java.awt.Component
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.UIManager
import javax.swing.WindowConstants

/**
 * For internal use, when starting up part of the UI in its own main method.
 */
class Framed {
    companion object {
        init {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            JFrame.setDefaultLookAndFeelDecorated(true)
        }

        fun showWithContext(function: ((context: FramedContext) -> Component), size: Dimension? = null) {
            val _context = FramedContext()
            val component = function.invoke(_context)
            Framed()._show(arrayOf(component), size)
        }


        fun show(component: Component, size: Dimension? = null) {
            Framed()._show(arrayOf(component), size)
        }

        fun show(components: Array<Component>, size: Dimension? = null) {
            Framed()._show(components, size)
        }
    }

    private fun _show(components: Array<Component>, size: Dimension? = null) {
        val frame = JFrame()

        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.contentPane.layout = BoxLayout(frame.contentPane, BoxLayout.X_AXIS)

        components.forEach { frame.contentPane.add(it) }

        if (size != null) {
            frame.size = size
        } else {
            frame.pack()
        }
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}
class FramedContext(
        val bus: EventBus = EventBus(),
        val clock: Clock = RealClock(),
        val swing: SwingFactory = SwingFactory(bus, clock)
)