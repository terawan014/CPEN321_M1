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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import android.util.Log
import okio.ByteString
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import coil.compose.AsyncImage
import androidx.activity.compose.BackHandler

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

    // button 2
    val pixels = remember {
        mutableStateListOf<String>().apply {
            repeat(256) {
                add("#FFFFFF")
            }
        }
    }
    var pixelWebSocket by remember { mutableStateOf<WebSocket?>(null) }
    var currentScreen by remember { mutableStateOf("home") }

    // button 3
    var minutesInput by remember { mutableStateOf("") }
    var secondsInput by remember { mutableStateOf("") }

    var remainingSeconds by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    var pokemonName by remember { mutableStateOf("") }
    var pokemonImageUrl by remember { mutableStateOf("") }
    var pokemonType by remember { mutableStateOf("") }
    var pokemonTotalStats by remember { mutableStateOf(0) }
    var showPokemon by remember { mutableStateOf(false) }
    var pokemonRarity by remember { mutableStateOf("") }
    val pokemonTeam = remember {
        mutableStateListOf<Pokemon>()
    }

    var teamMessage by remember {
        mutableStateOf("")
    }

    BackHandler(enabled = currentScreen != "home") {
        currentScreen = "home"
    }

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
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {

            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }

            // Timer finished, now get Pokémon FIRST
            val pokemon = fetchRandomPokemon()

            if (pokemon != null) {
                pokemonName = pokemon.name
                pokemonImageUrl = pokemon.imageUrl
                pokemonType = pokemon.type
                pokemonTotalStats = pokemon.totalStats
                showPokemon = true
                pokemonRarity = pokemon.rarity
            }
            isTimerRunning = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 44.dp)
    ) {

        if (currentScreen == "home") {

            // Button 1 = Google Login
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
                                credential.type ==
                                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleCredential =
                                    GoogleIdTokenCredential.createFrom(credential.data)

                                val firstName = googleCredential.givenName ?: ""
                                val lastName = googleCredential.familyName ?: ""

                                loginStatusText =
                                    "Google User Name: ${"$firstName $lastName".trim()}"

                                isLoggedIn = true
                                currentScreen = "button1"
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
                Text("Button 1: Sign in with Google")
            }

            Text(
                text = loginStatusText,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )



            // Button 2
            Spacer(modifier = Modifier.height(44.dp))
            Button(
                onClick = {
                    currentScreen = "button2"

                    if (pixelWebSocket == null) {
                        pixelWebSocket = connectToPixelWebSocket { pixel ->
                            val index = pixel.y * 16 + pixel.x

                            if (index in 0 until 256) {
                                pixels[index] = pixel.color
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Button 2: Live Updates")
            }
            // Button 3
            Spacer(modifier = Modifier.height(44.dp))
            Button(
                onClick = {
                    currentScreen = "button3"
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Button 3: Timer")
            }
        }

        else if (currentScreen == "button1") {

            Spacer(modifier = Modifier.height(80.dp))

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

        else if (currentScreen == "button2") {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Pixel Art")

                Spacer(modifier = Modifier.height(24.dp))

                PixelGrid(
                    pixels = pixels
                )
            }
        }
        else if (currentScreen == "button3") {
            val displayMinutes = remainingSeconds / 60
            val displaySeconds = remainingSeconds % 60
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text("Set a Timer")

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { newValue ->
                        minutesInput = newValue.filter { it.isDigit() }
                    },
                    label = {
                        Text("Minutes")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = secondsInput,
                    onValueChange = { newValue ->
                        secondsInput = newValue.filter { it.isDigit() }
                    },
                    label = {
                        Text("Seconds")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val minutes = minutesInput.toIntOrNull() ?: 0
                        val seconds = secondsInput.toIntOrNull() ?: 0
                        teamMessage = ""
                        showPokemon = false
                        remainingSeconds = minutes * 60 + seconds

                        if (remainingSeconds > 0) {
                            isTimerRunning = true
                        }
                    }
                ) {
                    Text("Start Timer")
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = String.format(
                        "%02d:%02d",
                        displayMinutes,
                        displaySeconds
                    )
                )
                if (showPokemon) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("A wild Pokémon appeared!")

                    Spacer(modifier = Modifier.height(12.dp))

                    AsyncImage(
                        model = pokemonImageUrl,
                        contentDescription = pokemonName,
                        modifier = Modifier.size(160.dp)
                    )

                    Text("Name: $pokemonName",color = rarityColor(pokemonRarity))
                    Text("Total Stats: $pokemonTotalStats",color = rarityColor(pokemonRarity))
                    Text("Rarity: $pokemonRarity",color = rarityColor(pokemonRarity))

                    // Team System
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (pokemonTeam.size < 3) {
                                    val pokemon = Pokemon(
                                        name = pokemonName,
                                        imageUrl = pokemonImageUrl,
                                        type = pokemonType,
                                        totalStats = pokemonTotalStats,
                                        rarity = pokemonRarity
                                    )

                                    pokemonTeam.add(pokemon)

                                    teamMessage = "$pokemonName added to your team!"

                                    showPokemon = false
                                } else {
                                    teamMessage = "Your team is full!"
                                }
                            }
                        ) {
                            Text("Add to Team")
                        }

                        Button(
                            onClick = {
                                teamMessage = "$pokemonName was skipped."
                                showPokemon = false
                            }
                        ) {
                            Text("Skip")
                        }
                        if (teamMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(teamMessage)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Your Team (${pokemonTeam.size}/3)")

                    pokemonTeam.forEach { pokemon ->
                        Text(
                            text = "${pokemon.name} - ${pokemon.rarity}",
                            color = rarityColor(pokemon.rarity)
                        )
                    }
                }
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


// Button 2
@Composable
fun PixelGrid(
    pixels: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        for (y in 0 until 16) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                for (x in 0 until 16) {
                    val index = y * 16 + x
                    val cellColor = colorFromHex(pixels[index])

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(cellColor)
                            .border(0.5.dp, Color.LightGray)
                    )
                }
            }
        }
    }
}

// receive data from websocket
private fun connectToPixelWebSocket(
    onPixelUpdate: (PixelUpdate) -> Unit
): WebSocket {
    val client = OkHttpClient()

    val request = Request.Builder()
        .url("wss://34.123.228.126/ws")
        .build()

    return client.newWebSocket(
        request,
        object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("PixelWS", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val text = bytes.utf8()
                val pixel = parsePixelUpdate(text)
                onPixelUpdate(pixel)
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {
                Log.e("PixelWS", "Error: ${t.message}", t)
            }
        }
    )
}

// parse pixel message
data class PixelUpdate(
    val x: Int,
    val y: Int,
    val color: String
)
private fun parsePixelUpdate(json: String): PixelUpdate {
    val jsonObject = JSONObject(json)
    val x = jsonObject.getInt("x")
    val y = jsonObject.getInt("y")
    val color = jsonObject.getString("color")

    return PixelUpdate(
        x = x,
        y = y,
        color = color
    )
}

private fun colorFromHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.White
    }
}

// button 3
data class Pokemon(
    val name: String,
    val imageUrl: String,
    val type: String,
    val totalStats: Int,
    val rarity: String
)

private fun getRarity(totalStats: Int): String {
    return when {
        totalStats < 350 -> "Common"
        totalStats < 450 -> "Uncommon"
        totalStats < 550 -> "Rare"
        totalStats < 600 -> "Very Rare"
        else -> "Ultra Rare"
    }
}

// pick a random pokemon
private suspend fun fetchRandomPokemon(): Pokemon? {
    return withContext(Dispatchers.IO) {
        try {
            repeat(20) {
                val randomId = (1..151).random()

                val client = OkHttpClient()

                val request = Request.Builder()
                    .url("https://pokeapi.co/api/v2/pokemon/$randomId")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@repeat
                }

                val body = response.body?.string()
                    ?: return@repeat

                val json = JSONObject(body)

                val name = json.getString("name")

                val imageUrl =
                    json.getJSONObject("sprites")
                        .getString("front_default")

                val typesArray = json.getJSONArray("types")

                val type =
                    typesArray
                        .getJSONObject(0)
                        .getJSONObject("type")
                        .getString("name")

                val statsArray = json.getJSONArray("stats")

                var totalStats = 0

                for (i in 0 until statsArray.length()) {
                    totalStats +=
                        statsArray
                            .getJSONObject(i)
                            .getInt("base_stat")
                }

                if (shouldAcceptPokemon(totalStats)) {
                    return@withContext Pokemon(
                        name = name,
                        imageUrl = imageUrl,
                        type = type,
                        totalStats = totalStats,
                        rarity = getRarity(totalStats)
                    )
                }
            }

            null

        } catch (e: Exception) {
            Log.e("Pokemon", "Failed to fetch Pokémon", e)
            null
        }
    }
}

// helper function for rarity
private fun shouldAcceptPokemon(totalStats: Int): Boolean {
    val chance = when {
        totalStats < 350 -> 100
        totalStats < 450 -> 65
        totalStats < 550 -> 30
        totalStats < 600 -> 12
        else -> 3
    }

    return (1..100).random() <= chance
}

private fun rarityColor(rarity: String): Color {
    return when (rarity) {
        "Common" -> Color.Black
        "Uncommon" -> Color.Blue
        "Rare" -> Color(0xFF800080)      // Purple
        "Very Rare" -> Color(0xFFFFA500) // Orange
        "Ultra Rare" -> Color(0xFFFFD700) // Gold
        else -> Color.Black
    }
}