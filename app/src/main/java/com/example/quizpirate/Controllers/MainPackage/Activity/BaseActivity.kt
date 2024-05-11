package com.example.quizpirate.Controllers.MainPackage.Activity


import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.quizpirate.R


open class BaseActivity : AppCompatActivity() {

    protected lateinit var videoView: VideoView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
    }

    fun setVideo() {
        // Récupérer la référence du VideoView
        videoView = findViewById(R.id.videoView)

        // Chemin d'accès à la vidéo dans res/raw
        val videoPath = "android.resource://" + packageName + "/" + R.raw.pyro_main_theme

        // Convertir le chemin en URI
        val uri = Uri.parse(videoPath)

        // Charger la vidéo dans le VideoView
        videoView.setVideoURI(uri)
        // Démarrer la lecture de la vidéo en boucle
        videoView.start()
        videoView.setOnPreparedListener { mp: MediaPlayer ->
            // Définir la boucle de la vidéo
            mp.isLooping = true
        }
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
    }

    override fun onResume() {
        super.onResume()
        videoView.start()
    }
}