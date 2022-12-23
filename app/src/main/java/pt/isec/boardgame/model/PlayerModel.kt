package pt.isec.boardgame.model

import java.io.Serializable

class PlayerModel(
    val imagePath : String,
    val name: String,
    val points: Int,
    val level: Int,
    val time: Long
) : Serializable