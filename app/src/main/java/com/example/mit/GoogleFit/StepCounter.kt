package com.example.mit.GoogleFit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mit.BuildConfig
import com.example.mit.R
import com.example.mit.mainhealthcare.Health_main
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.material.snackbar.Snackbar
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit

val TAG = "StepCounter"
enum class FitActionRequestCode { SUBSCRIBE, READ_DATA }

class StepCounter : AppCompatActivity() {

    private val fitnessOptions = FitnessOptions.builder()
            .accessActivitySessions(FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .build()

    private val runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stepcounter)

        val button : Button = findViewById(R.id.button2)
        val button1 : Button = findViewById(R.id.button4)

        button1.setOnClickListener {
            fitSignIn(FitActionRequestCode.READ_DATA)
        }

        fitSignIn(FitActionRequestCode.READ_DATA)
        checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)

//        button.setOnClickListener{
//            //initializeLogging()
//            checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)
//            //historyAPI()
//        }
    }



    private fun checkPermissionsAndRun(fitActionRequestCode: FitActionRequestCode) {
        if (permissionApproved()) {
            fitSignIn(fitActionRequestCode)
        } else {
            requestRuntimePermissions(fitActionRequestCode)
        }
    }


    private fun fitSignIn(requestCode: FitActionRequestCode) {
        if (oAuthPermissionsApproved()) {
            performActionForRequestCode(requestCode)
        } else {
            requestCode.let {
                GoogleSignIn.requestPermissions(
                        this,
                        requestCode.ordinal,
                        getGoogleAccount(), fitnessOptions
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_OK -> {
                val postSignInAction = FitActionRequestCode.values()[requestCode]
                performActionForRequestCode(postSignInAction)
            }
            else -> oAuthErrorMsg(requestCode, resultCode)
        }
    }

    private fun performActionForRequestCode(requestCode: FitActionRequestCode) =
            when (requestCode) {
                FitActionRequestCode.READ_DATA -> readData()
                //FitActionRequestCode.READ_DATA -> historyAPI()
                FitActionRequestCode.SUBSCRIBE -> subscribe()
            }

    private fun oAuthErrorMsg(requestCode: Int, resultCode: Int) {
        val message = """
            There was an error signing into Fit. Check the troubleshooting section of the README
            for potential issues.
            Request code was: $requestCode
            Result code was: $resultCode
        """.trimIndent()
        Log.e(TAG, message)
    }

    private fun oAuthPermissionsApproved() = GoogleSignIn.hasPermissions(
            getGoogleAccount(),
            fitnessOptions
    )


    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

    private fun subscribe() {

        Fitness.getRecordingClient(this, getGoogleAccount())
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Successfully subscribed!")
                    } else {
                        Log.w(TAG, "There was a problem subscribing.", task.exception)
                    }
                }
    }

    private fun dumpDataSet(dataSet: DataSet) {
        Log.i(TAG, "Data returned for Data type: ${dataSet.dataType.name}")
        for (dp in dataSet.dataPoints) {
            Log.i(TAG,"Data point:")
            Log.i(TAG,"\tType: ${dp.dataType.name}")
            Log.i(TAG,"\tStart: ${dp.getStartTimeString()}")
            Log.i(TAG,"\tEnd: ${dp.getEndTimeString()}")
            for (field in dp.dataType.fields) {
                Log.i(TAG,"\tField: ${field.name.toString()} Value: ${dp.getValue(field)}")
            }
        }
    }

    private fun DataPoint.getStartTimeString() = Instant.ofEpochSecond(this.getStartTime(TimeUnit.SECONDS))
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime().toString()

    private fun DataPoint.getEndTimeString() = Instant.ofEpochSecond(this.getEndTime(TimeUnit.SECONDS))
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime().toString()



    private fun readData() {

        val data_list = mutableListOf<Int>()

        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val startTime = endTime.minusWeeks(1)
        Log.i(TAG, "Range Start: $startTime")
        Log.i(TAG, "Range End: $endTime")

        //val data_list = ArrayList<Int>()

        Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener { dataSet ->
                    var total = when {
                        dataSet.isEmpty -> 0
                        else -> dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                    }

                    var text7 : TextView = findViewById(R.id.textView7)
                    //data_list.add(total)
                    text7.setText("총 걸음 수 : $total")
                    Log.i(TAG, "Total steps: $total")

                    val ID = intent.getStringExtra("ID")
                    var total_String = total.toString()
                    //connect("$ID", "$total_String")

        val readRequest =
                DataReadRequest.Builder()
                        .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                        .build()

        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener { response ->
                    for (dataSet in response.buckets.flatMap { it.dataSets }) {
                        dumpDataSet(dataSet)

                        val hit_total = when {
                            dataSet.isEmpty -> 0
                            else -> dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                        }
                        Log.i(TAG, "과거 걸음 수 : $hit_total")

                        data_list.add(hit_total)
                    }
                    println(data_list)
                    data_list.add(total)

                    var text6 : TextView = findViewById(R.id.textView6)
                    text6.setText("7일전 : " + data_list[0]+" 6일전 : " + data_list[1]+" 5일전 : " + data_list[2]+" 4일전 : "
                            + data_list[3]+" 3일전 : " + data_list[4]+" 2일전 : "
                            + data_list[5]+" 1일전 : " + data_list[6])

                    val lineChart : LineChart = findViewById(R.id.Chart)
                    val visitors = ArrayList<Entry>()

                    visitors.add(Entry(1.0f, data_list[0].toFloat()))
                    visitors.add(Entry(2.0f, data_list[1].toFloat())) // (8, 400)
                    visitors.add(Entry(3.0f, data_list[2].toFloat()))
                    visitors.add(Entry(4.0f, data_list[3].toFloat()))
                    visitors.add(Entry(5.0f, data_list[4].toFloat()))
                    visitors.add(Entry(6.0f, data_list[5].toFloat()))
                    visitors.add(Entry(7.0f, data_list[6].toFloat()))
                    visitors.add(Entry(8.0f, total.toFloat()))

                    val lineDataSet = LineDataSet(visitors, "7일 전부터 오늘의 걸음 수 그래프")

                    lineDataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
                    lineDataSet.valueTextColor = Color.BLACK
                    lineDataSet.valueTextSize = 16f

                    val lineData = LineData(lineDataSet)

                    lineChart.data = lineData
                    lineChart.invalidate()
                    // barChart.description.text = "Bar Chart Example"
                    lineChart.animateY(2000)

                    }
                .addOnFailureListener { e ->Log.w(TAG,"There was an error reading data from Google Fit", e) }

                }
                .addOnFailureListener { e -> Log.w(TAG, "There was a problem getting the step count.", e) }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the main; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_read_data) {
            fitSignIn(FitActionRequestCode.READ_DATA)
            return true
        }
        if (id == R.id.action_logout) {
            val logout_intent = Intent(this, Health_main::class.java)
            Toast.makeText(this, "로그아웃을 누르셨습니다.", Toast.LENGTH_SHORT)
            startActivity(logout_intent)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun permissionApproved(): Boolean {
        return if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            true
        }
    }

    private fun requestRuntimePermissions(requestCode: FitActionRequestCode) {
        val shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACTIVITY_RECOGNITION
                )
        requestCode.let {
            if (shouldProvideRationale) {
                Log.i(TAG, "Displaying permission rationale to provide additional context.")
                Snackbar.make(
                        findViewById(R.id.main_activity_view),
                        R.string.permission_rationale,
                        Snackbar.LENGTH_INDEFINITE
                )
                        .setAction(R.string.ok) {
                            // Request permission
                            ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                                    requestCode.ordinal
                            )
                        }
                        .show()
            } else {
                Log.i(TAG, "Requesting permission")
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                        requestCode.ordinal
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>,
            grantResults: IntArray
    ) {
        when {
            grantResults.isEmpty() -> {

                Log.i(TAG, "User interaction was cancelled.")
            }
            grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                val fitActionRequestCode = FitActionRequestCode.values()[requestCode]
                fitActionRequestCode.let {
                    fitSignIn(fitActionRequestCode)
                }
            }
            else -> {
                Snackbar.make(
                        findViewById(R.id.main_activity_view),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE
                )
                        .setAction(R.string.settings) {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
            }
        }
    }
}
