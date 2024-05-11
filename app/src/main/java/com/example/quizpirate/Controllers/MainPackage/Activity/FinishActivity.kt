package com.example.quizpirate.Controllers.MainPackage.Activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.quizpirate.Models.BddClass
import com.example.quizpirate.R
import java.util.regex.Pattern

class FinishActivity : BaseActivity() {

    private lateinit var  handle : TextView
    private lateinit var pseudo : TextView
    private lateinit var email : TextView
    private lateinit var valid : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish)
        setVideo()
        valid = findViewById(R.id.btnValidate)
        findViewById<TextView>(R.id.TVNbPoint).text = MainActivity.res.getString(R.string.Text_NbPointRep) + intent.getIntExtra("point", 0).toString() + "/100"
        findViewById<TextView>(R.id.TVNbQues).text = MainActivity.res.getString(R.string.Text_NbQues) + intent.getIntExtra("question", 0).toString()

        handle = findViewById(R.id.editHandleUser)
        pseudo = findViewById(R.id.editPseudoUser)
        email = findViewById(R.id.editEmailUser)

        (MainActivity.user?.name ?: handle.text).also { handle.text = it }
        (MainActivity.user?.pseudoDiscord ?: pseudo.text).also { pseudo.text = it }
        (MainActivity.user?.info ?: email.text).also { email.text = it }
        findViewById<TextView>(R.id.TVInfVef).text = MainActivity.res.getString(R.string.Text_Nbmail)

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

        var error = false
        // Vérifiez si les champs sont remplis correctement
        if (handle.text.isEmpty()) {
            handle.error = "L'handle est requis"
            error = true
        }

        if (pseudo.text.isEmpty()) {
            pseudo.error = "Le pseudo Discord est requis"
            error = true
        }

        // Définissez le pattern d'expression régulière pour un email
        val pattern: Pattern = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$",
            Pattern.CASE_INSENSITIVE
        )

        if (email.text.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            email.error = "Une adresse email valide est requis"
            error = true
        }

        if (error)
            return

        val getUser = {
            MainActivity.bdd!!.select(
                "SELECT * from ${BddClass.Companion.TABLE.USER.TABLE_NAME} " +
                        "where ${BddClass.Companion.TABLE.USER.TABLE_NAME}.${BddClass.Companion.TABLE.USER.NAME}='${handle.text}'"
            ) {
                MainActivity.user = BddClass.Companion.User(
                    it.getInt(BddClass.Companion.TABLE.USER.ID),
                    it.getString(BddClass.Companion.TABLE.USER.NAME),
                    it.getString(BddClass.Companion.TABLE.USER.DISCORD),
                    it.getString(BddClass.Companion.TABLE.USER.INFO),
                    it.getInt(BddClass.Companion.TABLE.USER.NBRETRY)
                )

            }
        }

        if (MainActivity.user == null) {
            getUser()

            if (MainActivity.user == null) {
                MainActivity.bdd!!.insert("insert into ${BddClass.Companion.TABLE.USER.TABLE_NAME} " +
                        "(${BddClass.Companion.TABLE.USER.NAME}, ${BddClass.Companion.TABLE.USER.DISCORD}, ${BddClass.Companion.TABLE.USER.INFO}, " +
                        "${BddClass.Companion.TABLE.USER.NBRETRY}, ${BddClass.Companion.TABLE.USER.MAILENV}, ${BddClass.Companion.TABLE.USER.LANG}, ${BddClass.Companion.TABLE.USER.NBMAIL}) " +
                        "VALUES ('${handle.text}', '${pseudo.text}', '${email.text}', 1, 'N', '${MainActivity.res.configuration.locales[0].language.uppercase()}', 1)")
            } else {
                MainActivity.user!!.nbRetry++
                MainActivity.bdd!!.update("update ${BddClass.Companion.TABLE.USER.TABLE_NAME} set " +
                        "${BddClass.Companion.TABLE.USER.INFO} = '${MainActivity.user!!.info}', ${BddClass.Companion.TABLE.USER.NAME} = '${MainActivity.user!!.name}', " +
                        "${BddClass.Companion.TABLE.USER.DISCORD} = '${MainActivity.user!!.pseudoDiscord}', " +
                        "${BddClass.Companion.TABLE.USER.NBRETRY} = ${MainActivity.user!!.nbRetry}, " +
                        "${BddClass.Companion.TABLE.USER.MAILENV} = 'N', " +
                        "${BddClass.Companion.TABLE.USER.LANG} = '${MainActivity.res.configuration.locales[0].language.uppercase()}'" +
                        " where ${BddClass.Companion.TABLE.USER.ID} = ${MainActivity.user!!.id}")

            }
        }

        getUser()

        intent.getStringExtra("sqlText")?.let {
            var text = it.replace("userId", MainActivity.user!!.id.toString())
            text = text.replace("nbRetry", MainActivity.user!!.nbRetry.toString())
            MainActivity.bdd!!.insert(text)
        }

        val text = "insert into ${BddClass.Companion.TABLE.USER_TEMPS.TABLE_NAME} " +
                "(${BddClass.Companion.TABLE.USER_TEMPS.USER_ID}, ${BddClass.Companion.TABLE.USER_TEMPS.RETRY}, ${BddClass.Companion.TABLE.USER_TEMPS.TEMPS}," +
                "${BddClass.Companion.TABLE.USER_TEMPS.POINTS}) " +
                "VALUES (${MainActivity.user!!.id}, ${MainActivity.user!!.nbRetry}, '${intent.getStringExtra("temps")}'," +
                "${intent.getIntExtra("point", 0)})"
        MainActivity.bdd!!.insert(text)

        val resultIntent = Intent()
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivity.esp32?.sendMessage("4")
    }
}