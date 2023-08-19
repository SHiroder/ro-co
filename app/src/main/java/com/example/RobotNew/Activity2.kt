package com.example.RobotNew

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.lang.Runnable
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class Activity2 : AppCompatActivity(), OnGoToLocationStatusChangedListener {
    private lateinit var robot: Robot
    private var state: Int = -1
    private var pp: Int = 1
    private var ii: Int = 9
    private var pp2: Int = 0
    private var ii2: Int = 8
    private var ii3: Int = 9
    private var temiId: Int = 2
    private var lol = false
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2)
        robot = Robot.getInstance()         // Initialize the robot instance


        Thread(Runnable {  // Create a new thread to perform some operations
            var k = 0
            while (k < 2) {
                Thread.sleep(1000)
                k += 1
            }
            try {
                robot.goTo("home base", backwards = false, noBypass = false)  // Attempt to make the robot go to the "home base"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }

    /**
     * This function, Apilocation, fetches data from a remote API endpoint using a background thread.
     *
     * @param callback A lambda function that takes three ArrayLists as parameters, representing room data, patient names, and patient IDs.
     */
    private fun Apilocation(callback: (ArrayList<String>, ArrayList<String>, ArrayList<String>) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Fetch data from the remote API endpoint
                val result = URL("http://10.232.254.109:8080/project/DB/get/room.php").readText()
                val jsonObject = JSONObject(result)
                val jsonArray = jsonObject.getJSONArray("data")
                val arrayList = ArrayList<String>()
                val arrayText = ArrayList<String>()
                val arrayIDp = ArrayList<String>()
    
                // Parse the JSON data and populate the ArrayLists
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val room = jsonObject.getString("Room")
                    val patientName = jsonObject.getString("Patientname")
                    val IDp = jsonObject.getString("IDp")
                    arrayList.add(room)
                    arrayText.add(patientName)
                    arrayIDp.add(IDp)
                }
                // Log the retrieved data
                Log.d("activity2", "Item location: $arrayList")
                // Invoke the callback function with the retrieved data
                callback(arrayList, arrayText, arrayIDp)
            } catch (e: Exception) {
                // Handle exceptions, if any, and print the stack trace
                e.printStackTrace()
            }
        }
    }

    
    private fun updateLocationToServer(locationId: Int) { // Define a private function to update location information on the server.
        val client = OkHttpClient() // Create an instance of OkHttpClient to make HTTP requests.
        val url = "http://10.232.254.109:8080/project/DB/get/temi_location.php" // Define the URL of the server endpoint you want to send the data to.
        val formBody = FormBody.Builder() // Create a request body with the location ID and a success status parameter.
            .add("ID", locationId.toString())
            .add("status_success", "success")
            .build()
        // Build an HTTP POST request with the URL and the request body.
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()
        client.newCall(request).enqueue(object : Callback { // Send the HTTP request asynchronously using OkHttp's enqueue method.
            override fun onFailure(call: Call, e: IOException) { // This function is called when the server responds to the request.
                Log.e("Activity2", "Error updating location: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) { // This function is called when the server responds to the request.
                if (response.isSuccessful) {
                    Log.d("Activity2", "Location updated successfully")  // Log a success message if the response indicates success.
                } else {
                    Log.e("Activity2", "Error updating location: ${response.code}") // Log an error message if the response indicates an error.
                }
            }
        })
    }


    private fun updateStatusToIdle() {
        val client = OkHttpClient()  // Create an instance of the OkHttpClient
        val url = "http://10.232.254.109:8080/project/DB/get/temi_location_idle.php"  // Define the URL for the HTTP request
        val requestBody = JSONObject().apply {         // Create a JSON object with a "status_success" field set to "IDLE"
            put("status_success", "IDLE")
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())
        // Build an HTTP request with the specified URL and a PATCH request method
        val request = Request.Builder()
            .url(url)
            .patch(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback { // Asynchronously send the HTTP request and handle the response
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Activity2", "Error updating status: ${e.message}") // Handle the case where the request fails
            }
            override fun onResponse(call: Call, response: Response) {

                if (response.isSuccessful) { // Handle the response from the server
                    Log.d("Activity2", "Status updated to IDLE successfully")    // If the response is successful (HTTP status 200), log a success message
                    updateTemiStartStatus(client) // Call a function to update the "Temi" start status
                } else {
                    Log.e("Activity2", "Error updating status: ${response.code}")  // If the response is not successful, log an error message with the HTTP status code
                }
            }
        })
    }


    private fun updateTemiLocation(client: OkHttpClient) {
    // Move the robot to its home base with specific parameters.
    robot.goTo("home base", backwards = false, noBypass = false)
    // Log a message indicating that the temi_start is updated to IDLE successfully.
    Log.d("Activity2", "temi_start updated to IDLE successfully")
    // Create an Intent to navigate back to the MainActivity.
    val intent = Intent(this@Activity2, MainActivity::class.java)
    // Start the MainActivity using the created intent.
    startActivity(intent)
}
    
      // Create ArrayLists to store data
    private var listlocation = ArrayList<String>()
    private var arrayText = ArrayList<String>()
    private var arrayIDp = ArrayList<String>()
    
    // Implementing an interface method for location status change
    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        try {
            if (status == "complete") {
                if (listlocation.size > state) {
                    // Create a new thread for network operations
                    Thread(Runnable {
                        try {
                            while (!lol) {
                                // Construct the URL for fetching data
                                val url = "http://10.232.254.109:8080/project/DB/get/healthbox_symptoms.php?Temi_ID=$ii2"
                                Log.d("HBSYMPTOMS", "url status: $url / dataObject : $ii2 ")
    
                                // Read data from the URL
                                val response = URL(url).readText()
                                val jsonObject = JSONObject(response)
                                val success = jsonObject.getBoolean("success")
    
                                if (success) {
                                    // Log the status and data
                                    Log.d(
                                        "symptoms",
                                        "updateLocationToServer: $state / insertDataToServer : $temiId / StatusOnOff : $ii3 at $pp2 //$state < ${listlocation.size} "
                                    )
    
                                    // Extract data from the response
                                    val dataArray = jsonObject.getJSONArray("data")
                                    val dataObject = dataArray.getJSONObject(0)
                                    val ss = dataObject.getString("Status_symptoms")
                                    Log.d("Database", "url status: $url / dataObject : $ss / data $dataObject")
    
                                    if (ss == "success") {
                                        Log.d("ss", "$ss = success")
    
                                        if (state < listlocation.size) {
                                            // Log information about the state and location
                                            Log.d("state < listlocation.size", "$state < ${listlocation.size}")
    
                                            // Perform actions with the robot
                                            robot.goTo(
                                                listlocation[state],
                                                backwards = false,
                                                noBypass = false
                                            )
                                            robot.speak(
                                                TtsRequest.create("Go to room "+listlocation[state]+"           Please check temperature, heart rate, and blood oxygen levels on the right side of the box.",
                                                    language = TtsRequest.Language.TH_TH
                                                )
                                            )
    
                                            updateLocationToServer(pp2)
                                            insertDataToServer(temiId)
                                            StatusOnOff(ii3)
    
                                            pp++
                                            ii++
                                            Log.d("robotgoto", " status: ${listlocation[state]} on ii3 $ii3 /ii2 $ii2 /ii $ii area $state at pp2 $pp2 / pp $pp")
                                            Thread.sleep(10000)
                                        } else if (pp2 == listlocation.size + 1) {
                                            var final = false
                                            while (!final) {
                                                // Check for the final status
                                                Log.d("ppez", " $pp2 >= ${listlocation.size + 1} ? $ii")
                                                val url2 =
                                                    "http://10.232.254.109:8080/project/DB/get/healthbox_symptoms.php?Temi_ID=$ii2"
                                                val response2 = URL(url2).readText()
                                                val jsonObject2 = JSONObject(response2)
                                                val dataArray2 = jsonObject2.getJSONArray("data")
                                                val dataObject2 = dataArray2.getJSONObject(0)
                                                val ss2 = dataObject2.getString("Status_symptoms")
                                                Log.d("symptoms_final", "url status: $url2 / dataObject : $ss2 ")
    
                                                if (ss2 == "success") {
                                                    robot.speak(
                                                        TtsRequest.create("Returning to HOMEBASE",
                                                            language = TtsRequest.Language.TH_TH
                                                        )
                                                    )
                                                    updateStatusToIdle()
                                                    final = true
                                                    lol = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }).start()
                    state += 1
                    ii2 += 1
                    ii3 += 1
                    pp2 += 1
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


  // Function to turn a device on or off based on an integer ID
fun StatusOnOff(ii: Int) {
    try {
        // Construct the URL to control the device's status
        val url = "http://10.232.254.109:8080/project/update/healthbox.php?ID=$ii&Status_Onoff=On"
        // Send a GET request to the URL and read the response
        val response = URL(url).readText()
        // Log the response for debugging purposes
        Log.d("activity2", "StatusOnOff: $response")
    } catch (e: Exception) {
        // Handle any exceptions that may occur during the process
        e.printStackTrace()
    }
}

fun insertDataToServer(temiId: Int) { // Function to insert data to a server using a POST request
    val postData = "temi_id=$temiId" // Prepare the data to be sent in the POST request
    val url = "http://10.232.254.109:8080/project/update/temi_history.php"     // Define the URL to the server endpoint
    try {
        val connection = URL(url).openConnection() as HttpURLConnection   // Create an HTTP connection to the server
        connection.requestMethod = "POST"
        connection.doOutput = true
        val writer = OutputStreamWriter(connection.outputStream)    // Write the data to the output stream of the connection
        writer.write(postData)
        writer.flush()
        val responseCode = connection.responseCode         // Get the response code from the server
        if (responseCode == HttpURLConnection.HTTP_OK) {
          
            val response = connection.inputStream.bufferedReader().readText()   // Read and log the response from the server
            Log.d("insertDataToServer", "Response from server: $response")
        } else {
            Log.e("insertDataToServer", "HTTP request failed with response code: $responseCode")   // Log an error message if the HTTP request fails
        }
        // Disconnect the connection when done
        connection.disconnect()
    } catch (e: Exception) {
        Log.e("insertDataToServer", "Error: ${e.message}")  // Handle any exceptions that may occur during the process
    }
}

override fun onStart() {
    super.onStart()
    robot.addOnGoToLocationStatusChangedListener(this) // Add a listener to track changes in robot location
    // Call Apilocation function to retrieve location data and store it in variables
    Apilocation { locations, texts, IDps ->
        listlocation = locations
        arrayText = texts
        arrayIDp = IDps
    }
}
