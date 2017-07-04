/*
    Copyright 2017 Robert Theis
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rmtheis.translator.standalone

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v4.app.Fragment
import android.util.Log
import android.widget.Toast
import java.lang.IllegalArgumentException
import java.util.*

/**
 * Retained fragment for handling text-to-speech.
 */
class TextToSpeechFragment : Fragment() {

    private var tts: TextToSpeech? = null
    private lateinit var ttsOnInitListener: TextToSpeech.OnInitListener

    companion object {
        fun newInstance(): TextToSpeechFragment {
            return TextToSpeechFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        ttsOnInitListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.ERROR) {
                Log.e("TtsFragment", "TTS initialization failed")
                tts = null
            }
        }
        tts = TextToSpeech(activity.applicationContext, ttsOnInitListener)
    }

    override fun onDestroy() {
        tts?.stop()
        super.onDestroy()
    }

    fun updateTtsAvailability(ttsCode: String): Boolean {
        val targetLocale = Locale(ttsCode)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && tts != null &&
                    tts?.availableLanguages?.contains(targetLocale) ?: false) {
                tts?.language = targetLocale
                activity.volumeControlStream = AudioManager.STREAM_MUSIC
                return true
            }
        } catch (e: NullPointerException) {
            // Apparent Android 5 issue
        } catch (e: IllegalArgumentException) {
            // Apparent TTS implementation failure
        }
        return false
    }

    @TargetApi(21)
    fun speakText(text: String) {
        if (tts != null) {
            if (tts!!.isSpeaking) {
                tts!!.stop()
                return
            }
            if (checkVolume()) {
                tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "place-holder")
            }
        }
    }

    private fun checkVolume(): Boolean {
        val audioManager: AudioManager = activity.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            Toast.makeText(activity, getString(R.string.msg_volume_off), Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
}
