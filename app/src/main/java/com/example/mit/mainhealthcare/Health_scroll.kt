package com.example.mit.mainhealthcare

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mit.GoogleFit.HeartRate
import com.example.mit.GoogleFit.StepCounter
import com.example.mit.R
import java.sql.DriverManager
import java.sql.SQLException


class Health_scroll : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.health_scroll)

        val button_data : ImageButton = findViewById(R.id.btn_data)
        val button_step : ImageButton = findViewById(R.id.btn_step)
        val button_heart : ImageButton = findViewById(R.id.btn_heart)

        button_data.setOnClickListener {
            val ID = intent.getStringExtra("ID")
            login("$ID")
        }

        button_step.setOnClickListener {
            val intent = Intent(this, StepCounter::class.java)
            startActivity(intent)
        }

        button_heart.setOnClickListener {
            val intent = Intent(this, HeartRate::class.java)
            startActivity(intent)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_logout) {
            val logout_intent = Intent(this, Health_main::class.java)
            Toast.makeText(this, "로그아웃을 누르셨습니다.", Toast.LENGTH_SHORT)
            startActivity(logout_intent)
        }
        return super.onOptionsItemSelected(item)
    }




    private fun login(ID : String) {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val jdbcURL = "jdbc:postgresql://192.168.0.4:5432/server"
        val username = "postgres"
        val password = "150526"

        try {
            val connection = DriverManager.getConnection(jdbcURL, username, password)
            println("Connected to PostgreSQL server")
            val sql = "SELECT 이름 FROM register WHERE 아이디='$ID'"
            val statement = connection.createStatement()
            val result = statement.executeQuery(sql)

            println("1차")

            while (result.next()) {

                println("2차")

                if (ID != null) {

                    val sql1 = "SELECT 생년월일 FROM register WHERE 아이디 = '$ID'"
                    val statement1 = connection.createStatement()
                    val result1 = statement1.executeQuery(sql1)

                    val sql2 = "SELECT 성별 FROM register WHERE 아이디= '$ID'"
                    val statement2 = connection.createStatement()
                    val result2 = statement2.executeQuery(sql2)


                    while (result1.next() and result2.next() ) {
                        val birth = result1.getString("생년월일")
                        val gender = result2.getString("성별")
                        val name = result.getString("이름")

                        if (birth != null) {

                            val intent = Intent(this, Health_data::class.java)

                            intent.putExtra("GENDER", gender)
                            intent.putExtra("ID", ID)
                            intent.putExtra("BIRTH", birth)
                            intent.putExtra("NAME", name)
                            println("-------------------------------------$birth")
                            println("-------------------------------------$ID")
                            println("-------------------------------------$gender")
                            println("-------------------------------------$name")

                            startActivity(intent)

                        }
                    }
                } else {
                    Toast.makeText(this, "Fail.", Toast.LENGTH_SHORT).show()
                }
            }
            connection.close()

        } catch (e: SQLException) {
            println("Error in connected to PostgreSQL server")
            Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

}
