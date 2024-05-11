package com.example.quizpirate.Controllers.MainPackage.Activity

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import com.example.appesp32.Models.TcpClient
import com.example.quizpirate.Models.BddClass
import com.example.quizpirate.R
import com.example.quizpirate.Utils.WaitNotify
import com.example.quizpirate.databinding.ActivityMainBinding
import java.util.Locale
import java.util.Timer
import java.util.TimerTask


class MainActivity : BaseActivity() {

    private lateinit var mainBinding: ActivityMainBinding

    //Variables relative a l'esp
    private var useEsp : Boolean = true
    private val mNotify = WaitNotify()
    private var idString : Int = R.string.Texte_Debut

    //Varible pour le XML
    private lateinit var textPrinc : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        setVideo()

        textPrinc = findViewById(R.id.TextPrinc)


        configAndShowConnect()
        res = getResourcesForLocale(this, Locale("en"))
        onChangeLanguage(textPrinc)

    }

    fun onClickParam(view : View) {
        var dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_settings)
        dialog.setTitle("Paramètres")

        val chbEsp = dialog.findViewById<CheckBox>(R.id.chbEsp)
        chbEsp.isChecked = useEsp
        chbEsp.setOnCheckedChangeListener { _, isChecked ->
            dialog.findViewById<EditText>(R.id.editPortEsp).isEnabled = isChecked
            dialog.findViewById<EditText>(R.id.editIPEsp).isEnabled = isChecked
        }

        if (esp32 != null) {
            dialog.findViewById<EditText>(R.id.editPortEsp).setText(esp32!!.host())
            dialog.findViewById<EditText>(R.id.editIPEsp).setText(esp32!!.port().toString())
        }

        dialog.findViewById<Button>(R.id.btnValidParam).text = getResourcesForLocale(this, Locale("fr")).getString(R.string.Texte_ButtonValider)

        val textTime = dialog.findViewById<TextView>(R.id.TextTime)
        var timerMinutes : Int = (timeTimer / 1000 / 60).toInt()
        var timerSeconds : Int = (timeTimer / 1000 % 60).toInt()
        textTime.text = "$timerMinutes : $timerSeconds"


        textTime.setOnClickListener{

            val timePicker = PickerClass()
            timePicker.setTitle("Choix Timer")

            timePicker.setOnTimeSetOption("Valider") {minutes, seconds ->
                textTime.text = "$minutes : $seconds"
                timerMinutes = minutes
                timerSeconds = seconds
            }

            timePicker.initialMinute = timerMinutes
            timePicker.initialSeconds = timerSeconds


            /* To show the dialog you have to supply the "fragment manager"
                and a tag (whatever you want)
            */
            timePicker.show(supportFragmentManager, "time_picker")
        }


        dialog.findViewById<Button>(R.id.btnValidParam).setOnClickListener {
            try {
                if (dialog.findViewById<EditText>(R.id.mdp).text.toString() != "1CCBeacon")
                    throw Exception("Mot de passe Incorrect")

                useEsp = chbEsp.isChecked
                timeTimer = ((timerMinutes * 60000) + (timerSeconds * 1000)).toLong()

                if (useEsp) {
                    esp32 = TcpClient(
                        dialog.findViewById<EditText>(R.id.editIPEsp).text.toString(),
                        dialog.findViewById<EditText>(R.id.editPortEsp).text.toString().toInt(),
                        mNotify
                    )

                    esp32!!.mMessageListener = ::messageReceive
                    esp32!!.run()
                    mNotify.doWait()

                    if (!esp32!!.isRun) {
                        throw Exception("Connexion non abouti")
                    } else {
                        esp32!!.sendMessage("O")
                        idString = R.string.Texte_AttentePrincipalEsp
                        textPrinc.text = res.getString(idString)
                        mainBinding.root.setOnClickListener { null }
                    }
                } else {
                    idString = R.string.Texte_AttentePrincipal
                    textPrinc.text = res.getString(idString)
                    mainBinding.root.setOnClickListener {
                        val timer = Timer()
                        timer.schedule(MyTimerTask(this, timer), 0, 1000)
                        mainBinding.root.setOnClickListener {null}
                    }
                }

                dialog.dismiss()
            } catch (e: Exception) {
                val textError = dialog.findViewById<TextView>(R.id.textErrorParam)
                textError.text = "Erreur"
                textError.visibility = View.VISIBLE

                val textErrorDetail = dialog.findViewById<TextView>(R.id.textDetailErrorParam)
                textErrorDetail.visibility = View.VISIBLE
                textErrorDetail.text = e.message
            }

        }

        dialog.show()
    }

    fun onChangeLanguage( view : View) {
        res = getResourcesForLocale(this, Locale(
            if (res.configuration.locales[0].language == "fr")
                "en"
            else
                "fr"
        ))


        textPrinc.text = res.getString(idString).replace("%s", " " + user?.name)

        findViewById<ImageButton>(R.id.LangBtn).setImageDrawable(
            getDrawable(
                if(res.configuration.locales[0].language == "fr") R.drawable.drapeau_anglais
                else R.drawable.drapeau_francais
            )
        )

    }

    private fun getResourcesForLocale(context: Context, locale: Locale): Resources {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration).resources
    }

    private fun configAndShowConnect() {
        if (bdd != null ) return
        val dialog = Dialog(this)

        dialog.setContentView(R.layout.popup_try_connect)
        dialog.setTitle("Dialog box")

        dialog.findViewById<Button>(R.id.BtnConnect).setOnClickListener {
            try {
                bdd = BddClass(
                    dialog.findViewById<EditText>(R.id.editIP).text.toString(),
                    dialog.findViewById<EditText>(R.id.editPort).text.toString(),
                    dialog.findViewById<EditText>(R.id.editNameBdd).text.toString(),
                    dialog.findViewById<EditText>(R.id.editIdBdd).text.toString(),
                    dialog.findViewById<EditText>(R.id.editMdpBdd).text.toString()
                )
                bdd!!.connect()

                dialog.dismiss()

            } catch (e: Exception) {
                val textError = dialog.findViewById<TextView>(R.id.textError)
                textError.text = "Erreur de connexion à la BDD: "
                textError.visibility = View.VISIBLE

                val textErrorDetail = dialog.findViewById<TextView>(R.id.textDetailError)
                textErrorDetail.visibility = View.VISIBLE
                textErrorDetail.text = e.message
            }
        }
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()
    }

    private fun messageReceive(message: String) {

        var values = message.split(';')
        if (values.size >= 2) {
            when (values[0].toInt()) {
                1 -> {
                    when (values[1].toInt()) {

                        4 -> {
                            val timer = Timer()
                            timer.schedule(MyTimerTask(this, timer), 0, 1000)
                        }

                        2 -> {
                            if (values.size >= 2) {
                                //Information handle;info;
                                bdd!!.select("select * from ${BddClass.Companion.TABLE.USER.TABLE_NAME} " +
                                        "where ${BddClass.Companion.TABLE.USER.NAME} = '${values[2]}'") {
                                    user = BddClass.Companion.User(it.getInt(BddClass.Companion.TABLE.USER.ID),
                                        it.getString(BddClass.Companion.TABLE.USER.NAME),
                                        values[4],
                                        values[3],
                                        it.getInt(BddClass.Companion.TABLE.USER.NBRETRY))
                                }

                                if (user == null)  user = BddClass.Companion.User(0, values[2], values[3], values[4], 1)
                            }

                            idString = R.string.Texte_ButtonPrincipal


                        }
                        else -> {
                            idString = when (values[1].toInt()) {
                                0 -> R.string.Texte_AttentePrincipalEsp
                                1 -> R.string.Texte_ChargementPrincipal
                                5 -> R.string.Texte_RetraitCarte
                                else -> R.string.Texte_ErreurPrincipal
                            }
                        }
                    }

                    this@MainActivity.runOnUiThread(java.lang.Runnable {
                        textPrinc.text = res.getString(idString).replace("%s", " " + user?.name)
                    })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        esp32?.sendMessage("4")
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            findViewById<ImageButton>(R.id.LangBtn).isEnabled = true
            findViewById<ImageButton>(R.id.BtnParam).isEnabled = true
            if (useEsp)
                esp32?.sendMessage("4")
            else {
                idString = R.string.Texte_AttentePrincipal
                textPrinc.text = res.getString(idString)
                mainBinding.root.setOnClickListener {
                    val timer = Timer()
                    timer.schedule(MyTimerTask(this, timer), 0, 1000)
                    mainBinding.root.setOnClickListener {null}
                }
            }

            user = null
        }
    }

    private fun timerDebut(i : Int){
        idString = R.string.Texte_AutoriserPrincipal
        var str = res.getString(idString).replace("0", i.toString()) +
                res.getString(if (i == 1) R.string.texte_seconde else R.string.texte_secondes)
        textPrinc.text = str
    }


    class MyTimerTask (private val activity: MainActivity, private val timer: Timer) : TimerTask() {
        private var i = 5

        init {
            activity.findViewById<ImageButton>(R.id.LangBtn).isEnabled = false
            activity.findViewById<ImageButton>(R.id.BtnParam).isEnabled = false
        }
        override fun run() {
            if (i == 0) {
                timer.cancel()

                val intent = Intent(activity, QuizActivity::class.java)
                activity.resultLauncher.launch(intent)
            }
            activity.runOnUiThread{ activity.timerDebut(i--) }
        }
    }



    companion object {
        var bdd : BddClass? = null
        var user: BddClass.Companion.User? = null
        var esp32 : TcpClient? = null
        var timeTimer : Long = 90000
        lateinit var res : Resources
        const val REQUEST_CODE = 452
    }
}