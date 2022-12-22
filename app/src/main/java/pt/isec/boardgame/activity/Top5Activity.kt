package pt.isec.boardgame.activity

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.isec.boardgame.R
import pt.isec.boardgame.databinding.ActivityTop5Binding
import pt.isec.boardgame.model.PlayerModel
import java.io.File

class Top5Activity : AppCompatActivity() {
    private lateinit var binding: ActivityTop5Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTop5Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get all documents data from Firestore
        getDocumentsData()
    }

    private fun getDocumentsData() {
        val db = Firebase.firestore
        val dataSets = ArrayList<PlayerModel>()
        db.collection("Top5").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val imageUrl = document["image"].toString()
                val name = document["player"].toString()
                val points = document["points"] as Long
                val level = document["level"] as Long
                val time = document["time"] as Long

                val player = PlayerModel(imageUrl,name,points.toInt(),level.toInt(),time)
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
                0 -> {
                    binding.tvTop1Name.text = players[i].name
                    val imgFile = File(players[i].imagePath)
                    if(imgFile.exists()) binding.ivTop1.setImageBitmap(BitmapFactory.decodeFile(imgFile.absolutePath))
                    binding.tvTop1Points.text = players[i].points.toString()
                    binding.tvTop1Time.text = players[i].time.toString()
                }
                1 -> {
                    binding.tvTop2Name.text = players[i].name
                    val imgFile = File(players[i].imagePath)
                    if(imgFile.exists()) binding.ivTop2.setImageBitmap(BitmapFactory.decodeFile(imgFile.absolutePath))
                    binding.tvTop2Points.text = players[i].points.toString()
                    binding.tvTop2Time.text = players[i].time.toString()
                }
                2 -> {
                    binding.tvTop3Name.text = players[i].name
                    val imgFile = File(players[i].imagePath)
                    if(imgFile.exists()) binding.ivTop3.setImageBitmap(BitmapFactory.decodeFile(imgFile.absolutePath))
                    binding.tvTop3Points.text = players[i].points.toString()
                    binding.tvTop3Time.text = players[i].time.toString()
                }
                3 -> {
                    binding.tvTop4Name.text = players[i].name
                    val imgFile = File(players[i].imagePath)
                    if(imgFile.exists()) binding.ivTop4.setImageBitmap(BitmapFactory.decodeFile(imgFile.absolutePath))
                    binding.tvTop4Points.text = players[i].points.toString()
                    binding.tvTop4Time.text = players[i].time.toString()
                }
                4 -> {
                    binding.tvTop5Name.text = players[i].name
                    val imgFile = File(players[i].imagePath)
                    if(imgFile.exists()) binding.ivTop5.setImageBitmap(BitmapFactory.decodeFile(imgFile.absolutePath))
                    binding.tvTop5Points.text = players[i].points.toString()
                    binding.tvTop5Time.text = players[i].time.toString()
                }
            }
        }
    }
}