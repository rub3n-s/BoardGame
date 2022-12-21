package pt.isec.boardgame

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.widget.Button
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.abs
import kotlin.random.Random

//  SHA1: 37:46:E1:CC:7C:AA:AD:2B:AA:6A:AD:F3:18:AB:01:C9:81:7C:A1:8F

class GameActivity : AppCompatActivity() {
    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100

        private var PLAYER_NAME : String = ""
        private const val COLUMNS = 5
        private const val LINES = 5
        private const val START_TIME_IN_MILLIS : Long = 5000

        //private const val SERVER_MODE = 0
        private const val CLIENT_MODE = 1

        /*fun getServerModeIntent(context : Context, playerName : String) : Intent {
            return Intent(context,GameActivity::class.java).apply {
                PLAYER_NAME = playerName    // Set the player's name
                putExtra("mode", SERVER_MODE)
            }
        } */

        fun getClientModeIntent(context : Context, playerName : String) : Intent {
            PLAYER_NAME = playerName    // Set the player's name
            return Intent(context,GameActivity::class.java).apply {
                putExtra("mode", CLIENT_MODE)
            }
        }
    }

    private lateinit var auth: FirebaseAuth

    private var random = Random(123456789L)
    private var minInterval = 0
    private var maxInterval = 0
    private var levelSeconds : Long = 0
    private var operators : ArrayList<Char> = ArrayList()
    private var level = 1
    private var points = 0
    private var bonus : Long = 0
    private var minRightExpressions = 0
    private var rightExpressions = 0
    private var lost = false

    // GridView Data
    private var itemsArray : ArrayList<Any> = ArrayList()
    private lateinit var itemsGV : GridView
    private lateinit var itemGVAdapter : GridViewAdapter
    private lateinit var detector: GestureDetectorCompat

    private val linesValues : ArrayList<Float> = ArrayList()
    private val columnsValues : ArrayList<Float> = ArrayList()

    // alertDialog CountDownTimer
    private lateinit var alertDialog: AlertDialog
    private lateinit var alertCountDownTimer : CountDownTimer
    private lateinit var buttonPauseStart : Button
    private var mTimerRunning = false
    private var mTimeLeftInMillis = START_TIME_IN_MILLIS

    // Level Time Limit CountDownTimer
    private lateinit var levelCountDownTimer : CountDownTimer
    private var levelTimerRunning = false
    private var levelTimeLeftInMillis : Long = 0

    // Level Loss CountDownTimer
    private lateinit var buttonLossOk : Button
    private lateinit var alertLossCountDownTimer : CountDownTimer

    // Total played time
    private var totalTime : Long = 0
    private var start : Long = 0
    private var end : Long = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        auth = Firebase.auth

        // Initialize GridView
        itemsGV = findViewById(R.id.gvItems)

        // Define the initial game values
        defineValues()

        // GridView Gesture Listener
        detector = GestureDetectorCompat(this, MyGestureListener())
        itemsGV.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }

        // Get start time
        start = System.currentTimeMillis()

        // Lister for End Game Button click
        findViewById<Button>(R.id.btnEndGame).setOnClickListener {
            totalTime = totalTimeCounter()

            // Stop level limit timer
            if (levelTimerRunning) levelCountDownTimer.cancel()

            //  Firebase update
            updateTop5()

            // Finish this activity
            finish()

            // Start main activity (Main Activity)
            startActivity(Intent(this,MainActivity::class.java))
        }

        // Start the first level timer
        startLevelTimer()

        // Remove all data from Firestore
        //for (i in 1..5) removeDataFromFirestore(i)

        // Add data to Firestore
        //for (i in 1..5) addDataToFirestore(i)
    }

    // Minimize this activity instead of going back so PlayerActivity
    @Deprecated("Deprecated in Java", ReplaceWith("moveTaskToBack(true)"))
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun nextLevel() {
        level++
        defineValues()
    }

    @SuppressLint("SetTextI18n")
    private fun defineValues() {
        when (level) {
            1 -> {
                minInterval = 0
                maxInterval = 9
                levelSeconds = 70000
                //levelSeconds = 10000
                bonus = 3000
                operators.add('+')
                minRightExpressions = 5
            }
            2 -> {
                minInterval = 0
                maxInterval = 99
                //levelSeconds = 60000
                levelSeconds = 20000
                bonus = 5000
                operators.add('-')
                minRightExpressions = 4
            }
            3 -> {
                minInterval = 0
                maxInterval = 999
                //levelSeconds = 50000
                levelSeconds = 20000
                bonus = 7000
                operators.add('*')
                minRightExpressions = 3
            }
            4 -> {
                minInterval = 0
                maxInterval = 9999
                //levelSeconds = 40000
                levelSeconds = 20000
                bonus = 10000
                operators.add('/')
                minRightExpressions = 2
            }
        }

        // Define the level time limit
        levelTimeLeftInMillis = levelSeconds

        // Fill array with new values
        fillArray()

        // Display data in TextViews
        findViewById<TextView>(R.id.tvTimeLeft).text = ""
        findViewById<TextView>(R.id.tvPlayerName).text = PLAYER_NAME
        findViewById<TextView>(R.id.tvPoints).text = points.toString()
        findViewById<TextView>(R.id.tvLevel).text = level.toString()
    }

    private fun fillArray() {
        // Clear GridView data
        itemsArray.clear()
        linesValues.clear()
        columnsValues.clear()

        // Create temporary variables to store random values
        var tmp : Any
        val tmpArray : ArrayList<Any> = ArrayList()

        for (i in 0 until LINES) {
            for (j in 0 until COLUMNS) {
                // 3 numbers and 2 operators
                tmp = if (i % 2 == 0) {
                    if (j % 2 == 0)
                        randomNumber()  // numbers
                    else
                        randomOperator()    // operators
                }
                // 3 operators and 2 empty cells
                else {
                    if (j % 2 == 0)
                        randomOperator()
                    else
                        ""
                }
                itemsArray.add(tmp)
                tmpArray.add(tmp)
            }
            // Calculate lines
            if (i == 1 || i == 3)
                linesValues.add(-1.0f)
            else
                linesValues.add(calculate(tmpArray))
            tmpArray.clear()
        }

        // Calculate columns
        tmpArray.clear()
        var column = 0
        var counter = 0
        while (column < 5) {
            for (i in 0 until LINES) {
                for (j in 0 until COLUMNS) {
                    if (j == column) {
                        tmpArray.add(itemsArray[counter])
                    }
                    counter++
                }
            }
            if (column == 1 || column == 3)
                columnsValues.add(-1.0f)
            else
                columnsValues.add(calculate(tmpArray))
            tmpArray.clear()
            column++
            counter = 0
        }

        // Insert new data into GridView
        itemGVAdapter = GridViewAdapter(itemsArray,this@GameActivity)

        // Set adapter to GridView
        itemsGV.adapter = itemGVAdapter

        for (m in 0 until linesValues.size) Log.i(TAG, "fillArray: line[$m] = ${linesValues[m]}")
        for (n in 0 until columnsValues.size) Log.i(TAG, "fillArray: column[$n] = ${columnsValues[n]}")
    }

    private fun randomNumber() : Int {
        //return random.nextInt(minInterval,maxInterval)
        return random.nextInt(maxInterval - minInterval + 1) + minInterval
    }

    private fun randomOperator() : Char {
        if (operators.size == 1)
            return operators[0]
        return operators[random.nextInt(0,operators.size)]
    }

    // ==================================================
    //              GridView Gesture Listener
    // ==================================================
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Call onTouchEvent
        detector.onTouchEvent(event!!)
        return super.onTouchEvent(event)
    }

    inner class MyGestureListener : SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean {
            return true
        }
        override fun onFling(
            event1: MotionEvent, event2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            val diffY = event2.y - event1.y
            val diffX = event2.x - event1.x
            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        if (!lost) onSwipeRight(event1.y,event2.y)
                    } else {
                        if (!lost) onSwipeLeft(event1.y,event2.y)
                    }
                }
            } else {
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        if (!lost) onSwipeBottom(event1.x,event2.x)
                    } else {
                        if (!lost) onSwipeTop(event1.x,event2.x)
                    }
                }
            }
            return true
        }
    }

    private fun onSwipeLeft(pressY : Float, releaseY : Float) {
        Log.i(TAG, "onSwipeLeft: ${pressY.toInt()},${releaseY.toInt()}")
        evaluateLine(pressY,releaseY)
    }

    private fun onSwipeRight(pressY : Float, releaseY : Float) {
        Log.i(TAG, "onSwipeRight: ${pressY.toInt()},${releaseY.toInt()}")
        evaluateLine(pressY,releaseY)
    }

    private fun onSwipeTop(pressX : Float, releaseX : Float) {
        Log.i(TAG, "onSwipeTop: ${pressX.toInt()},${releaseX.toInt()}")
        evaluateColumn(pressX,releaseX)
    }

    private fun onSwipeBottom(pressX : Float, releaseX : Float) {
        Log.i(TAG, "onSwipeBottom: ${pressX.toInt()},${releaseX.toInt()}")
        evaluateColumn(pressX,releaseX)
    }

    private fun evaluateLine(pressY : Float, releaseY: Float) {
        // Get lines limits (for swipe action)
        val lineHeight = itemsGV.height/5
        var swipedLine = -1
        val linesLimits : ArrayList<Int> = ArrayList()

        // clear lines limits array
        linesLimits.clear()

        for (i in 0 until LINES) {
            if (i == 0)
                linesLimits.add(lineHeight)
            else
                linesLimits.add(linesLimits[i-1] + lineHeight)
            Log.i(TAG, "evaluateLine: Line[$i] = ${linesLimits[i]}")
        }

        // Check swipe action position
        for (i in 0 until LINES) {
            if (i == 0 && pressY >= 0 && pressY <= linesLimits[i] &&
                releaseY >= 0 && releaseY <= linesLimits[i])
                    swipedLine = 0
            else if (i > 0 && pressY >= linesLimits[i-1] && pressY <= linesLimits[i] &&
                releaseY >= linesLimits[i-1] && releaseY <= linesLimits[i])
                swipedLine = i
        }

        if (swipedLine > -1) {
            var message: String

            // Verify if is the highest value or the second highest
            val tmpPoints = getPoints(linesValues[swipedLine])

            // Get cells position
            val selectedPositions = getLineCellsPosition(swipedLine)

            // If the client gets a right expression
            if (tmpPoints > 0) {
                // Highlight the swiped cells (Color: BLUE = right answer)
                itemGVAdapter.selectedPositions(selectedPositions, true)

                // Notify that data has been change in the adapter class
                itemGVAdapter.notifyDataSetChanged()

                // Cancel level timer
                levelCountDownTimer.cancel()

                // Add bonus to timer
                levelTimeLeftInMillis += bonus
                if (levelTimeLeftInMillis > levelSeconds) levelTimeLeftInMillis = levelSeconds

                // Increase right answers
                rightExpressions++

                // Increase points counter and update points TextView
                points += tmpPoints

                Handler(Looper.getMainLooper()).postDelayed({
                    findViewById<TextView>(R.id.tvPoints).text = points.toString()

                    // Display message
                    Log.i(TAG, "swipedLine: $swipedLine")
                    message = "swipedLine[$swipedLine] = ${linesValues[swipedLine]} "
                    if (tmpPoints == 2)
                        message += ". Highest value"
                    else if (tmpPoints == 1)
                        message += ". Second highest value"

                    // Fill the array again with random values
                    fillArray()

                    // Start the current level timer
                    startLevelTimer()

                    // Display message
                    Toast.makeText(
                        applicationContext,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }, 1000)
            }
            else {
                // Highlight the swiped cells (Color: RED = wrong answer)
                itemGVAdapter.selectedPositions(selectedPositions, false)

                // Notify that data has been change in the adapter class
                itemGVAdapter.notifyDataSetChanged()

                // Set lost variable to true, prevents further GridView touch actions
                lost = true

                Handler(Looper.getMainLooper()).postDelayed({
                    // Clear the painted positions on the GridView Adapter
                    //itemGVAdapter.clearSelectedPositions()

                    // Notify that data has been change in the adapter class
                    //itemGVAdapter.notifyDataSetChanged()

                    // Cancel level timer
                    levelCountDownTimer.cancel()

                    // Display loss dialog
                    displayLossAlertDialog()

                    // Display invalid answer message
                    Log.i(TAG, "swipedLine: $swipedLine")
                    message = "swipedLine[$swipedLine] = invalid answer"

                    // Display message
                    Toast.makeText(
                        applicationContext,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }, 1500)
            }
        }
    }

    private fun evaluateColumn(pressX : Float, releaseX: Float) {
        // Get columns limits (for swipe action)
        val columnWidth = itemsGV.width/5
        var swipedColumn = -1
        val columnsLimits : ArrayList<Int> = ArrayList()

        // clear columns limits array
        columnsLimits.clear()

        for (i in 0 until COLUMNS) {
            if (i == 0)
                columnsLimits.add(columnWidth)
            else
                columnsLimits.add(columnsLimits[i-1] + columnWidth)
            Log.i(TAG, "evaluateColumn: Column[$i] = ${columnsLimits[i]}")
        }

        // Check swipe action position
        for (i in 0 until COLUMNS) {
            if (i == 0 && pressX >= 0 && pressX <= columnsLimits[i] &&
                releaseX >= 0 && releaseX <= columnsLimits[i])
                swipedColumn = 0
            else if (i > 0 && pressX >= columnsLimits[i-1] && pressX <= columnsLimits[i] &&
                releaseX >= columnsLimits[i-1] && releaseX <= columnsLimits[i])
                swipedColumn = i
        }

        if (swipedColumn > -1) {
            var message: String

            // Verify if is the highest value or the second highest
            val tmpPoints = getPoints(columnsValues[swipedColumn])

            // Get cells position
            val selectedPositions = getColumnCellsPosition(swipedColumn)

            if (tmpPoints > 0) {
                // Highlight the swiped cells (Color: BLUE = right answer)
                itemGVAdapter.selectedPositions(selectedPositions, true)

                // Notify that data has been change in the adapter class
                itemGVAdapter.notifyDataSetChanged()

                // Cancel level timer
                levelCountDownTimer.cancel()

                // Add bonus to timer
                levelTimeLeftInMillis += bonus
                if (levelTimeLeftInMillis > levelSeconds) levelTimeLeftInMillis = levelSeconds

                // Increase right answers
                rightExpressions++

                // Increase points counter and update points TextView
                points += tmpPoints

                Handler(Looper.getMainLooper()).postDelayed({
                    findViewById<TextView>(R.id.tvPoints).text = points.toString()

                    // Display message
                    Log.i(TAG, "swipedColumn: $swipedColumn")
                    message = "swipedColumn[$swipedColumn] = ${columnsValues[swipedColumn]} "
                    if (tmpPoints == 2)
                        message += ". Highest value"
                    else if (tmpPoints == 1)
                        message += ". Second highest value"

                    // Fill the array again with random values
                    fillArray()

                    // Start the current level timer
                    startLevelTimer()

                    // Display message
                    Toast.makeText(
                        applicationContext,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }, 1000)
            } else {
                // Highlight the swiped cells (Color: RED = wrong answer)
                itemGVAdapter.selectedPositions(selectedPositions, false)

                // Notify that data has been change in the adapter class
                itemGVAdapter.notifyDataSetChanged()

                // Set lost variable to true, prevents further GridView touch actions
                lost = true

                Handler(Looper.getMainLooper()).postDelayed({
                    // Clear the painted positions on the GridView Adapter
                    //itemGVAdapter.clearSelectedPositions()

                    // Notify that data has been change in the adapter class
                    //itemGVAdapter.notifyDataSetChanged()

                    // Cancel level timer
                    levelCountDownTimer.cancel()

                    // Display loss dialog
                    displayLossAlertDialog()

                    // Display invalid answer message
                    Log.i(TAG, "swipedColumn: $swipedColumn")
                    message = "swipedColumn[$swipedColumn] = invalid answer"

                    // Display message
                    Toast.makeText(
                        applicationContext,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }, 1500)
            }
        }
    }

    /*private fun recalcLineValues() {
        val tmpArray : ArrayList<Any> = ArrayList()
        var counter = 0
        linesValues.clear()
        for (i in 0 until LINES) {
            for (j in 0 until COLUMNS) {
                tmpArray.add(itemsArray[counter])
                counter++
            }
            // Calculate line
            if (i == 1 || i == 3)
                linesValues.add(-1.0f)
            else
                linesValues.add(calculate(tmpArray))
            tmpArray.clear()
        }

        // Insert new data into GridView
        itemGVAdapter = GridViewAdapter(itemsArray,this@GameActivity)

        // Set adapter to GridView
        itemsGV.adapter = itemGVAdapter
    }

    private fun recalcColumnValues() {
        val tmpArray : ArrayList<Any> = ArrayList()
        var column = 0
        var counter = 0
        columnsValues.clear()
        while (column < 5) {
            for (i in 0 until LINES) {
                for (j in 0 until COLUMNS) {
                    if (j == column) {
                        tmpArray.add(itemsArray[counter])
                    }
                    counter++
                }
            }
            // Calculate column
            if (column == 1 || column == 3)
                columnsValues.add(-1.0f)
            else
                columnsValues.add(calculate(tmpArray))
            tmpArray.clear()
            column++
            counter = 0
        }

        // Insert new data into GridView
        itemGVAdapter = GridViewAdapter(itemsArray,this@GameActivity)

        // Set adapter to GridView
        itemsGV.adapter = itemGVAdapter
    } */

    private fun getPoints(value: Float) : Int {
        // Create a tmp array
        var descendingValues : ArrayList<Float> = ArrayList()

        // Add values from columns and lines
        for (column in columnsValues) descendingValues.add(column)
        for (line in linesValues) descendingValues.add(line)

        // Order Descending
        descendingValues.sortDescending()

        // Remove repeated values (if they exist)
        descendingValues = descendingValues.distinct() as ArrayList<Float>
        //descendingValues.toSet().toList()

        // Verify if is the highest
        if (value == descendingValues[0])
            return 2    // 2 points for the highest
        else if (value == descendingValues[1])
            return 1    // 1 points for the second highest highest
        return 0
    }

    private fun getColumnCellsPosition(column : Int) : ArrayList<Int> {
        val selectedPositions : ArrayList<Int> =  ArrayList()
        var counter = 0
        for (i in 0 until LINES) {
            for (j in 0 until COLUMNS) {
                if (j == column)
                    selectedPositions.add(counter)
                counter++
            }
        }
        return selectedPositions
    }

    private fun getLineCellsPosition(line : Int) : ArrayList<Int> {
        val selectedPositions : ArrayList<Int> =  ArrayList()
        var counter = 0
        for (i in 0 until LINES) {
            for (j in 0 until COLUMNS) {
                if (i == line)
                    selectedPositions.add(counter)
                counter++
            }
        }
        return selectedPositions
    }

    private fun calculate(array : ArrayList<Any>) : Float{
        var calc = 0.0f

        // Operator conditions
        val op1 = array[1] as Char
        val op2 = array[3] as Char
        val op1Priority = hasPriority(op1)
        val op2Priority = hasPriority(op2)

        // Order doesnt matter
        if (op1Priority && op2Priority) {
            when (op1) {
                '*' -> calc = (array[0] as Int * array[2] as Int).toFloat()
                '/' -> calc = (array[0] as Int / array[2] as Int).toFloat()
            }
            when (op2) {
                '*' -> calc *= (array[4] as Int).toFloat()
                '/' -> calc /= (array[4] as Int).toFloat()
            }
            return calc
        }

        // Only the first operator has priority
        if (op1Priority) {
            when (op1) {
                '*' -> calc = (array[0] as Int * array[2] as Int).toFloat()
                '/' -> calc = (array[0] as Int / array[2] as Int).toFloat()
            }
            when (op2) {
                '+' -> calc += (array[4] as Int).toFloat()
                '-' -> calc -= (array[4] as Int).toFloat()
            }
            return calc
        }

        // Only the second operator has priority
        if (op2Priority) {
            when (op2) {
                '*' -> calc = (array[2] as Int * array[4] as Int).toFloat()
                '/' -> calc = (array[2] as Int / array[4] as Int).toFloat()
            }
            when (op1) {
                '+' -> calc += (array[0] as Int).toFloat()
                '-' -> calc -= (array[0] as Int).toFloat()
            }
            return calc
        }

        // If none of the operators has priority
        when (op1) {
            '+' -> calc = (array[0] as Int + array[2] as Int).toFloat()
            '-' -> calc = (array[0] as Int - array[2] as Int).toFloat()
        }
        when (op2) {
            '+' -> calc += (array[4] as Int).toFloat()
            '-' -> calc -= (array[4] as Int).toFloat()
        }
        return calc
    }

    private fun hasPriority(op : Char) : Boolean {
        return when(op) {
            '*','/' -> true
            else -> false
        }
    }

    // ===================================================
    //                Loss Alert Dialog
    // ===================================================
    private fun displayLossAlertDialog() {
        alertDialog = AlertDialog.Builder(this)
            .setMessage("You lost")
            .setPositiveButton("Ok",null).create()

        alertDialog.setOnShowListener {
            // Initialize button
            buttonLossOk = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

            buttonLossOk.setOnClickListener {
                alertLossCountDownTimer
            }

            startLossTimer()
        }

        alertDialog.show()
    }

    private fun startLossTimer() {
        alertLossCountDownTimer = object : CountDownTimer(5000,1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished : Long) {
                buttonLossOk.text = "Ok (${millisUntilFinished / 1000 + 1})"
            }

            override fun onFinish() {
                alertDialog.dismiss()
            }
        }.start()
    }

    // ===================================================
    //       Transition Alert Dialog CountDownTimer
    // ===================================================
    private fun displayAlertDialog() {
        alertDialog = AlertDialog.Builder(this)
            .setMessage("Transitioning to next level")
            .setPositiveButton("yes",null).create()

        alertDialog.setOnShowListener {
            // Initialize button
            buttonPauseStart = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

            // AlertDialog pause/start functionality (level transition)
            buttonPauseStart.setOnClickListener {
                if (mTimerRunning)
                    pauseTimer()
                else
                    startTimer()
            }

            startTimer()
        }

        alertDialog.show()
    }

    private fun startTimer() {
        alertCountDownTimer = object : CountDownTimer(mTimeLeftInMillis,1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished : Long) {
                // var mTimeLeftInMillis: stores current seconds for having access to them if
                // the user pauses the timer
                mTimeLeftInMillis = millisUntilFinished
                buttonPauseStart.text = "Pause (${millisUntilFinished / 1000 + 1})"
            }

            override fun onFinish() {
                mTimerRunning = false
                mTimeLeftInMillis = START_TIME_IN_MILLIS
                alertDialog.dismiss()

                // Start the current level timer
                startLevelTimer()
            }
        }.start()

        mTimerRunning = true
    }

    private fun pauseTimer() {
        alertCountDownTimer.cancel()
        mTimerRunning = false
    }

    // ===================================================
    //          Level Time Limit CountDownTimer
    // ===================================================
    private fun startLevelTimer() {
        levelCountDownTimer = object : CountDownTimer(levelTimeLeftInMillis,1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished : Long) {
                findViewById<TextView>(R.id.tvTimeLeft).text = "${millisUntilFinished / 1000 + 1}"
                levelTimerRunning = true
                levelTimeLeftInMillis = millisUntilFinished
            }

            override fun onFinish() {
                findViewById<TextView>(R.id.tvTimeLeft).text = "0"
                levelTimerRunning = false

                // If the client don't get the minimum right expressions, go back go level 1
                if (rightExpressions < minRightExpressions) {
                    level = 1
                    defineValues()
                } else {
                    // Set the new level values
                    nextLevel()

                    // Display Transition Alert
                    displayAlertDialog()
                }
            }
        }.start()
    }

    // ===================================================
    //              Total Time Counter
    // ===================================================
    private fun totalTimeCounter() : Long {
        end = System.currentTimeMillis()
        return (end - start) / 1000 + 1
    }

    // ===================================================
    //                     Top 5
    // ===================================================
    private fun updateTop5() {
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
                Log.i(TAG, "updateTop5: name=$name,points=$points,level=$level,time=$time")
            }
            //call a function to work with your array
            orderTop5(dataSets)
        }
    }

    private fun orderTop5(players : ArrayList<PlayerModel>) {
        // Add this player to the array
        players.add(PlayerModel(PLAYER_NAME,points,level,totalTime))

        // Sort Descending from higher points to lower
        players.sortByDescending { playerModel -> playerModel.points }

        // Because at this point the array will have 6 players
        // We remove the last index
        players.removeAt(players.lastIndex)

        for (i in 1..5) updateDataInFirestoreTrans(i,players[i-1])
    }

    // ===================================================
    //              FireBase Functions
    // ===================================================
    private fun addDataToFirestore(position : Int) {
        val db = Firebase.firestore
        val scores = hashMapOf(
            "player" to "",
            "level" to 0,
            "points" to 0,
            "time" to 0
        )
        db.collection("Top5").document(position.toString()).set(scores)
            .addOnSuccessListener {
                Log.i(TAG, "addDataToFirestore: Success")
            }.
            addOnFailureListener { e->
                Log.i(TAG, "addDataToFirestore: ${e.message}")
            }
    }

    private fun updateDataInFirestoreTrans(position: Int, player: PlayerModel) {
        val db = Firebase.firestore
        val v = db.collection("Top5").document(position.toString())
        db.runTransaction { transaction ->
            val doc = transaction.get(v)
            if (doc.exists()) {
                val name = player.name
                val points = player.points
                val level = player.level
                val time = player.time
                transaction.update(v, "player", name)
                transaction.update(v, "points", points)
                transaction.update(v, "level", level)
                transaction.update(v, "time", time)
                null
            } else
                throw FirebaseFirestoreException(
                    "Error",
                    FirebaseFirestoreException.Code.UNAVAILABLE
                )
        }.addOnSuccessListener {
            Log.i(TAG, "updateDataInFirestoreTrans: Success")
        }.addOnFailureListener { e ->
            Log.i(TAG, "updateDataInFirestoreTrans: ${e.message}")
        }
    }

    private fun removeDataFromFirestore(position : Int) {
        val db = Firebase.firestore
        val v = db.collection("Top5").document(position.toString())
        v.delete()
            .addOnSuccessListener {
                Log.i(TAG, "removeDataFromFirestore: Success")
            }
            .addOnFailureListener { e ->
                Log.i(TAG, "removeDataFromFirestore: ${e.message}")
            }
    }
}