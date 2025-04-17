package com.example.quizpirate.Controllers.MainPackage.Activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.quizpirate.Controllers.BDD.DAO.QuestionDao
import com.example.quizpirate.Controllers.BDD.DAO.UserResponseDao
import com.example.quizpirate.Controllers.BDD.Entity.QuestionWithResponses
import com.example.quizpirate.Controllers.BDD.Entity.UserResponse
import com.example.quizpirate.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class QuizActivity : BaseActivity() {
    //Variable pour les questions
    private val questionList = mutableListOf<QuestionWithResponses>()
    private var textInsert : ArrayList<String> = arrayListOf()

    private var index : Int = 0
    private var nbPoint : Int = 0
    private var nbQuesRep : Int = 0
    private val nbQuesLimit = 10
    private var nbQuestionTotal : Long = 0

    //Variable pour le XML
    private lateinit var quesTab : Array<Button>

    private lateinit var validBtn : Button
    private lateinit var timerText : TextView
    private lateinit var  timer : CountDownTimer
    private lateinit var timerBar : ProgressBar
    private var timeStart : Long = 0

    private lateinit var questionDao : QuestionDao
    private lateinit var userResponseDao : UserResponseDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_activity)
        setVideo()

        configIdAndButtons()


        questionDao = MainActivity.db.questionDao()
        userResponseDao = MainActivity.db.userReponseDao()

        // Utilisation d'une coroutine pour effectuer la requête en arrière-plan
        lifecycleScope.launch {
            nbQuestionTotal = withContext(Dispatchers.IO) {
                questionDao.countQuestionsByLanguage(MainActivity.res.configuration.locales[0].language.uppercase())
            }

            addQuestion()
            setQuestion()

            configTimer()
        }
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

    fun onValidClick(view: View) {
        // Utilisation d'une coroutine pour effectuer l'insertion en arrière-plan
        lifecycleScope.launch(Dispatchers.IO) {
            // Recherche de la réponse sélectionnée dans quesTab
            val selectedIndex = quesTab.indexOfFirst { it.isSelected }
            // Si aucune réponse n'est sélectionnée, on prendra -1 comme id de réponse
            val responseId = if (selectedIndex != -1) {
                val selectedResponse = questionList[index].responses[selectedIndex]
                // Mise à jour des points si la réponse est correcte
                if (selectedResponse.rep_bon) {
                    nbPoint += 100 / nbQuestionTotal.toInt()
                }
                selectedResponse.rep_id
            } else {
                -1
            }

            // On crée l'objet UserReponse pour l'insertion en base
            // userId et nbRetry doivent être définis dans ton activité ou récupérés via ton modèle
            val userResponse = UserResponse(
                usre_usr_id = MainActivity.user.usr_id,         // par exemple, l'id de l'utilisateur connecté
                usre_rep_id = responseId,       // -1 si aucune réponse n'a été sélectionnée
                usre_nbretry = MainActivity.user.usr_nbretry         // la tentative actuelle ou une variable définie
            )

            // Insertion dans la base via le DAO
            userResponseDao.insertUserReponse(userResponse)

            // Mise à jour de l'index et du nombre de questions répondues
            index++
            nbQuesRep++

            // Si toutes les questions ont été traitées, terminer le timer sinon passer à la question suivante
            if (index >= questionList.size) {
                timer.onFinish()
            } else {
                runOnUiThread {
                    setQuestion()// Code qui modifie l'UI ici
                }
            }
        }
    }


    private suspend fun addQuestion(excludedIds: List<Int> = emptyList()) {

        // Récupération de la langue courante (ici, on utilise la langue en majuscule)
        val currentLang = MainActivity.res.configuration.locales[0].language.uppercase()

        val questionsWithResponses = withContext(Dispatchers.IO) {
            if (excludedIds.isEmpty()) {
                questionDao.getRandomQuestions(currentLang, nbQuesLimit)
            } else {
                questionDao.getRandomQuestionsExcluding(currentLang, excludedIds, nbQuesLimit)
            }
        }
        // Ajoute les questions récupérées à ta liste
        questionsWithResponses.let { questionList.addAll(it) }

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setQuestion() {
        // Vérifie si on a dépassé le nombre de questions
        if (index >= questionList.size) {
            timer.onFinish()
            return
        }

        // Récupère la question courante
        val questionItem = questionList[index]
        findViewById<TextView>(R.id.TVQuestion).text = questionItem.question.que_name

        // Mise à jour des réponses (par exemple sur des boutons ou TextView)
        try {
            quesTab.forEach { elem ->
                val pos = quesTab.indexOf(elem)
                if (pos < questionItem.responses.size) {
                    elem.text = questionItem.responses[pos].rep_name
                    elem.isSelected = false
                    elem.background = getDrawable(R.drawable.futurist_degrad)
                }
            }
        } catch (e: Exception) {
            println(e.toString())
        }

        // Si on approche de la fin de la liste, on charge de nouvelles questions
        if ((index + (nbQuesLimit / 4)) > questionList.size) {
            lifecycleScope.launch(Dispatchers.IO) {
                // On récupère les identifiants déjà présents pour les exclure de la nouvelle requête
                val excludedIds = questionList.map { it.question.que_id }
                addQuestion(excludedIds)
            }
        }
    }

}