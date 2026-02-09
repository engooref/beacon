package com.example.quizpirate

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource


class App : Application() {
    lateinit var player: ExoPlayer
        private set

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val rawUri = RawResourceDataSource.buildRawResourceUri(R.raw.orison)

        player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(rawUri))
            prepare()
            playWhenReady = true
        }
    }
}
