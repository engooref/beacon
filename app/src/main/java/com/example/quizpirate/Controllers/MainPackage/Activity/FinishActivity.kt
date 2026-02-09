package com.example.quizpirate.Controllers.MainPackage.Activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.example.quizpirate.Controllers.BDD.DAO.UserDao
import com.example.quizpirate.Controllers.BDD.DAO.UserResponseDao
import com.example.quizpirate.Controllers.BDD.DAO.UserTempsDao
import com.example.quizpirate.Controllers.BDD.Entity.UserTemps
import com.example.quizpirate.R
import com.example.quizpirate.databinding.ActivityFinishBinding
import com.example.quizpirate.databinding.QuizActivityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class FinishActivity : BaseActivity() {

    private lateinit var  handle : TextView
    private lateinit var valid : Button

    private lateinit var userDao: UserDao
    private lateinit var userResponseDao: UserResponseDao
    private lateinit var userTempsDao : UserTempsDao
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainBinding = ActivityFinishBinding.inflate(layoutInflater)
        setContentLayout(mainBinding.root)

        valid = findViewById(R.id.btnValidate)
        findViewById<TextView>(R.id.TVNbPoint).text = MainActivity.res.getString(R.string.Text_NbPointRep) + intent.getIntExtra("point", 0).toString() + "/100"
        findViewById<TextView>(R.id.TVNbQues).text = MainActivity.res.getString(R.string.Text_NbQues) + ' ' + intent.getIntExtra("question", 0).toString() + '/' + intent.getIntExtra("total", 0).toString()

        handle = findViewById(R.id.editHandleUser)

        handle.text = ""
        findViewById<TextView>(R.id.TVInfVef).text = MainActivity.res.getString(R.string.Text_Nbmail)

        userDao = MainActivity.db.userDao()
        userResponseDao = MainActivity.db.userReponseDao()
        userTempsDao = MainActivity.db.userTempsDao()
        configTimer()
    }

    private fun configTimer() {

        valid.isClickable = false

        var timer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                val seconds = (millisUntilFinished / 1000 % 60) + 1

                valid.text = seconds.toString()
            }

            override fun onFinish() {
                valid.text = MainActivity.res.getString(R.string.Texte_ButtonValider)
                valid.isClickable = true
            }
        }
        timer.start()
    }

    fun onValid(it : View) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (handle.text.isEmpty())
                return@launch

            val userHandle = handle.text.toString()

            // Recherche d'un utilisateur existant avec le même handle
            val existingUser = userDao.getUserByName(userHandle)

            // Un utilisateur avec ce handle existe déjà.
            // On considère que MainActivity.user est l'utilisateur temporaire (vide) créé au lancement.
            if (existingUser != null) {
                // Récupère toutes les réponses associées à l'utilisateur temporaire
                val tempUserResponses =
                    userResponseDao.getResponsesForUser(MainActivity.user.usr_id)

                // Pour chaque réponse, on incrémente le compteur de tentatives (try) et on change l'id utilisateur pour l'existant
                tempUserResponses.forEach { response ->
                    val updatedResponse = response.copy(
                        usre_nbretry = response.usre_nbretry + 1,
                        usre_usr_id = existingUser.usr_id
                    )
                    userResponseDao.deleteUserResponse(response)
                    userResponseDao.insertUserReponse(updatedResponse)
                }

                // On supprime l'utilisateur temporaire
                userDao.deleteUser(MainActivity.user)
                // On utilise l'utilisateur existant pour la suite
                MainActivity.user = existingUser
                existingUser.usr_nbretry += 1
            } else {
                MainActivity.user.usr_name = userHandle
                MainActivity.user.usr_lang = MainActivity.res.configuration.locales[0].language
            }
            // Insertion dans la table USER_TEMPS
            val userTempsRecord = UserTemps(
                ust_usr_id = MainActivity.user.usr_id,
                ust_usr_retry = MainActivity.user.usr_nbretry,
                ust_temp = intent.getStringExtra("temps") ?: "",
                ust_point = intent.getIntExtra("point", 0)
            )
            userTempsDao.insertUserTemps(userTempsRecord)

            userDao.updateUser(MainActivity.user)
            // Finalisation de l'activité
            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}