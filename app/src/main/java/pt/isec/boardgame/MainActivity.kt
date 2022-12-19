package pt.isec.boardgame

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnPlay).setOnClickListener() {
            startActivity(Intent(this,PlayerActivity::class.java))
        }

        findViewById<Button>(R.id.btnTop5).setOnClickListener() {
            startActivity(Intent(this,Top5Activity::class.java))
        }

        findViewById<Button>(R.id.btnLeave).setOnClickListener() {
            finish()
        }
    }
}