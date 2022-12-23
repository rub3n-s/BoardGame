package pt.isec.boardgame.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import pt.isec.boardgame.R
import pt.isec.boardgame.databinding.ActivityMenuBinding
import pt.isec.boardgame.model.PlayerModel
import kotlin.system.exitProcess

class MenuActivity : AppCompatActivity() {
    companion object {
        private lateinit var player: PlayerModel
        fun getPlayerIntent(context : Context, player : PlayerModel) : Intent {
            this.player = player    // Set the player's name
            return Intent(context, MenuActivity::class.java).apply { }
        }
    }

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPlay.setOnClickListener() {
            startActivity(GameActivity.getClientModeIntent(this, player))
        }

        binding.btnTop5.setOnClickListener() {
            startActivity(Intent(this, Top5Activity::class.java))
        }

        binding.btnLeave.setOnClickListener() {
            exitProcess(0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.groupId) {
            R.id.grpMenu -> {
                if (item.itemId == R.id.mnCredits) startActivity(Intent(this, CreditsActivity::class.java))
                if (item.itemId == R.id.mnProfile) startActivity(ProfileActivity.getPlayerIntent(this, player))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}