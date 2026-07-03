package com.kyzn.photofindai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val videoView = findViewById<VideoView>(R.id.videoView)
        val videoPath = "android.resource://" + packageName + "/" + R.raw.splash_video
        videoView.setVideoURI(Uri.parse(videoPath))

        videoView.setOnCompletionListener {
            startMainActivity()
        }

        videoView.setOnErrorListener { _, _, _ ->
            startMainActivity()
            true
        }

        videoView.start()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
