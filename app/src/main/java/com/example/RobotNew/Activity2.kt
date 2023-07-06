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
    private var temiId: Int = 2
    private var lol = false
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2)
        robot = Robot.getInstance()

        Thread(Runnable {
            var k = 0
            while (k < 2) {
                Thread.sleep(1000)
                k += 1
            }
            try {

                robot.goTo("home base", backwards = false, noBypass = false)


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }

    private fun Apilocation(callback: (ArrayList<String>, ArrayList<String>, ArrayList<String>) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val result = URL("http://10.232.4.111:8080/project/DB/get/room.php").readText()
                val jsonObject = JSONObject(result)
                val jsonArray = jsonObject.getJSONArray("data")
                val arrayList = ArrayList<String>()
                val arrayText = ArrayList<String>()
                val arrayIDp = ArrayList<String>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val room = jsonObject.getString("Room")
                    val patientName = jsonObject.getString("Patientname")
                    val IDp = jsonObject.getString("IDp")
                    arrayList.add(room)
                    arrayText.add(patientName)
                    arrayIDp.add(IDp)
                }
                Log.d("activity2", "Item location: $arrayList")
                callback(arrayList, arrayText, arrayIDp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun updateLocationToServer(locationId: Int) {
        val client = OkHttpClient()
        val url = "http://10.232.4.111:8080/project/DB/get/temi_location.php"
        val formBody = FormBody.Builder()
            .add("ID", locationId.toString())
            .add("status_success", "success")
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Activity2", "Error updating location: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("Activity2", "Location updated successfully")
                } else {
                    Log.e("Activity2", "Error updating location: ${response.code}")
                }
            }
        })
    }

    private fun updateStatusToIdle() {
        val client = OkHttpClient()
        val url = "http://10.232.4.111:8080/project/DB/get/temi_location_idle.php"
        val requestBody = JSONObject().apply {
            put("status_success", "IDLE")
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .patch(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Activity2", "Error updating status: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("Activity2", "Status updated to IDLE successfully")
                    updateTemiStartStatus(client)
                } else {
                    Log.e("Activity2", "Error updating status: ${response.code}")
                }
            }
        })
    }

    private fun updateTemiStartStatus(client: OkHttpClient) {
        val temiStartUrl = "http://10.232.4.111:8080/project/update/temi_start.php?ID=2"
        val temiStartRequestBody = JSONObject().apply {
            put("Status_start", "IDLE")
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val temiStartRequest = Request.Builder()
            .url(temiStartUrl)
            .put(temiStartRequestBody)
            .build()
        client.newCall(temiStartRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Activity2", "Error updating temi_start: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    updateTemiLocation(client)
                } else {
                    Log.e("Activity2", "Error updating temi_start: ${response.code}")
                }
            }
        })
    }

    private fun updateTemiLocation(client: OkHttpClient) {
        robot.goTo("home base", backwards = false, noBypass = false)
        Log.d("Activity2", "temi_start updated to IDLE successfully")
        val intent = Intent(this@Activity2, MainActivity::class.java)
        startActivity(intent)
    }

    private var listlocation = ArrayList<String>()
    private var arrayText = ArrayList<String>()
    private var arrayIDp = ArrayList<String>()
    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        try {
            if (status == "complete") {
                if (listlocation.size > state) {
                    Thread(Runnable {
                        try {

                            while (!lol) {
                                val url = "http://10.232.4.111:8080/project/DB/get/healthbox_symptoms.php?Temi_ID=$ii2"
                                Log.d("HBSYMPTOMS", "url status: $url / dataObject : $ii2 ")
                                Log.d("II", "dataObject : $ii2 ")
                                val response = URL(url).readText()
                                val jsonObject = JSONObject(response)
                                val success = jsonObject.getBoolean("success")
                                if (success) {
                                    Log.d(
                                        "symptoms",
                                        "updateLocationToServer: $state / insertDataToServer : $temiId / StatusOnOff : $ii "
                                    )
                                    val dataArray = jsonObject.getJSONArray("data")
                                        val dataObject = dataArray.getJSONObject(0)
                                        val ss = dataObject.getString("Status_symptoms")
                                        Log.d("Database", "url status: $url / dataObject : $ss / data $dataObject")

                                        if (ss == "success") {
                                            if (state < listlocation.size) {

                                                robot.goTo(
                                                    listlocation[state],
                                                    backwards = false,
                                                    noBypass = false

                                                )
                                                robot.speak(
                                                    TtsRequest.create(
                                                        arrayText[state],
                                                        language = TtsRequest.Language.TH_TH
                                                    )
                                                )
                                                updateLocationToServer(pp)
                                                insertDataToServer(temiId)
                                                StatusOnOff(ii)
                                                pp++
                                                ii++
                                                Log.d("robotgoto", " status: ${listlocation[state]} on $ii area $state")


                                                Log.d("II2", "dataObject : $ii  //$pp //$state")

                                        }else  if (pp2 == listlocation.size+1) {
                                                var final = false
                                                while (!final) {
                                                    Log.d("ppez", " $pp >= ${listlocation.size+1} ? $ii")
                                                    val url2 =
                                                        "http://10.232.4.111:8080/project/DB/get/healthbox_symptoms.php?Temi_ID=$ii2"
                                                    val response2 = URL(url2).readText()
                                                    val jsonObject2 = JSONObject(response2)
                                                    val dataArray2 = jsonObject2.getJSONArray("data")
                                                    val dataObject2 = dataArray2.getJSONObject(0)
                                                    val ss2 = dataObject2.getString("Status_symptoms")
                                                    Log.d("symptoms_final", "url status: $url2 / dataObject : $ss2 ")
                                                    Thread.sleep(3000)
                                                    if (ss2 == "success") {
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
                    state+=1
                    ii2+=1
                    pp2+=1

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun StatusOnOff(ii: Int) {
        try {

            val url = "http://10.232.4.111:8080/project/update/healthbox.php?ID=$ii&Status_Onoff=On"
            val response = URL(url).readText()
            Log.d("activity2", "StatusOnOff: $response")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun insertDataToServer(temiId: Int) {
        val postData = "temi_id=$temiId"
        val url = "http://10.232.4.111:8080/project/update/temi_history.php"

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(postData)
            writer.flush()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                Log.d("insertDataToServer", "Response from server: $response")
            } else {
                Log.e("insertDataToServer", "HTTP request failed with response code: $responseCode")
            }

            connection.disconnect()
        } catch (e: Exception) {
            Log.e("insertDataToServer", "Error: ${e.message}")
        }
    }


    override fun onStart() {
        super.onStart()
        robot.addOnGoToLocationStatusChangedListener(this)
        Apilocation { locations, texts,IDps ->
            listlocation = locations
            arrayText = texts
            arrayIDp = IDps

        }
    }
}