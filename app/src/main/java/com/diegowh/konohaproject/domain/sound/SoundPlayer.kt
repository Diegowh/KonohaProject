package com.diegowh.konohaproject.domain.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.annotation.RawRes
import com.diegowh.konohaproject.core.sound.SoundType

class SoundPlayer(private val context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private val sounds = mutableMapOf<SoundType, Int>()
    private val loaded = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
    }

    fun loadSound(type: SoundType, @RawRes resId: Int) {
        val sampleId = soundPool.load(context, resId, 1)
        sounds[type] = sampleId
    }

    fun play(type: SoundType) {
        val sampleId = sounds[type] ?: return

        if (sampleId in loaded) {
            val volIndex = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val vol = volIndex.toFloat() / maxVolume
            soundPool.play(sampleId, vol, vol, 1, 0, 1f)
        }
    }

    fun release() = soundPool.release()
}