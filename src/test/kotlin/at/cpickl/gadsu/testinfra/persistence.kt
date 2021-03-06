package at.cpickl.gadsu.testinfra

import at.cpickl.gadsu.appointment.AppointmentJdbcRepository
import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.client.ClientJdbcRepository
import at.cpickl.gadsu.client.CurrentClient
import at.cpickl.gadsu.client.xprops.XPropsSqlJdbcRepository
import at.cpickl.gadsu.persistence.FlywayDatabaseManager
import at.cpickl.gadsu.persistence.PersistenceErrorCode
import at.cpickl.gadsu.persistence.PersistenceException
import at.cpickl.gadsu.persistence.SpringJdbcx
import at.cpickl.gadsu.preferences.JdbcPrefs
import at.cpickl.gadsu.report.multiprotocol.MultiProtocolJdbcRepository
import at.cpickl.gadsu.service.Clock
import at.cpickl.gadsu.service.IdGenerator
import at.cpickl.gadsu.treatment.Treatment
import at.cpickl.gadsu.treatment.TreatmentJdbcRepository
import at.cpickl.gadsu.treatment.TreatmentMeridiansJdbcRepository
import at.cpickl.gadsu.treatment.dyn.treats.BloodPressureJdbcRepository
import at.cpickl.gadsu.treatment.dyn.treats.HaraDiagnosisJdbcRepository
import at.cpickl.gadsu.treatment.dyn.treats.PulseDiagnosisJdbcRepository
import at.cpickl.gadsu.treatment.dyn.treats.TongueDiagnosisJdbcRepository
import com.google.common.eventbus.EventBus
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.hsqldb.jdbc.JDBCDataSource
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod

fun buildDatabaseUrl(dbNameSuffix: String) =
        "jdbc:hsqldb:mem:testDb$dbNameSuffix"

fun newTestDataSource(javaClazz: Class<Any>) = newTestDataSource(javaClazz.simpleName)

fun newTestDataSource(dbNameSuffix: String): JDBCDataSource {
    val ds = JDBCDataSource()
    ds.url = buildDatabaseUrl(dbNameSuffix)
    ds.user = "SA"
    return ds
}

fun setupTestDatabase(javaClazz: Class<Any>): Pair<JDBCDataSource, SpringJdbcx> {
    val ds = newTestDataSource(javaClazz)

    val jdbcx = SpringJdbcx(ds)

    FlywayDatabaseManager(ds).migrateDatabase()

    return Pair(ds, jdbcx)
}

abstract class HsqldbTest {
    companion object {
        init {
            TestLogger().configureLog()
        }
    }

    protected val TABLE_CLIENT = ClientJdbcRepository.TABLE
    protected val TABLE_TREATMENT = TreatmentJdbcRepository.TABLE
    protected val TABLE_TREATMENT_MERIDIAN = TreatmentMeridiansJdbcRepository.TABLE
    protected val TABLE_APPOINTMENT = AppointmentJdbcRepository.TABLE
    protected val TABLE_XPROPS = XPropsSqlJdbcRepository.TABLE
    protected val TABLE_MULTIPROTOCOL = MultiProtocolJdbcRepository.TABLE
    protected val TABLE_MULTIPROTOCOL_KEYS = MultiProtocolJdbcRepository.TABLE_KEYS
    protected val TABLE_PREFERENCES = JdbcPrefs.TABLE
    protected val TABLE_DYNTREATMENT_HARA = HaraDiagnosisJdbcRepository.TABLE
    protected val TABLE_DYNTREATMENT_HARA_KYO = HaraDiagnosisJdbcRepository.TABLE_KYO
    protected val TABLE_DYNTREATMENT_HARA_JITSU = HaraDiagnosisJdbcRepository.TABLE_JITSU
    protected val TABLE_DYNTREATMENT_TONGUE = TongueDiagnosisJdbcRepository.TABLE
    protected val TABLE_DYNTREATMENT_TONGUE_PROPERTIES = TongueDiagnosisJdbcRepository.TABLE_PROPERTIES
    protected val TABLE_DYNTREATMENT_BLOOD = BloodPressureJdbcRepository.TABLE
    protected val TABLE_DYNTREATMENT_PULSE = PulseDiagnosisJdbcRepository.TABLE
    protected val TABLE_DYNTREATMENT_PULSE_PROPERTIES = PulseDiagnosisJdbcRepository.TABLE_PROPERTIES

    private val log = LoggerFactory.getLogger(javaClass)
    private val allTables = arrayOf(
            TABLE_MULTIPROTOCOL_KEYS,
            TABLE_MULTIPROTOCOL,
            TABLE_XPROPS,
            TABLE_APPOINTMENT,
            TABLE_DYNTREATMENT_HARA_KYO,
            TABLE_DYNTREATMENT_HARA_JITSU,
            TABLE_DYNTREATMENT_HARA,
            TABLE_DYNTREATMENT_TONGUE,
            TABLE_DYNTREATMENT_TONGUE_PROPERTIES,
            TABLE_DYNTREATMENT_BLOOD,
            TABLE_DYNTREATMENT_PULSE,
            TABLE_DYNTREATMENT_PULSE_PROPERTIES,
            TABLE_TREATMENT_MERIDIAN,
            TABLE_TREATMENT,
            TABLE_CLIENT,
            TABLE_PREFERENCES
    )

    private var dataSource: JDBCDataSource? = null
    protected lateinit var jdbcx: SpringJdbcx

    protected lateinit var bus: EventBus
    protected lateinit var busListener: TestBusListener
    protected lateinit var clock: Clock
    protected lateinit var idGenerator: IdGenerator
    protected lateinit var currentClient: CurrentClient


    @BeforeClass
    fun initDb() {
        val (dataSource, jdbcx) = setupTestDatabase(javaClass)
        this.dataSource = dataSource
        this.jdbcx = jdbcx
        log.info("Using data source URL: ${dataSource.url}")
    }

    @BeforeMethod
    fun resetState() {
        bus = EventBus()
        busListener = TestBusListener()
        bus.register(busListener)
        clock = SimpleTestableClock()
        idGenerator = SequencedTestableIdGenerator()
        currentClient = CurrentClient(bus)

        allTables.forEach { jdbcx.deleteTable(it) }
    }

    @AfterClass
    fun shutdownDb() {
        // could have happened, that @BeforeClass failed, in this case shutting down will fail as a consequence. avoid this!
        dataSource?.connection?.close()
    }

    protected fun insertClientViaRepo(prototype: Client = Client.unsavedValidInstance()): Client {
        return ClientJdbcRepository(jdbcx, idGenerator).insertWithoutPicture(prototype)
    }

    protected fun deleteClientViaRepo(client: Client) {
        ClientJdbcRepository(jdbcx, idGenerator).delete(client)
    }

    protected fun insertTreatment(prototype: Treatment, id: String = TEST_UUID1): Treatment {
        return TreatmentJdbcRepository(jdbcx, SimpleTestableIdGenerator(id)).insert(prototype)
    }

    protected fun <T> assertRows(table: String, mapper: RowMapper<T>, vararg expected: T) {
        val rawRows = jdbcx.query("SELECT * FROM $table", mapper)
        if (expected.isEmpty()) {
            assertThat(rawRows, Matchers.emptyIterable())
        } else {
            assertThat(rawRows, Matchers.hasSize(expected.size))
            assertThat(rawRows, Matchers.contains(*expected))
        }
    }

}

fun SpringJdbcx.assertEmptyTable(tableName: String) {
    assertThat("Expected table '$tableName' to be empty.", countTableEntries(tableName), equalTo(0))
}

fun SpringJdbcx.countTableEntries(tableName: String): Int {
    var count: Int? = null
    jdbc.query("SELECT COUNT(*) AS cnt FROM $tableName") { rs -> count = rs.getInt("cnt") }
    return count!!
}


fun Expects.expectPersistenceException(errorCode: PersistenceErrorCode, executeAction: () -> Unit) {
    expect(
            type = PersistenceException::class,
            action = executeAction,
            exceptionAsserter = { e -> e.errorCode == errorCode }
    )
}
