package com.example.quizpirate.Controllers.MainPackage.Activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.quizpirate.Controllers.BDD.DAO.QuestionDao
import com.example.quizpirate.Controllers.BDD.DAO.UserResponseDao
import com.example.quizpirate.Controllers.BDD.Entity.QuestionWithResponses
import com.example.quizpirate.Controllers.BDD.Entity.UserResponse
import com.example.quizpirate.R
import com.example.quizpirate.Utils.ArcProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.quizpirate.databinding.QuizActivityBinding

@OptIn(UnstableApi::class)
class QuizActivity : BaseActivity() {
    //Variable pour les questions
    private val questionList = mutableListOf<QuestionWithResponses>()
    private var textInsert : ArrayList<String> = arrayListOf()

    private var index : Int = 0
    private var nbPoint : Int = 0
    private var nbQuesRep : Int = 0
    private val nbQuesLimit = 10
    private var nbQuestionTotal : Int = 50

    //Variable pour le XML
    private lateinit var quesTab : Array<Button>

    private lateinit var validBtn : Button
    private lateinit var  timer : CountDownTimer
    private lateinit var timerBar: ArcProgressBar
    private lateinit var questionNum: TextView
    private var timeStart : Long = 0

    private lateinit var questionDao : QuestionDao
    private lateinit var userResponseDao : UserResponseDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainBinding = QuizActivityBinding.inflate(layoutInflater)
        setContentLayout(mainBinding.root)

        configIdAndButtons()

        questionDao = MainActivity.db.questionDao()
        userResponseDao = MainActivity.db.userReponseDao()

        // Utilisation d'une coroutine pour effectuer la requête en arrière-plan
        lifecycleScope.launch {

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
        timerBar.max = MainActivity.timeTimer.toFloat()
        timerBar.value = 0f
        timer = object: CountDownTimer(MainActivity.timeTimer, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsed = MainActivity.timeTimer - millisUntilFinished
                timerBar.value = elapsed.toFloat()

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
                intent.putExtra("total", nbQuestionTotal)
                intent.putExtra("temps", "${minutes}min:${seconds}sec")

                resultLauncher.launch(intent)

            }
        }
        timeStart = System.currentTimeMillis()
        timer.start()
    }


    private fun configIdAndButtons() {
        quesTab = arrayOf(findViewById(R.id.btnAnswer1),
            findViewById(R.id.btnAnswer2),
            findViewById(R.id.btnAnswer3),
            findViewById(R.id.btnAnswer4))

        questionNum = findViewById(R.id.nbQuestion)

        validBtn = findViewById(R.id.btnValidateQuiz)
        timerBar = findViewById(R.id.arcProgressBar)
        timerBar.shipScale = 0.1f
        timerBar.chaserScale = 0.1f
        timerBar.torpedoScale = 0.1f
        timerBar.alignToTangent = true
        timerBar.shipAheadPx = 8f


        // 🎯 Chargement des images
        timerBar.setShipResource(MainActivity.vaisseauChoice)      // vaisseau poursuivi (la cible)
        timerBar.setChaserResource(R.drawable.polaris)  // vaisseau poursuivant
        timerBar.setTorpedoResource(R.drawable.torpille)


        timerBar.spriteGlobalRatio = 1.2f // grossir tous les sprites
        timerBar.scaleByStrokeWidth = true

        // petite helper si tu n’as pas l’extension FloatRange.random()
        fun rand(minF: Float, maxF: Float) = Random.nextFloat() * (maxF - minF) + minF

        timerBar.chaserSpeed  = rand(0.60f, 0.90f)
        timerBar.chaserLagPct = rand(0.18f, 0.30f)   // lag un peu plus grand = bien derrière

        val gapMin = 0.15f   // 8% de l’arc minimum
        val gapMax = 0.20f   // jusqu’à 15% possible
        val gap    = rand(gapMin, gapMax)

        val torpLagMax = (timerBar.chaserLagPct - 0.05f).coerceAtLeast(0f)
        val torpLagMin = (timerBar.chaserLagPct - gap).coerceAtLeast(0f)

        val lo = min(torpLagMin, torpLagMax)
        val hi = maxOf(torpLagMin, torpLagMax)

        timerBar.torpedoLagPct = rand(lo, hi)  // => 0 ≤ torpedoLagPct < chaserLagPct - 0.05f
        val speedDeltaMin = 0.10f
        val speedDeltaMax = 0.20f
        val torpMinSpeed  = (timerBar.chaserSpeed + speedDeltaMin)
        val torpMaxSpeed  = min(0.96f, timerBar.chaserSpeed + speedDeltaMax)

        timerBar.torpedoSpeed = rand(torpMinSpeed, torpMaxSpeed)
        // Apparition en douceur
        timerBar.chaserEntryDistancePx = 140f
        timerBar.chaserAppearRangePct = 0.14f
        timerBar.torpedoEntryDistancePx = 140f
        timerBar.torpedoAppearRangePct = 0.10f



        validBtn.text = MainActivity.res.getString(R.string.Texte_ButtonValider)
    }

    fun onRepClick(it: View) {
        quesTab.forEach {
                elem ->
            if(it != elem) {
                elem.isSelected = false
            }
        }
        it.isSelected = true
    }

    fun onValidClick(view: View) {
        // Utilisation d'une coroutine pour effectuer l'insertion en arrière-plan
        lifecycleScope.launch(Dispatchers.IO) {
            // Recherche de la réponse sélectionnée dans quesTab
            val selectedIndex = quesTab.indexOfFirst { it.isSelected }
            quesTab.forEach { it.isSelected = false }

            // Si aucune réponse n'est sélectionnée, on prendra -1 comme id de réponse
            val responseId = if (selectedIndex != -1) {
                val selectedResponse = questionList[index].responses[selectedIndex]
                // Mise à jour des points si la réponse est correcte
                if (selectedResponse.rep_bon) {
                    nbPoint += 100 / nbQuestionTotal.toInt()
                    nbQuesRep++
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
            // Limite le nombre total de questions à nbQuestionTotal
            val nbRestant = (nbQuestionTotal - questionList.size).toInt().coerceAtLeast(0)
            val questionsToAdd = if (questionsWithResponses.size > nbRestant) {
             questionsWithResponses.take(nbRestant)
            } else {
             questionsWithResponses
            }
        questionList.addAll(questionsToAdd)
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
        findViewById<TextView>(R.id.textQuestion).text = questionItem.question.que_name
        questionNum.text = buildString {
            append((index + 1).toString())
            append("/")
            append(nbQuestionTotal.toString())
        }

        // Mise à jour des réponses (par exemple sur des boutons ou TextView)
        try {
            quesTab.forEach { elem ->
                val pos = quesTab.indexOf(elem)
                if (pos < questionItem.responses.size) {
                    elem.text = questionItem.responses[pos].rep_name
                    elem.isSelected = false
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