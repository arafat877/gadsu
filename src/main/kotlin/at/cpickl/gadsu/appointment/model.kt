package at.cpickl.gadsu.appointment

import at.cpickl.gadsu.persistence.Persistable
import at.cpickl.gadsu.service.HasId
import com.google.common.collect.ComparisonChain
import org.joda.time.DateTime

data class Appointment(
        override val id: String?,
        val clientId: String,
        val created: DateTime,
        val start: DateTime,
        val end: DateTime,
        val note: String,
        val gcalId: String?,
        val gcalUrl: String?
) : Comparable<Appointment>, HasId, Persistable {

    companion object {
        fun insertPrototype(clientId: String, start: DateTime): Appointment {
            return Appointment(null, clientId, DateTime.now(), start, start.plusMinutes(90), "", null, null)
        }
    }

    val idComparator: (Appointment) -> Boolean
        get() = { that -> this.id.equals(that.id) }

    override fun compareTo(other: Appointment): Int {
        return ComparisonChain.start()
                .compare(this.start, other.start)
                .compare(this.end, other.end)
                .compare(this.clientId, other.clientId)
                .compare(this.id, other.id)
                .result()
    }


    override val yetPersisted: Boolean get() = id != null

}
