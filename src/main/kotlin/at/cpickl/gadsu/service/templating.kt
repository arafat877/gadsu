package at.cpickl.gadsu.service

import at.cpickl.gadsu.global.GadsuException
import freemarker.core.InvalidReferenceException
import freemarker.core.UnknownDateTypeFormattingUnsupportedException
import freemarker.template.Configuration
import freemarker.template.Template
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.util.*

interface TemplatingEngine {

    /**
     * @throws FreemarkerInvalidReferenceException if templateText contained variable not contained in data
     */
    fun process(templateText: String, data: Map<String, Any>): String

}

class FreemarkerTemplatingEngine : TemplatingEngine {
    private val log = LOG(javaClass)

    companion object {
        // http://freemarker.org/docs/pgui_quickstart_createconfiguration.html
        val configuration: Configuration by lazy {
            Configuration(Configuration.VERSION_2_3_25).apply {
                defaultEncoding = "UTF-8"
                logTemplateExceptions = false
                templateExceptionHandler = freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER
                locale = Locale.GERMAN
//                setSharedVariable("someGadsuVar", "foobar")
            }
        }
    }

    override fun process(templateText: String, data: Map<String, Any>): String {
        log.debug("process(..)")
        val reader: Reader = StringReader(templateText)
        val template = Template("myTemplate", reader, configuration)
        val writer = StringWriter()
        try {
            template.process(data, writer)
            return writer.toString()

        } catch(e: UnknownDateTypeFormattingUnsupportedException) {
            throw FreemarkerException("some date was fucked up. maybe tried to directly print out a date?!", template, data, e)

        } catch(e: InvalidReferenceException) {
            throw FreemarkerInvalidReferenceException("The template text contained a reference which was not found in the parameter map.", template, data, e)

        } catch(e: Exception) {
            throw FreemarkerException("Unknown reason!", template, data, e)
        }
    }

}

open class FreemarkerException(detailMessage: String, template: Template, data: Map<String, Any>, cause: Exception? = null)
    : GadsuException("" +
        "Freemarker template processing failed (mostly because of corrupt template).\n" +
        "$detailMessage\n" +
        "See the exception cause for details.\n" +
        "Template: $template\n" +
        "Data: $data", cause)

class FreemarkerInvalidReferenceException(detailMessage: String, template: Template, data: Map<String, Any>, cause: InvalidReferenceException? = null)
    : FreemarkerException(detailMessage, template, data, cause)

/*
termin am ${dateStart?string["d.M."]}
hallo <#if gender == "M">lieber <#elseif gender == "F">liebe </#if>${name?lower_case},
termin am ${dateStart?string["EEEE 'der' d. MMMMM"]?lower_case} von ${dateStart?string["HH:mm"]} bis ${dateEnd?string["HH:mm"]} uhr
 */
data class TemplateData<T>(
        val name: String,
        val description: String,
        val extractor: (T) -> Any
)

interface TemplateDeclaration<T> {
    val data: List<TemplateData<T>>

    fun process(t: T): Map<String, Any> =
            data.map { it.name to it.extractor(t) }.toMap()

    val toolTipText get() =
            "<html>" + data.joinToString("<br/>") { "<tt>${it.name}</tt> ... ${it.description}" } + "</html>"
}
