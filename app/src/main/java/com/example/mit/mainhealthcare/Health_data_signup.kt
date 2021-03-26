package com.example.mit.mainhealthcare


import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mit.R
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException


class Health_data_signup : AppCompatActivity() {

    val TAG = "DATA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.health_data_signup)

        val button: Button = findViewById(R.id.button)
        val T_birth : TextView = findViewById(R.id.BIRTH_data)
        val T_id : TextView = findViewById(R.id.ID_data)
        val T_name : TextView = findViewById(R.id.NAME_data)
        val T_gender : TextView = findViewById(R.id.GENDER_data)

        if(intent.hasExtra("ID")) { T_id.setText(intent.getStringExtra("ID")) }
        if(intent.hasExtra("NAME")) {T_name.setText(intent.getStringExtra("NAME")) }
        if(intent.hasExtra("BIRTH")) { T_birth.setText(intent.getStringExtra("BIRTH")) }
        if (intent.hasExtra("GENDER")) { T_gender.setText(intent.getStringExtra("GENDER")) }

        button.setOnClickListener {

            val ID = intent.getStringExtra("ID")
            Log.e(TAG, "ID : $ID")

            val height: EditText = findViewById(R.id.height)
            val weight: EditText = findViewById(R.id.weight)
            val HEIGHT = height.text.toString()
            val WEIGHT = weight.text.toString()

            connect("$ID", "$HEIGHT", "$WEIGHT")

        }
    }


    private fun connect(ID: String, HEIGHT: String, WEIGHT: String) {

        //이 부분 없으면 오류 이유 파익 x
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val jdbcURL = "jdbc:postgresql://192.168.0.4:5432/server" //서버 주소
        val username = "postgres" // 유저 이름
        val password = "150526" // 비번

        try {
            val connection = DriverManager.getConnection(jdbcURL, username, password) //연결한다,
            println("Connected to PostgreSQL server")

            /** 입력 */
            // 쿼리에 입력한다.
            var sql = "INSERT INTO data (아이디, 신장, 몸무게)" + " VALUES (?,?,?)"

            //"INSERT IGNORE INTO data (아이디, 신장, 몸무게) VALUES ('$ID', '$HEIGHT','$WEIGHT') " +
            // "ON DUPLICATE KEY UPDATE 신장='$HEIGHT', 몸무게='$WEIGHT'"

            val statement: PreparedStatement = connection.prepareStatement(sql)

            // 이 값을 테이블에 넣음
            statement.setString(1, "$ID")
            statement.setString(2, "$HEIGHT")
            statement.setString(3, "$WEIGHT")

            val rows = statement.executeUpdate()
            
            if (rows > 0) {
                println("A new contact has been inserted.")
                val intent = Intent(this, Health_scroll::class.java)
                intent.putExtra("ID", ID)
                Toast.makeText(this, "저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                startActivity(intent)
                finish()
            }
            connection.close()

        } catch (e: SQLException) {
            println("Error in connected to PostgreSQL server")
            e.printStackTrace()
            Toast.makeText(this, "저장을 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
