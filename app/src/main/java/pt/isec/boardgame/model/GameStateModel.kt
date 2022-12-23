package pt.isec.boardgame.model

import java.io.Serializable
import kotlin.random.Random

class GameStateModel(
    val random: Random,
    val operators: ArrayList<Char>,
    val itemsArray: ArrayList<Any>,
    val player: PlayerModel,
    val level: Int,
    val points: Int,
    val correctExpressions: Int,
    val lost: Boolean,
    val wrongAnswer: Boolean,
    val linesValues : ArrayList<Float>,
    val columnsValues : ArrayList<Float>,
    var mTimerRunning : Boolean,
    var mTimeLeftInMillis : Long,
    var levelTimerRunning : Boolean,
    var levelTimeLeftInMillis : Long,
    var totalTime : Long,
    var start : Long,
    var end : Long
) : Serializable