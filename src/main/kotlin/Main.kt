import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import org.caracode.Prompts.Companion.SYSTEM_PROMPT
import kotlin.text.get

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatRequest(val model: String, val messages: List<ChatMessage>)

@Serializable
data class ChatChoice(val index: Int, val message: ChatMessage)

@Serializable
data class ChatResponse(val choices: List<ChatChoice>)

fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 30_000 // 30 seconds
            connectTimeoutMillis = 10_000 // 10 seconds
            socketTimeoutMillis = 30_000 // 30 seconds
        }
    }
}

suspend fun interactWithOpenAI(client: HttpClient, prompt: String, apiKey: String): String {
    val request = ChatRequest(
        model = "gpt-4",
        messages = listOf(
            ChatMessage("system", "You are a helpful assistant."),
            ChatMessage("user", prompt)
        )
    )

    val response: ChatResponse = client.post("https://api.openai.com/v1/chat/completions") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $apiKey")
        setBody(request)
    }.body()

    return response.choices.first().message.content.trim()
}

fun main() = runBlocking {
    val dotenv = dotenv()
    val apiKey = dotenv["OPENAI_API_KEY"] ?: error("API key not found")

    val client = createHttpClient()

    try {
        val conversationHistory = mutableListOf(
            ChatMessage("system", SYSTEM_PROMPT)
        )

        val initialResponse = interactWithOpenAI(client, SYSTEM_PROMPT, apiKey)
        conversationHistory.add(ChatMessage("assistant", initialResponse))
        println(initialResponse)

        while (true) {
            print("\nPerson 1: ")
            val userInput = readLine()?.trim() ?: ""

            if (userInput.lowercase() == "exit") {
                println("Ending the game.")
                break
            }

            conversationHistory.add(ChatMessage("user", userInput))
            val openAIResponse = client.post("https://api.openai.com/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                setBody(ChatRequest("gpt-4", conversationHistory))
            }.body<ChatResponse>().choices.first().message.content.trim()

            conversationHistory.add(ChatMessage("assistant", openAIResponse))
            println(openAIResponse)
        }
    } finally {
        client.close()
    }
}