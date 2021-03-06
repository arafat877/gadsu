package non_test._main_.view

import at.cpickl.gadsu.tcm.ElementsStarView
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import non_test.Framed
import java.awt.Dimension


fun main(args: Array<String>) {
    val bus = EventBus()
    bus.register(object: Any() {
        @Subscribe fun onAny(event: Any) {
            println("====> Event dispatched: $event")
        }
    })
    Framed.show(ElementsStarView(bus), Dimension(450, 420))
}
