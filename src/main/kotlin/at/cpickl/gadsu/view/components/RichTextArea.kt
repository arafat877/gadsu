package at.cpickl.gadsu.view.components

import at.cpickl.gadsu.IS_OS_WIN
import at.cpickl.gadsu.acupuncture.AcupunctWordDetector
import at.cpickl.gadsu.isShortcutDown
import at.cpickl.gadsu.service.LOG
import at.cpickl.gadsu.view.Colors
import at.cpickl.gadsu.view.logic.MAX_FIELDLENGTH_LONG
import at.cpickl.gadsu.view.swing.enforceMaxCharacters
import at.cpickl.gadsu.view.swing.focusTraversalWithTabs
import at.cpickl.gadsu.view.swing.onTriedToInsertTooManyChars
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.HashMap
import java.util.LinkedList
import javax.swing.JLabel
import javax.swing.JTextPane
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext


data class ShortcutEvent(val format: RichFormat, val selectedText: String)
interface ShortcutListener {
    fun onShortcut(event: ShortcutEvent)
}

fun String.removeAllTags(): String {
    // this logic can be optimized a bit ;)
    var x = this
    RichFormat.values().forEach {
        x = x.replace(it.tag1, "").replace(it.tag2, "")
    }
    return x
}

enum class RichFormat(
        val label: String,
        val htmlTag: String,
        val shortcutKey: Char
) {

    Bold("bold", "b", 'b') {

        override fun addingAttribute(): AttributeSet {
            val sc = StyleContext.getDefaultStyleContext()
            var aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Background, Colors.TEXTEDITOR_BOLD)
            aset = sc.addAttribute(aset, StyleConstants.Bold, true)
            return aset
        }

        override fun removalAttribute(): AttributeSet {
            val sc = StyleContext.getDefaultStyleContext()
            var aset = sc.removeAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Background)
            aset = sc.addAttribute(aset, StyleConstants.Bold, false)
            return aset
        }

        override fun isStyle(attributes: AttributeSet): Boolean {
            val map = attributes.toMap()
            return map[StyleConstants.Bold]?.equals(true) ?: false
        }
    },

    Italic("italic", "i", 'i') {

        override fun addingAttribute(): AttributeSet {
            return StyleContext.getDefaultStyleContext()
                    .addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Italic, true)
        }

        override fun removalAttribute(): AttributeSet {
            return StyleContext.getDefaultStyleContext()
                    .addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Italic, false)
        }

        override fun isStyle(attributes: AttributeSet): Boolean {
            val map = attributes.toMap()
            return map[StyleConstants.Italic]?.equals(true) ?: false
        }
    }
    ;

    val tag1 = "<$htmlTag>"
    val tag2 = "</$htmlTag>"

    companion object {
        val CLEAN_FORMAT: AttributeSet

        init {
            val sc = StyleContext.getDefaultStyleContext()
            var aset = sc.removeAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Background)
            aset = sc.addAttribute(aset, StyleConstants.Bold, false)
            aset = sc.addAttribute(aset, StyleConstants.Italic, false)
            CLEAN_FORMAT = aset
        }
    }


    abstract fun isStyle(attributes: AttributeSet): Boolean
    abstract fun removalAttribute(): AttributeSet
    abstract fun addingAttribute(): AttributeSet

}

fun AttributeSet.toMap(): Map<Any, Any> {
    val map = HashMap<Any, Any>()
    attributeNames.iterator().forEach {
        map.put(it, getAttribute(it))
    }
    return map
}

open class RichTextArea(
        viewName: String,
        private val maxChars: Int = MAX_FIELDLENGTH_LONG
) : JTextPane() {

    companion object {
        private val FORMATS = RichFormat.values().associateBy { it.shortcutKey }
    }

    private val log = LOG(javaClass)

    init {
        name = viewName
        focusTraversalWithTabs()

        enforceMaxCharacters(maxChars)

        if (IS_OS_WIN) {
            font = JLabel().font
        }

        addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {

                if (e.isShortcutDown && FORMATS.containsKey(e.keyChar)) {
                    onToggleFormat(FORMATS[e.keyChar]!!)
                }
            }
        })
    }

    fun enableAcupunctDetection() {
        val words = WordDetector(this)

        words.addWordListener(AcupunctWordDetector().apply {
            addAcupunctListener {

            }
        })
    }

    fun RichFormat.clearTag(input: String) = input.replace(tag1, "").replace(tag2, "")

    fun readEnrichedText(enrichedText: String) {
        log.trace("readEnrichedText(enrichedText=[{}]) viewName={}", enrichedText, name)

        // dont forget to reset style before reading new!
        replaceTextStyle { adoc ->
            adoc.replace(0, text.length, text, RichFormat.CLEAN_FORMAT)
        }

        var cleanText = enrichedText
        RichFormat.values().forEach {
            cleanText = it.clearTag(cleanText)
        }
        text = cleanText

        var txt = enrichedText
        RichFormat.values().forEach {
            val tag1 = it.tag1
            val tag2 = it.tag2
            while (txt.contains(tag1)) {
                var pivotableTxt = txt
                RichFormat.values().forEach { j ->
                    pivotableTxt = if (j == it) pivotableTxt else j.clearTag(pivotableTxt)
                }

                val start = pivotableTxt.indexOf(tag1)
                val end = pivotableTxt.indexOf(tag2) - tag1.length
//                println("going to select: $start/$end")
                select(start, end)

                replaceTextStyle { adoc ->
                    adoc.replace(start, end - start, selectedText, it.addingAttribute())
                }

                txt = txt.replaceFirst(tag1, "").replaceFirst(tag2, "")
            }
        }

        caretPosition = text.length
    }

    fun toEnrichedText(): String {
        val result = StringBuilder()
        val n = text.length - 1
        for (i in 0..n) {
            val char = text[i]
            val charAttributes = styledDocument.getCharacterElement(i).attributes

            RichFormat.values().forEach {
                val isNowStyled = it.isStyle(charAttributes)
                val previousWasStyled = if (i == 0) false else {
                    val prevCharAttributes = styledDocument.getCharacterElement(i - 1).attributes
                    it.isStyle(prevCharAttributes)
                }
                if (!previousWasStyled && isNowStyled) {
                    result.append("<${it.htmlTag}>")
                }
            }

            result.append(char)

            RichFormat.values().forEach {
                val isNowStyled = it.isStyle(charAttributes)
                val nextIsStyled = if (i == (n)) false else {
                    val nextCharAttributes = styledDocument.getCharacterElement(i + 1).attributes
                    it.isStyle(nextCharAttributes)
                }
                if (isNowStyled && !nextIsStyled) {
                    result.append("</${it.htmlTag}>")
                }
            }
        }

        return result.toString()
    }


    private fun areAllCharsSameFormat(start: Int, end: Int, format: RichFormat): Boolean {
        for (i in start..end - 1) {
            val element = styledDocument.getCharacterElement(i)
//            element.attributes.dump()
            val isNotFormat = !format.isStyle(element.attributes)
            if (isNotFormat) {
                return false
            }
        }
        return true
    }


    private val listeners = LinkedList<ShortcutListener>()
    fun registerListener(listener: ShortcutListener) {
        listeners.add(listener)
    }

    private fun onToggleFormat(format: RichFormat, enableSimulation: Boolean = true) {
        if (selectedText == null || selectedText.isEmpty()) {
            log.trace("onToggleFormat() aborted because is empty")
            return
        }
        val allAreStyledByFormat = areAllCharsSameFormat(selectionStart, selectionEnd, format)
        log.trace("onToggleFormat() selectionStart=$selectionStart, selectionEnd=$selectionEnd; allAreStyledByFormat=$allAreStyledByFormat; selectedText=[$selectedText]")

        val aset: AttributeSet
        if (allAreStyledByFormat) {
            log.trace("remove format")
            aset = format.removalAttribute()
        } else {
            log.trace("add format")
            aset = format.addingAttribute()
        }

        val previousSelection = Pair(selectionStart, selectionEnd)

        if (enableSimulation && simulationSaysLengthIsTooLong(format)) {
            onTriedToInsertTooManyChars()
            return
        }
        replaceTextStyle { adoc ->
            adoc.replace(selectionStart, selectedText.length, selectedText, aset)
        }

        select(previousSelection.first, previousSelection.second)

        listeners.forEach { it.onShortcut(ShortcutEvent(format, selectedText)) }
//        val e = AbstractDocument.DefaultDocumentEvent(offs, str.length, DocumentEvent.EventType.CHANGE)
    }

    private fun simulationSaysLengthIsTooLong(format: RichFormat): Boolean {
        val simulation = RichTextArea("simulation", maxChars)
        simulation.readEnrichedText(this.toEnrichedText())
        simulation.select(this.selectionStart, this.selectionEnd)
        simulation.onToggleFormat(format, enableSimulation = false)

        return simulation.toEnrichedText().length > maxChars
    }

    private fun replaceTextStyle(fn: (AbstractDocument) -> Unit) {
        val adoc = styledDocument as AbstractDocument
        _isReformatting = true
        fn(adoc)
        _isReformatting = false
    }

    private var _isReformatting = false
    val isReformatting: Boolean get() = _isReformatting

    private fun AttributeSet.dump() {
        println("attributes: $this")
        attributeNames.iterator().forEach {
            val name = it
            val value = getAttribute(name)
            println("    name = [$name]; value = [$value]")
        }
    }
}
