package com.diegowh.konohaproject.domain.sound

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import com.diegowh.konohaproject.utils.sound.SoundType

class SoundPlayer(private val context: Context) {

    private val soundPool = SoundPool.Builder().setMaxStreams(2).build()
    private val sounds = mutableMapOf<SoundType, Int>()

    fun loadSound(type: SoundType, resId: Int) {
        sounds[type] = soundPool.load(context, resId, 1)
    }

    fun play(type: SoundType) {
        sounds[type]?.let { id ->
            val vol = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                .getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            soundPool.play(id, vol, vol, 1, 0, 1.0f)
        }
    }

    fun release() = soundPool.release()

}