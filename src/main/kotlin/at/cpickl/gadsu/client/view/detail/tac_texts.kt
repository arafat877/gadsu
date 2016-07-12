package at.cpickl.gadsu.client.view.detail

import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.view.Fields
import at.cpickl.gadsu.view.ViewNames
import at.cpickl.gadsu.view.addFormInput
import at.cpickl.gadsu.view.components.panels.VFillFormPanel
import at.cpickl.gadsu.view.language.Labels
import at.cpickl.gadsu.view.logic.ModificationChecker
import java.awt.GridBagConstraints


class ClientTabTexts(
        modificationChecker: ModificationChecker
) : DefaultClientTab(
        title = Labels.Tabs.ClientTexts
//        scrolled = false
) {

    private val fields = Fields<Client>(modificationChecker)

    val inpImpression = fields.newTextArea("Allgemeiner Eindruck", {it.textImpression}, ViewNames.Client.InputTextImpression)
    val inpMedical = fields.newTextArea("Medizinisches", {it.textMedical}, ViewNames.Client.InputTextMedical)
    val inpComplaints = fields.newTextArea("Beschwerden", {it.textComplaints}, ViewNames.Client.InputTextComplaints)
    val inpPersonal = fields.newTextArea("Lebensprofil", {it.textPersonal}, ViewNames.Client.InputTextPersonal)
    val inpObjective = fields.newTextArea("Ziele", {it.textObjective}, ViewNames.Client.InputTextObjective)

    init {
        c.fill = GridBagConstraints.BOTH
        c.weightx = 1.0
        c.weighty = 1.0
        add(VFillFormPanel().apply {
            addFormInput(inpImpression)
            addFormInput(inpMedical)
            addFormInput(inpComplaints)
            addFormInput(inpPersonal)
            addFormInput(inpObjective)
        })
    }

    override fun isModified(client: Client): Boolean {
        return fields.isAnyModified(client)
    }

    override fun updateFields(client: Client) {
        fields.updateAll(client)
    }

}
