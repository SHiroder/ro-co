package com.example.lib
// Test call API function system 
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main() {
    val client = HttpClient.newBuilder().build();
    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://61b3-202-176-124-129.ap.ngrok.io/project/DB/getrRoom.php"))
        .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString());
    println(response.body())
}
