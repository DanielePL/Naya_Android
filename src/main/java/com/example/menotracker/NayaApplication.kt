package com.example.menotracker

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.example.menotracker.community.notifications.MaxOutFridayNotificationManager
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.data.UserProfileRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class NayaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Initialize Room database for UserProfileRepository
        UserProfileRepository.initialize(this)

        // Initialize notification channels for community features
        if (CommunityFeatureFlag.ENABLED) {
            MaxOutFridayNotificationManager.createNotificationChannel(this)
        }

        // Initialize PDFBox for WOD Scanner PDF parsing
        try {
            PDFBoxResourceLoader.init(applicationContext)
            Log.d("NayaApp", "PDFBox initialized successfully")
        } catch (e: Exception) {
            Log.e("NayaApp", "Failed to initialize PDFBox: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
