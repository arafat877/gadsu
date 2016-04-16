package at.cpickl.gadsu.client

import com.google.common.collect.ComparisonChain
import org.joda.time.DateTime

data class Client(
        val id: String?, // it is null if not yet persisted
        val firstName: String,
        val lastName: String,
        val created: DateTime
) : Comparable<Client> {
    companion object {
        // needed for static extension methods

        val INSERT_PROTOTYPE = Client(null, "", "", DateTime.now()) // created will be overridden anyway, so its ok to use no Clock here ;)
    }

    val yetPersisted: Boolean
        get() = id != null

    val fullName: String
        get() {
            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                return firstName + " " + lastName
            }
            return firstName + lastName
        }

    override fun compareTo(other: Client): Int {
        return ComparisonChain.start()
                .compare(this.lastName, other.lastName)
                .compare(this.firstName, other.firstName)
                .result()
    }

    val idComparator: (Client) -> Boolean
        get() = { that -> this.id.equals(that.id) }

}