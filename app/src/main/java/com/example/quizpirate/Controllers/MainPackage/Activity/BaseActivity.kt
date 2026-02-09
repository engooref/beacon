package com.example.quizpirate.Controllers.MainPackage.Activity

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi

import com.example.quizpirate.App
import com.example.quizpirate.R

@UnstableApi
open class BaseActivity : AppCompatActivity() {

    private var playerView: androidx.media3.ui.PlayerView? = null
    protected lateinit var contentHost: FrameLayout
    private val appPlayer get() = (application as App).player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ❗️Toujours le layout commun ici
        setContentView(R.layout.activity_base)
        playerView = findViewById(R.id.playerView)
        contentHost = findViewById(R.id.contentHost)
    }

    /** Appelle ça dans les écrans enfants au lieu de setContentView */
    protected fun setContentLayout(view: View) {
        contentHost.removeAllViews()
        contentHost.addView(view)
    }

    override fun onStart() {
        super.onStart()
        playerView?.player = appPlayer
    }

    override fun onResume() {
        super.onResume()
        appPlayer.playWhenReady = true
    }

    override fun onStop() {
        playerView?.player = null
        super.onStop()
    }
}
