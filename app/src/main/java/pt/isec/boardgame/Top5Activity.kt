package pt.isec.boardgame

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Top5Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top5)

        // Get all documents data from Firestore
        getDocumentsData()
    }

    private fun getDocumentsData() {
        val db = Firebase.firestore
        val dataSets = ArrayList<PlayerModel>()
        db.collection("Top5").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val name = document["player"].toString()
                val points = document["points"] as Long
                val level = document["level"] as Long
                val time = document["time"] as Long

                val player = PlayerModel(name,points.toInt(),level.toInt(),time)
                dataSets.add(player)
                Log.i(TAG, "getDocumentsData: name=$name,points=$points,level=$level,time=$time")
            }
            //call a function to work with your array
            publishTop5(dataSets)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun publishTop5(players : ArrayList<PlayerModel>) {
        // Fill TextViews with data
        for (i in 0 until players.size) {
            when (i) {
                0 -> findViewById<TextView>(R.id.tvTop1).text = "1. ${players[i].name}, ${players[i].points} points, ${players[i].time}s"
                1 -> findViewById<TextView>(R.id.tvTop2).text = "2. ${players[i].name}, ${players[i].points} points, ${players[i].time}s"
                2 -> findViewById<TextView>(R.id.tvTop3).text = "3. ${players[i].name}, ${players[i].points} points, ${players[i].time}s"
                3 -> findViewById<TextView>(R.id.tvTop4).text = "4. ${players[i].name}, ${players[i].points} points, ${players[i].time}s"
                4 -> findViewById<TextView>(R.id.tvTop5).text = "5. ${players[i].name}, ${players[i].points} points, ${players[i].time}s"
            }
        }
    }
}