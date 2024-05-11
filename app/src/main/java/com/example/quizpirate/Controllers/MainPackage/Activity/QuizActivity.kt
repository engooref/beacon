package com.example.quizpirate.Controllers.MainPackage.Activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.quizpirate.Models.BddClass.Companion
import com.example.quizpirate.R


class QuizActivity : BaseActivity() {
    //Variable pour les questions
    private var question: Array<Companion.Question> = arrayOf()
    private var textInsert : ArrayList<String> = arrayListOf()

    private var index : Int = 0
    private var nbPoint : Int = 0
    private var nbQuesRep : Int = 0
    private val nbQuesLimit = 10
    private var nbQuestionTotal = 0

    //Variable pour le XML
    private lateinit var quesTab : Array<Button>

    private lateinit var validBtn : Button
    private lateinit var timerText : TextView
    private lateinit var  timer : CountDownTimer
    private lateinit var timerBar : ProgressBar
    private var timeStart : Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_activity)
        setVideo()

        configIdAndButtons()

        val text = "select count(*) as nbQues from ${Companion.TABLE.QUESTION.TABLE_NAME} " +
                " WHERE ${Companion.TABLE.QUESTION.LANGUE} = '${MainActivity.res.configuration.locales[0].language.uppercase()}'"

        MainActivity.bdd!!.select(text) { rs ->
            nbQuestionTotal = rs.getInt("nbQues")
        }

        addQuestion()
        setQuestion()

        configTimer()

    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            this@QuizActivity.finish()
        }
    }

    private fun configTimer() {
        timerBar.max = MainActivity.timeTimer.toInt()
        timerBar.progress = 1000
        timer = object: CountDownTimer(MainActivity.timeTimer, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000 % 60)

                timerBar.progress += 1000
                timerText.text = "$minutes min: ${ if (seconds < 10 ) "0" + seconds else seconds}"
            }

            override fun onFinish() {
                validBtn.isClickable = false
                val timeEnd = System.currentTimeMillis()

                val differenceInMillis = timeEnd - timeStart
                val minutes = differenceInMillis / 1000 / 60
                val seconds = (differenceInMillis / 1000 % 60)

                val intent = Intent(this@QuizActivity, FinishActivity::class.java)
                intent.putExtra("point", nbPoint)
                intent.putExtra("question", nbQuesRep)
                intent.putExtra("temps", "${minutes}min:${seconds}sec")

                timer.cancel()
                intent.putExtra("sqlText", "insert into ${Companion.TABLE.USER_REPONSE.TABLE_NAME} " +
                        "(${Companion.TABLE.USER_REPONSE.USER_ID}, ${Companion.TABLE.USER_REPONSE.REPONSE_ID}, ${Companion.TABLE.USER_REPONSE.USER_NBRETRY}) VALUES " +
                        textInsert.joinToString()
                )
                resultLauncher.launch(intent)

            }
        }
        timeStart = System.currentTimeMillis()
        timer.start()
    }



    private fun configIdAndButtons() {
        quesTab = arrayOf(findViewById(R.id.btnRep1),
            findViewById(R.id.btnRep2),
            findViewById(R.id.btnRep3),
            findViewById(R.id.btnRep4))

        validBtn = findViewById(R.id.BtnValid)
        timerText = findViewById(R.id.TVTimer)
        timerBar = findViewById(R.id.timerBar)

        validBtn.text = MainActivity.res.getString(R.string.Texte_ButtonValider)
    }

    fun onRepClick(it: View) {
        quesTab.forEach {
                elem ->
            if(it != elem) {
                elem.isSelected = false
                elem.background = getDrawable(R.drawable.futurist_degrad)
            }
        }
        it.isSelected = true
        it.background = getDrawable(R.drawable.futurist_degrad_select)
    }

    fun onValidClick(view : View) {
        var sqlText = ""
        quesTab.forEach { elem ->
            if (elem.isSelected) {
                sqlText = "(userId, ${question[index].responses[quesTab.indexOf(elem)].id}, nbRetry) "

                if (question[index].responses[quesTab.indexOf(elem)].good)  { nbPoint += 100 / nbQuestionTotal; }
            }
        }

        if (sqlText.isEmpty())
            sqlText = "(userId, -1, nbRetry) "

        textInsert += sqlText
        index++
        nbQuesRep++

        if(index >= question.size)
            timer.onFinish()
        else
            setQuestion()
    }

    private fun addQuestion(id : ArrayList<String> = arrayListOf()) {
        val text = "select * from ${Companion.TABLE.QUESTION.TABLE_NAME} " +
                " WHERE ${Companion.TABLE.QUESTION.LANGUE} = '${MainActivity.res.configuration.locales[0].language.uppercase()}' " +
                if (id.isNotEmpty()) {
                     " and ${Companion.TABLE.QUESTION.ID} not in ${
                        id.joinToString(
                            ",",
                            "(",
                            ")"
                        )
                    } "
                } else "" +
                "ORDER BY RAND() Limit $nbQuesLimit"

        MainActivity.bdd!!.select(text) { rs ->
            val ques = Companion.Question(
                rs.getInt(Companion.TABLE.QUESTION.ID),
                rs.getString(Companion.TABLE.QUESTION.NAME),
                arrayOf(),
                rs.getString(Companion.TABLE.QUESTION.LANGUE)
            )

            MainActivity.bdd!!.select("select * from ${Companion.TABLE.REPONSE.TABLE_NAME} where ${Companion.TABLE.REPONSE.QUES_ID} = ${ques.id} ORDER BY RAND()") {
                ques.responses += Companion.Response(
                    it.getInt(Companion.TABLE.REPONSE.ID),
                    it.getString(Companion.TABLE.REPONSE.NAME),
                    it.getString(Companion.TABLE.REPONSE.BON) == "O"
                )
            }
            question += ques
        }
    }

    private fun setQuestion() {
        if(index >= question.size)
            timer.onFinish()

        val questionItem = question[index]
        findViewById<TextView>(R.id.TVQuestion).text = questionItem.name
        try {
            quesTab.forEach { elem ->
                elem.text = questionItem.responses[quesTab.indexOf(elem)].name
                elem.isSelected = false
                elem.background = getDrawable(R.drawable.futurist_degrad)
            }
        } catch (e: Exception) {
            println(e.toString())
        }


        if ((index + (nbQuesLimit / 4)) > question.size) {
            Thread {
                var questionId: ArrayList<String> = arrayListOf()
                question.forEach { questionId.add(it.id.toString()) }
                addQuestion(questionId)
            }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivity.esp32?.sendMessage("4")
    }

}