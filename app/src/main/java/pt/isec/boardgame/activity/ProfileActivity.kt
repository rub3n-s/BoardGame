package pt.isec.boardgame.activity

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import pt.isec.boardgame.PlayerActivity
import pt.isec.boardgame.R
import pt.isec.boardgame.databinding.ActivityProfileBinding
import pt.isec.boardgame.model.PlayerModel
import java.io.File

class ProfileActivity : AppCompatActivity() {
    companion object {
        private lateinit var player: PlayerModel
        fun getPlayerIntent(context : Context, player : PlayerModel) : Intent {
            this.player = player    // Set the player's name
            return Intent(context, ProfileActivity::class.java).apply { }
        }
    }

    private lateinit var binding : ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val file = File(player.imagePath)
        if (file.exists())
            binding.userImage.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        binding.etPlayerName.text = player.name
    }
}