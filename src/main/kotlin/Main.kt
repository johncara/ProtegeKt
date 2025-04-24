package org.caracode

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
import org.caracode.Prompts.Companion.INPUT_PROMPT

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatRequest(val model: String, val messages: List<ChatMessage>)

@Serializable
data class ChatChoice(val index: Int, val message: ChatMessage)

@Serializable
data class ChatResponse(val choices: List<ChatChoice>)

suspend fun main() {
//    val apiKey = System.getenv("OPENAI_API_KEY") ?: error("Set OPENAI_API_KEY environment variable.")
    val dotenv = dotenv()
    val apiKey = dotenv["OPENAI_API_KEY"] ?: error("API key not found")

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val request = ChatRequest(
        model = "gpt-4.1",
        messages = listOf(
            ChatMessage("user", INPUT_PROMPT)
        )
    )

    val response: ChatResponse = client.post("https://api.openai.com/v1/chat/completions") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $apiKey")
        setBody(request)
    }.body()

    println("OpenAI says: ${response.choices.first().message.content}")
    client.close()
}