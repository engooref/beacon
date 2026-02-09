package com.example.quizpirate.Controllers.MainPackage.Activity

import BluetoothManagerPerso
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.quizpirate.Controllers.BDD.AppDatabase
import com.example.quizpirate.Controllers.BDD.Entity.User
import com.example.quizpirate.R
import com.example.quizpirate.databinding.ActivityMainBinding
import com.example.quizpirate.Utils.CsvUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import androidx.core.graphics.createBitmap
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


@OptIn(UnstableApi::class) @Suppress("UNUSED_EXPRESSION")
class MainActivity : BaseActivity() {

    private lateinit var mainBinding: ActivityMainBinding

    private var idString : Int = R.string.Texte_Debut

    private var bluetoothAdapter: BluetoothAdapter? = null

    //Varible pour le XML
    private lateinit var textPrinc : TextView
    private lateinit var textSec : TextView

    private lateinit var sideList: RecyclerView
    private lateinit var sideAdapter: SideImageAdapter
    private var selectedSideIndex: Int = RecyclerView.NO_POSITION


    private val REQUEST_CODE_PERM = 1001
    private val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }.toTypedArray()

    // Lancer le sélecteur de fichiers pour obtenir un fichier CSV
    private val csvFilePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        // Copier le contenu dans un fichier temporaire dans le cache
                        val tempFile = File.createTempFile("imported_questions", ".csv", cacheDir)
                        tempFile.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                        CsvUtils.importQuestionsFromCsv(tempFile, db.questionDao(), db.responseDao())
                    }



                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private var isReceiver : Boolean = false
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private var deviceAdapter: ArrayAdapter<String>? = null
    private var deviceDialog: AlertDialog? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    @Suppress("DEPRECATION")
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!discoveredDevices.contains(it)) {
                            discoveredDevices.add(it)
                            // Mettre à jour l'adapter de la popup si elle est affichée
                            deviceAdapter?.apply {
                                add(it.name ?: it.address)
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentLayout(mainBinding.root)

        textPrinc = findViewById(R.id.TextPrinc)
        textSec = findViewById(R.id.tagline)

        // 2) RecyclerView
        sideList = findViewById(R.id.sideImageList)
        sideList.setHasFixedSize(true)
        sideList.itemAnimator = DefaultItemAnimator()
        // >>> IMPORTANT : LayoutManager <<<
        sideList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        // Si tu veux horizontal : LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        val shipImages: List<Int> = loadDrawablesByPrefix("vaisseau_")

        // 3) Adapter + callback
        sideAdapter = SideImageAdapter(shipImages) { position ->

            val resId = shipImages.getOrNull(position) ?: return@SideImageAdapter
            selectedSideIndex = position

            vaisseauChoice = resId                    // ⬅️ tu as l'ID R.drawable ici
            sideAdapter.setSelectedIndex(position)

        }
        sideList.adapter = sideAdapter

        val restored = savedInstanceState?.getInt("sel", RecyclerView.NO_POSITION) ?: RecyclerView.NO_POSITION
        if (restored in shipImages.indices) {
            selectedSideIndex = restored
            vaisseauChoice = shipImages[restored]
            sideAdapter.setSelectedIndex(restored)
            sideList.scrollToPosition(restored)

        } else if (shipImages.isNotEmpty()) {
            // ✅ Sélection par défaut
            selectedSideIndex = 0
            vaisseauChoice = shipImages[0]
            sideAdapter.setSelectedIndex(0)
            sideList.scrollToPosition(0)

        }

        db = AppDatabase.getDatabase(this)

        res = getResourcesForLocale(this, Locale("en"))
        onChangeLanguage("fr")

        // Vérifier les permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERM)
        }

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val sharedPref = getSharedPreferences("quiz", MODE_PRIVATE)
        timeTimer = sharedPref.getLong("timeTimer", timeTimer)

        idString = R.string.Texte_AttentePrincipal
        textPrinc.text = res.getString(idString)
        mainBinding.root.setOnClickListener {
            val timer = Timer()
            timer.schedule(MyTimerTask(this, timer), 0, 1000)
            mainBinding.root.setOnClickListener {null}
        }
    }

    private fun loadDrawablesByPrefix(prefix: String): List<Int> {
        val drawClass = R.drawable::class.java
        val ids = mutableListOf<Int>()
        for (field in drawClass.fields) {
            if (field.name.startsWith(prefix)) {
                try { ids += field.getInt(null) } catch (_: Exception) {}
            }
        }
        return ids.sortedBy { id ->
            try { resources.getResourceEntryName(id) } catch (_: Exception) { id.toString() }
        }
    }

    private fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Fonction qui affiche la boîte de dialogue pour saisir le mot de passe
    fun onClickParam(view : View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Entrez le mot de passe")
        builder.setCancelable(false)
        // Création d'un EditText pour saisir le mot de passe
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        // Bouton "Valider"
        builder.setPositiveButton("Valider") { _, _ ->
            val password = input.text.toString()
            if (password == "1CCBeacon") {
                // Si le mot de passe est correct, affiche une popup de confirmation
                showParamPopup()
            } else {
                Toast.makeText(this, "Mot de passe incorrect", Toast.LENGTH_SHORT).show()
            }
        }

        // Bouton "Annuler"
        builder.setNegativeButton("Annuler") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create().apply {
            // empêche la fermeture par tap à l’extérieur
            setCanceledOnTouchOutside(false)
        }

        dialog.show()
    }

    @SuppressLint("CutPasteId", "SetTextI18n")
    private fun showParamPopup() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_settings)
        dialog.setTitle("Paramètres")
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        val sharedPref = getSharedPreferences("quiz", MODE_PRIVATE)

        dialog.findViewById<Button>(R.id.btnValidParam).text = getResourcesForLocale(this, Locale("fr")).getString(R.string.Texte_ButtonValider)

        timeTimer = sharedPref.getLong("timeTimer", timeTimer)
        val textTime = dialog.findViewById<TextView>(R.id.TextTime)
        var timerMinutes : Int = (timeTimer / 1000 / 60).toInt()
        var timerSeconds : Int = (timeTimer / 1000 % 60).toInt()
        textTime.text = "$timerMinutes : $timerSeconds"

        dialog.findViewById<Button>(R.id.btnConEsp).visibility = if (bluetoothAdapter == null) View.INVISIBLE else View.VISIBLE

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

                timeTimer = ((timerMinutes * 60000) + (timerSeconds * 1000)).toLong()

                //val  = getSharedPreferences("quiz", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putLong("timeTimer", timeTimer)
                    apply() // ou commit() si tu veux une écriture synchrone
                }

                if (bluetooth != null) {
                    idString = R.string.Texte_AttentePrincipalEsp
                    textPrinc.text = res.getString(idString)
                    mainBinding.root.setOnClickListener { null }
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

    fun onClickDeleteBdd(view: View) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Efface toutes les tables de la base de données
            db.clearAllTables()
            // Optionnel : affiche un message à l'utilisateur pour indiquer que l'opération a réussi
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Toutes les données ont été supprimées", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun onClickBluethooth(view: View){

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetootLauncher.launch(enableBtIntent)
            return  // Vous pouvez attendre la réponse avant de poursuivre.
        }

        // Enregistrer le BroadcastReceiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        isReceiver = true
        registerReceiver(receiver, filter)

        // Démarrer la découverte
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
        bluetoothAdapter?.startDiscovery()

        // Afficher la popup pour la sélection des appareils
        showDeviceSelectionDialog()
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    private fun showDeviceSelectionDialog() {
        // Si aucun appareil n'est découvert, vous pouvez afficher un message ou attendre un court instant
        if (discoveredDevices.isEmpty()) {
            Toast.makeText(this, "Aucun appareil découvert pour l'instant", Toast.LENGTH_SHORT).show()
        }

        // Initialiser l'adapter à partir de la liste actuelle des appareils
        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, discoveredDevices.map {
            it.name ?: it.address
        }.toMutableList())

        val builder = AlertDialog.Builder(this)
            .setTitle("Sélectionnez un appareil")
            .setAdapter(deviceAdapter) { _, which ->
                val selectedDevice = discoveredDevices[which]
                Toast.makeText(this,
                    "Sélectionné : ${selectedDevice.name ?: selectedDevice.address}",
                    Toast.LENGTH_SHORT).show()

                bluetooth = bluetoothAdapter?.let { adapter ->
                    BluetoothManagerPerso(adapter, selectedDevice) { data ->
                        // onDataReceived → on écrit directement ce qu’on veut faire
                        messageReceive(data)
                    }.apply {
                        connect();
                    }
                }

            }
            .setNegativeButton("Annuler") { dialog, _ ->
                dialog.dismiss()
            }

        deviceDialog = builder.create()
        deviceDialog?.show()
    }

    fun onClickExport(view : View) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Exemple dans ton activité


            // Récupère les données d'export
            val exportData = db.userTempsDao().getBestUserRecords()

            // Crée un fichier CSV dans le répertoire externe de l'application
            val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportFile = File(downloadFolder, "user_export.csv")
            CsvUtils.exportUsersToCsv(exportFile, exportData)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Export effectué dans ${exportFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun onClickImport(view : View) {
        csvFilePickerLauncher.launch("*/*")
    }


    private fun getResourcesForLocale(context: Context, locale: Locale): Resources {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration).resources
    }

    private fun messageReceive(message: String) {

        if (initLaunch)
            return

        var values = message.split(';')
        if (values.size >= 2) {
            when (values[0].toInt()) {
                1 -> {
                    when (values[1].toInt()) {

                        4 -> {
                            val timer = Timer()
                            timer.schedule(MyTimerTask(this, timer), 0, 1000)
                        }
                        else -> {
                            idString = when (values[1].toInt()) {
                                0 -> R.string.Texte_AttentePrincipalEsp
                                1 -> R.string.Texte_ChargementPrincipal
                                2 -> R.string.Texte_ButtonPrincipal
                                5 -> R.string.Texte_RetraitCarte
                                else -> R.string.Texte_ErreurPrincipal
                            }
                        }
                    }

                    this@MainActivity.runOnUiThread(java.lang.Runnable {
                        textPrinc.text = res.getString(idString)
                    })
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()
        // Annuler la découverte et désenregistrer le BroadcastReceiver
        if (isReceiver) {
            unregisterReceiver(receiver)
        }

        // Arrêter la découverte si nécessaire
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            findViewById<ImageButton>(R.id.LangBtn).isEnabled = true
            findViewById<ImageButton>(R.id.BtnParam).isEnabled = true
            initLaunch = false

            if (bluetooth == null) {
                idString = R.string.Texte_AttentePrincipal
                textPrinc.text = res.getString(idString)
                mainBinding.root.setOnClickListener {
                    val timer = Timer()
                    timer.schedule(MyTimerTask(this, timer), 0, 1000)
                    mainBinding.root.setOnClickListener {null}
                }
            } else {
                bluetooth?.write("4");


            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    private var bluetootLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onClickBluethooth(findViewById<Button>(R.id.BtnParam))
        }

    }

    private fun timerDebut(i : Int){
        idString = R.string.Texte_AutoriserPrincipal
        val str = res.getString(idString).replace("0", i.toString()) +
                res.getString(if (i == 1) R.string.texte_seconde else R.string.texte_secondes)
        textPrinc.text = str
    }


    class MyTimerTask (private val activity: MainActivity, private val timer: Timer) : TimerTask() {
        private var i = 5

        init {
            activity.findViewById<ImageButton>(R.id.LangBtn).isEnabled = false
            activity.findViewById<ImageButton>(R.id.BtnParam).isEnabled = false
            initLaunch = true
        }
        override fun run() {
            if (i == 0) {
                timer.cancel()
                activity.createEmptyUser()
                val intent = Intent(activity, QuizActivity::class.java)
                activity.resultLauncher.launch(intent)
            }
            activity.runOnUiThread{ activity.timerDebut(i--) }
        }
    }

    private fun createEmptyUser() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Insère un utilisateur avec des valeurs par défaut
            user = db.userDao().getUserById(db.userDao().insertUser(User()))!!

        }
    }

    companion object {
        lateinit var db : AppDatabase
        var bluetooth : BluetoothManagerPerso? = null
        lateinit  var user : User
        var timeTimer : Long = 90000
        lateinit var res : Resources
        var initLaunch : Boolean = false
        var vaisseauChoice: Int = 0
        val LANGUAGES = listOf(
            LanguageItem("English", "en", R.drawable.drapeau_anglais),
            LanguageItem("Français", "fr", R.drawable.drapeau_francais),
            LanguageItem("Italian", "it", R.drawable.drapeau_italien),
            LanguageItem("Portuguese", "pt", R.drawable.drapeau_portugais),
            LanguageItem("Spanish", "es", R.drawable.drapeau_espagne)
        )
    }

    data class LanguageItem(val name: String, val code: String, val flagResId: Int)

    fun onShowLanguageList(v: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(res.getString(R.string.Texte_ChoixLangue))
        val items = LANGUAGES.map { it.name }
        val flags = LANGUAGES.map { it.flagResId }
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.select_dialog_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                val drawable = ContextCompat.getDrawable(context, flags[position])
                drawable?.setBounds(0, 0, 70, 70) // 64x64 pixels, ajuste selon besoin
                textView.setCompoundDrawables(drawable, null, null, null)
                textView.compoundDrawablePadding = 24
                return view
            }
        }
        builder.setAdapter(adapter) { _, which ->
            val lang = LANGUAGES[which]
            onChangeLanguage(lang.code)
        }
        builder.show()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun onChangeLanguage(langCode: String) {
        res = getResourcesForLocale(this, Locale(langCode))
        textPrinc.text = res.getString(idString)
        textSec.text = res.getString(R.string.mainText)
        findViewById<TextView>(R.id.shipLabel).text = res.getString(R.string.ton_vaisseau)
        val lang = LANGUAGES.firstOrNull { it.code == langCode }
        val imageButton = findViewById<ImageButton>(R.id.LangBtn)
        val resId = lang?.flagResId ?: R.drawable.drapeau_francais
        val drawable = ContextCompat.getDrawable(this, resId)
        if (drawable != null) {
            val density = resources.displayMetrics.density
            val sizePx = (70 * density).toInt()
            val bitmap = createBitmap(sizePx, sizePx)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            imageButton.setImageBitmap(bitmap)
        } else {
            imageButton.setImageResource(R.drawable.drapeau_francais)
        }
    }


}