package gadsu.persistence

import at.cpickl.gadsu.service.LOG
import com.google.common.annotations.VisibleForTesting
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import java.sql.Connection
import java.util.LinkedList


// https://flywaydb.org/documentation/migration/java
class V6_1__xprop_update : JdbcMigration {

    companion object {
        @VisibleForTesting val hungryDigestParts = listOf(
                "DigestionSlow", "DigestionFast", "Blockage", "Diarrhea", "UndigestedParts",
                "StoolHard", "StoolSoft", "WindBelly", "Farts")
        private val hungryDigest_prefixHungry = hungryDigestParts.map { "Hungry_$it" }
    }

    private val log = LOG(javaClass)

    override fun migrate(connection: Connection) {
        log.info("migrate")

        migrateTastesIntoExistingHungryAndDelete(connection)
        migrateHungryPartlyIntoNewDigestion(connection)
    }

    private fun migrateTastesIntoExistingHungryAndDelete(connection: Connection) {
        val xprops = selectXProps(connection)
        xprops.filter { it.key == "Taste" }.forEach { xprop ->

            val tasteVals = xprop.values.map { it.replace("Taste_", "Hungry_Taste") }

            val hungry = xprops.firstOrNull { it.idClient == xprop.idClient && it.key == "Hungry" }
            if (hungry == null) {
                connection.prepareStatement("INSERT INTO xprops (id_client, key, val) VALUES ('${xprop.idClient}', 'Hungry', '${tasteVals.joinToString(",")}')").apply { executeUpdate();close() }
            } else {
                val mergedVals = hungry.values.plus(tasteVals)
                connection.prepareStatement("UPDATE xprops SET val = '${mergedVals.joinToString(",")}' WHERE id_client = '${xprop.idClient}' AND key =  'Hungry'").apply { executeUpdate();close() }
            }
        }
        connection.prepareStatement("DELETE FROM xprops WHERE key = 'Taste'").apply { executeUpdate(); close() }
    }

    private fun migrateHungryPartlyIntoNewDigestion(connection: Connection) {
        val xprops = selectXProps(connection)
        xprops.filter { it.key == "Hungry" }.forEach { xprop ->
            val intersect = xprop.values.intersect(hungryDigest_prefixHungry)
            if (intersect.isNotEmpty()) {

                val digestionValues = intersect.map { it.replace("Hungry_", "Digestion_") }
                connection.prepareStatement("INSERT INTO xprops (id_client, key, val) VALUES " +
                        "('${xprop.idClient}', 'Digestion', '${digestionValues.joinToString(",")}')").apply { executeUpdate();close() }

                val hungryCleaned = xprop.values.minus(hungryDigest_prefixHungry)
                connection.prepareStatement("UPDATE xprops SET val = '${hungryCleaned.joinToString(",")}' WHERE id_client = '${xprop.idClient}' AND key =  'Hungry'").apply { executeUpdate();close() }
            }
        }
    }

    private fun selectXProps(connection: Connection): LinkedList<XPropDbo> {
        val statement = connection.prepareStatement("SELECT * FROM xprops")
        val result = LinkedList<XPropDbo>()
        try {
            val rs = statement.executeQuery()
            while (rs.next()) {
                // we already got a note here, but it will be always null, as no valid V6 entry was created yet
                result.add(XPropDbo(rs.getString("id_client"), rs.getString("key"), rs.getString("val").split(",")))
            }
        } finally {
            statement.close()
        }
        return result
    }

}

private data class XPropDbo(val idClient: String, val key: String, val values: List<String>)
