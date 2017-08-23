package non_test._main_

import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.client.xprops.model.CProps
import at.cpickl.gadsu.tcm.model.XProps
import at.cpickl.gadsu.tcm.patho.SyndromeGuesser
import at.cpickl.gadsu.testinfra.savedValidInstance
import com.github.christophpickl.kpotpourri.common.collection.prettyPrint


fun main(args: Array<String>) {
    val client = Client.savedValidInstance().copy(
            cprops = CProps.builder()
                    .add(XProps.Hungry, XProps.HungryOpts.LittleHunger, XProps.HungryOpts.BigHunger)
                    .add(XProps.Temperature, XProps.TemperatureOpts.SweatEasily)
                    .build()
    )
    println()
    println("Client props: ${client.cprops.map { it.clientValue.map { it.label }.joinToString() }.joinToString()}")
    println()

    println("Guessed syndroms:")
    val report = SyndromeGuesser().guess(client, emptyList())
    report.possibleSyndromes.sortedDescending().map {
        String.format("[%3s%%] %s: %s (NOT: %s)", it.matchPercentage, it.syndrome.label,
                it.matchedSymptoms.map { it.source.label }.joinToString(),
                it.notMatchedSymptoms.map { it.source.label }.joinToString()
        )
    }.prettyPrint()
}