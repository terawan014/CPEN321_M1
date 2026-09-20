package com.example.cpen321application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Column
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.Inet4Address
import java.net.NetworkInterface
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.compose.material3.Button
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        apiBaseUrl = BuildConfig.API_BASE_URL,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(apiBaseUrl: String, modifier: Modifier = Modifier) {
    var loginStatusText by remember { mutableStateOf("Not signed in") }
    var isLoggedIn by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember {
        CredentialManager.create(context)
    }
    val signInWithGoogleOption =
        GetSignInWithGoogleOption.Builder(
            serverClientId = BuildConfig.GOOGLE_CLIENT_ID
        ).build()

    val credentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()
    var nameText by remember { mutableStateOf("Checking backend at $apiBaseUrl/name") }
    var serverTimeText by remember { mutableStateOf("Checking backend at $apiBaseUrl/server_time...") }
    var serverIpText by remember { mutableStateOf("Checking backend at $apiBaseUrl/server_ip...") }
    var clientTimeText by remember { mutableStateOf("Loading client time...") }
    var clientIpText by remember { mutableStateOf("Loading client IP...") }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val nameJson = fetchApi(apiBaseUrl, "/name")
            nameText = parseName(nameJson)

            val serverTimeJson = fetchApi(apiBaseUrl, "/server_time")
            serverTimeText = parseServerTime(serverTimeJson)

            val serverIpJson = fetchApi(apiBaseUrl, "/server_ip")
            serverIpText = parseServerIp(serverIpJson)

            clientTimeText = "Client Time: ${getClientTime()}"
            clientIpText = "Client IP: ${getClientIp()}"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        if (!isLoggedIn) {
            Button(
                onClick = {
                    loginStatusText = "Opening Google sign-in..."

                    coroutineScope.launch {
                        try {
                            val result = credentialManager.getCredential(
                                context = context,
                                request = credentialRequest
                            )

                            val credential = result.credential

                            if (
                                credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleCredential =
                                    GoogleIdTokenCredential.createFrom(credential.data)

                                val firstName = googleCredential.givenName ?: ""
                                val lastName = googleCredential.familyName ?: ""

                                loginStatusText =
                                    "Google User Name: ${"$firstName $lastName".trim()}"

                                isLoggedIn = true
                            }

                        } catch (e: GetCredentialException) {
                            loginStatusText =
                                "Google sign-in failed: ${e.javaClass.simpleName}: ${e.message}"

                        } catch (e: Exception) {
                            loginStatusText =
                                "Unexpected error: ${e.javaClass.simpleName}: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Sign in with Google")

            }

            Text(text = loginStatusText,
                modifier = Modifier.align(Alignment.CenterHorizontally))


        } else {
            Spacer(modifier = Modifier.height(168.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = loginStatusText)
                Text(text = nameText)
                Text(text = serverTimeText)
                Text(text = serverIpText)
                Text(text = clientTimeText)
                Text(text = clientIpText)
            }
        }
    }
}

private suspend fun fetchApi(apiBaseUrl: String, endpoint: String): String = withContext(Dispatchers.IO) {
    val apiUrl = "${apiBaseUrl.trimEnd('/')}$endpoint"
    try {
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                "$body"
            }
            else -> {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "Backend error ($apiUrl): HTTP $code${errorBody?.let { " — $it" } ?: ""}"
            }
        }
    } catch (e: Exception) {
        "Backend unreachable ($apiUrl): ${e.message ?: e.javaClass.simpleName}"
    }
}
// parse the json to clear text
private fun parseName(json: String): String {
    val jsonObject = JSONObject(json)
    val firstName = jsonObject.getString("firstName")
    val lastName = jsonObject.getString("lastName")

    return "Student Name: $firstName $lastName"
}

private fun parseServerTime(json: String): String {
    val jsonObject = JSONObject(json)
    val serverTime = jsonObject.getString("serverTime")

    return "Server Time: $serverTime"
}

private fun parseServerIp(json: String): String {
    val jsonObject = JSONObject(json)
    val serverIp = jsonObject.getString("serverIP")

    return "Server IP: $serverIp"
}

// get client time (no need via backend)
private fun getClientTime(): String {
    val currentTime = Date()

    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormatter.format(currentTime)
    val timeZone = java.util.TimeZone.getDefault()
    val offsetMillis = timeZone.getOffset(currentTime.time)
    val offsetMinutes = offsetMillis / (60 * 1000)
    val offsetSign = if (offsetMinutes < 0) "-" else "+"
    val offsetHours = kotlin.math.abs(offsetMinutes) / 60
    val offsetMinutesRemainder = kotlin.math.abs(offsetMinutes) % 60
    val offsetHoursString = offsetHours.toString().padStart(2, '0')
    val offsetMinutesString = offsetMinutesRemainder.toString().padStart(2, '0')
    val offsetString = "$offsetSign$offsetHoursString:$offsetMinutesString"
    return "$formattedTime GMT$offsetString"
}

private fun getClientIp(): String {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (networkInterface in interfaces) {
        val addresses = networkInterface.inetAddresses
        for (address in addresses) {
            if (!address.isLoopbackAddress && address is Inet4Address) {
                return address.hostAddress ?: "Unknown"
            }
        }
    }
    return "Unknown"
}