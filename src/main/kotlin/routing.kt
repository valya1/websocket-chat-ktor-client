import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() {

    val client = HttpClient(CIO) {
        install(WebSockets)
    }

    runBlocking {
        val firstClient = launch {
            client.webSocket(host = "158.160.60.231", port = 8000, path = "ws/chat/room2/") {
                val listen = launch {
                    incoming.receiveAsFlow()
                        .collect { frame ->
                            if (frame is Frame.Text) {
                                val message = frame.readText()
                                println("Message received: $message")
                            }
                        }
                }
                val write = launch {
                    println("Enter your username: ")
                    val userName = readLine()!!
                    val authMess = Json.encodeToString(ChatCommand.Command("auth", userName))
                    println("Auth message is: $authMess")
                    outgoing.send(Frame.Text(authMess))
                    while (true) {
                        val message =
                            ChatCommand.Message(command = "sendmessage", message = requireNotNull(readLine()))
                        outgoing.send(Frame.Text(Json.encodeToString(message)))

                    }
                }
                listen.join()
                write.join()
            }
        }

        firstClient.join()
        client.close()
    }
}

sealed class ChatCommand {
    @Serializable
    data class Message(val command: String, val message: String)

    @Serializable
    data class Command(val command: String, val username: String)
}
