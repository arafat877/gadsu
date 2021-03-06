package at.cpickl.gadsu.service

import at.cpickl.gadsu.global.GADSU_DIRECTORY
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import java.io.File
import java.io.Reader
import java.io.StringReader


interface GoogleConnector {
    fun connectCalendar(credentials: GapiCredentials): Calendar
    fun connectGmail(credentials: GapiCredentials): Gmail
}

data class GapiCredentials(
        val clientId: String,
        val clientSecret: String
) {
    companion object {
        fun buildNullSafe(id: String? , secret: String?) =
                if (id == null || secret == null) null else GapiCredentials(id, secret)
    }
    val isNotEmpty: Boolean get() = clientId.isNotEmpty() && clientSecret.isNotEmpty()
}

// https://developers.google.com/gmail/api/quickstart/java
// https://developers.google.com/gmail/api/guides/sending
class GoogleConnectorImpl : GoogleConnector {
    companion object {
        private val APPLICATION_NAME: String = "gadsu"
        private val SCOPES = GmailScopes.all().union(CalendarScopes.all())
        private val DATASTORE_DIR = File(GADSU_DIRECTORY, "gapi_datastore")
    }

    private val log = LOG(javaClass)

    private val jsonFactory = JacksonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    override fun connectCalendar(credentials: GapiCredentials) =
            Calendar.Builder(httpTransport, jsonFactory, authorize(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build()!!

    override fun connectGmail(credentials: GapiCredentials) =
            Gmail.Builder(httpTransport, jsonFactory, authorize(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build()!!

    private fun authorize(credentials: GapiCredentials): Credential {
        val credentialsReader: Reader = StringReader(buildClientSecretJson(credentials))
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, credentialsReader)

        val flow = GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, SCOPES)
                .setDataStoreFactory(FileDataStoreFactory(DATASTORE_DIR))
                .setAccessType("offline")
                .build()

        val credential = AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize("user")
        log.debug("Credentials saved to: {}", DATASTORE_DIR.absolutePath);
        return credential!!
    }

    private fun buildClientSecretJson(credentials: GapiCredentials) = """
{
  "installed": {
    "client_id": "${credentials.clientId}",
    "client_secret": "${credentials.clientSecret}",
    "project_id": "lithe-bazaar-130716",
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://accounts.google.com/o/oauth2/token",
    "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
    "redirect_uris": [
      "urn:ietf:wg:oauth:2.0:oob",
      "http://localhost"
    ]
  }
}
"""
}

/*

// https://developers.google.com/gmail/api/quickstart/java
// https://developers.google.com/gmail/api/guides/sending

class GoogleConnectorImpl : GoogleConnector {
    companion object {
        private val APPLICATION_NAME: String = "gadsu"
//        private val DATASTORE: File = File(GADSU_DIRECTORY, "google_datastore.json")
        private val DATASTORE_GCAL: File = File(GADSU_DIRECTORY, "google_datastore_calendar")
        private val DATASTORE_GMAIL: File = File(GADSU_DIRECTORY, "google_datastore_gmail")

//        private val SCOPES = mutableListOf<String>().apply {
//            addAll(CalendarScopes.all())
//            add(GmailScopes.GMAIL_SEND)
//        }.toList()
        //CalendarScopes.all().union(listOf(GmailScopes.GMAIL_SEND))
        private val SCOPES_CALENDAR = CalendarScopes.all().toList()
        private val SCOPES_GMAIL = listOf(GmailScopes.GMAIL_SEND)

        private fun credentialsReader() = InputStreamReader(GoogleConnectorImpl::class.java.getResourceAsStream("/gadsu/google_client_secret.json"))
    }

    private val log = LOG(javaClass)

    private val jsonFactory = JacksonFactory.getDefaultInstance()

    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    override fun connectCalendar(): Calendar {
        log.debug("Connecting to GMail Calendar.")
        return Calendar.Builder(httpTransport, jsonFactory, authorize(SCOPES_CALENDAR, DATASTORE_GCAL))
                .setApplicationName(APPLICATION_NAME)
                .build()
    }

    override fun connectGmail(): Gmail {
        return Gmail.Builder(httpTransport, jsonFactory, authorize(SCOPES_GMAIL, DATASTORE_GMAIL))
                .setApplicationName(APPLICATION_NAME)
                .build()!!
    }

    private fun authorize(scopes: List<String>, datastore: File): Credential {
        log.info("authorize()")
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, credentialsReader())
        val authFlow = GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, clientSecrets, scopes)
                .setDataStoreFactory(FileDataStoreFactory(datastore))
                .setAccessType("offline")
                .build()
        val credential = AuthorizationCodeInstalledApp(authFlow, LocalServerReceiver()).authorize("user")
        log.debug("Credentials saved to: ${datastore.absolutePath}")
        return credential
    }

}
*/
