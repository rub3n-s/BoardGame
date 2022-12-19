package pt.isec.boardgame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class PlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        findViewById<Button>(R.id.btnConfirm).setOnClickListener() {
            val playerName = findViewById<EditText>(R.id.etPlayerName).text
            if (playerName == null || playerName.isEmpty())
                Toast.makeText(applicationContext,"You need to insert a player name",Toast.LENGTH_LONG).show()
            else
                startActivity(GameActivity.getClientModeIntent(this, playerName.toString()))
        }
    }
}