package at.cpickl.gadsu.report

import at.cpickl.gadsu.service.formatDate
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Test(groups = arrayOf("integration", "pdf"))
class ProtocolGeneratorTest {

    private val report = ProtocolReportData.DUMMY
    private lateinit var testee: ProtocolGenerator

    @BeforeMethod
    fun initTest() {
        testee = JasperProtocolGenerator(JasperEngineImpl())
    }

    fun `save dummy to PDF file and assert content`() {
        val bytes = testee.generateByteStream(report)
//        testee.savePdfTo(report, target)
        val asserter = PdfAsserter.byStream(bytes)

        asserter.containsPdfStrings(
            "Shiatsu Protokoll",
            "${report.author}",
            "${report.printDate.formatDate()}",
            "Beruf: ${report.client.job}",
            "Kinder: ${report.client.children}"
            // assert report.client.cprops
            // assert report.client.picture ... hard one
        )
        asserter.containsPdfStrings(
            report.rows.map {
                listOf(
                    "Behandlung ${it.number}",
                    "Datum: ${it.dateFormatted}"
                    // "${it.note}" ... nah, got some nasty line breaks, and i dont wannaaaa! :-p
                )
            }.flatten()
        )
    }

//    private fun pdfAsserter() = PdfAsserter(target) // lazy init, as of existing target pdf file

}

class PdfAsserter(private val bytes: ByteArray) {
    companion object {
        fun byStream(stream: ByteArrayOutputStream) = PdfAsserter(stream.toByteArray())
        // byFile() = IOUtils.toByteArray(FileInputStream(pdfFile))
    }
    private val content: String = extractPdfText(bytes)

    private fun extractPdfText(bytes: ByteArray): String {
        val pdfDocument = PDDocument.load(ByteArrayInputStream(bytes));
        try {
            return PDFTextStripper().getText(pdfDocument);
        } finally {
            pdfDocument.close()
        }
    }

    fun containsPdfStrings(substrings: List<String>) {
//        assertThat(content, allOf(substrings.map { Matchers.containsString(it) }))
        substrings.forEach { assertThat(content, Matchers.containsString(it)) }
    }
    fun containsPdfStrings(vararg substrings: String) {
         containsPdfStrings(substrings.toList())
    }

}