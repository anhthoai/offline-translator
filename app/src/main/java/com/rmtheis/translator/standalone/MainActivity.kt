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
import android.app.Activity
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.widget.CardView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.util.*

open class MainActivity : AdsActivity(), TranslatorFragment.TranslationListener {

    private var mTtsFragment: TextToSpeechFragment? = null
    private var mTranslatorFragment: TranslatorFragment? = null
    private var mIsTranslationInProgress: Boolean? = null
    private lateinit var mSharedPrefs: SharedPreferences
    private var mCurrentMode: String? = null
        set(value) {
            field = value
            updateModeLabel()
            updateTtsAvailability()
        }

    private lateinit var mSourceLanguage: TextView
    private lateinit var mTargetLanguage: TextView
    private lateinit var mInputLanguageLabel: TextView
    private lateinit var mClearButton: ImageButton
    private lateinit var mInputText: EditText
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mTranslationCardView: CardView
    private lateinit var mTargetLanguageLabel: TextView
    private lateinit var mTranslationText: TextView
    private lateinit var mSpeakButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assignContentView()

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        saveFirstVersionInfo()

        // Lazily instantiate and attach retained fragments for handling translation and
        // text-to-speech. These fragments don't have UI and don't receive calls to onCreateView().
        mTtsFragment = supportFragmentManager.findFragmentByTag(TEXT_TO_SPEECH_FRAGMENT_TAG)
                as TextToSpeechFragment?
        if (mTtsFragment == null) {
            mTtsFragment = TextToSpeechFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(mTtsFragment, TEXT_TO_SPEECH_FRAGMENT_TAG).commit()
        }

        mTranslatorFragment = supportFragmentManager.findFragmentByTag(TRANSLATOR_FRAGMENT_TAG)
                as TranslatorFragment?
        if (mTranslatorFragment == null) {
            mTranslatorFragment = TranslatorFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(mTranslatorFragment, TRANSLATOR_FRAGMENT_TAG).commit()
        }

        mSourceLanguage = findViewById(R.id.sourceLanguage) as TextView
        mTargetLanguage = findViewById(R.id.targetLanguage) as TextView
        mInputLanguageLabel = findViewById(R.id.inputLanguageLabel) as TextView
        mClearButton = findViewById(R.id.clearButton) as ImageButton
        mInputText = findViewById(R.id.inputText) as EditText
        mProgressBar = findViewById(R.id.progressBar) as ProgressBar
        mTranslationCardView = findViewById(R.id.translationCardView) as CardView
        mTargetLanguageLabel = findViewById(R.id.targetLanguageLabel) as TextView
        mTranslationText = findViewById(R.id.translationText) as TextView
        mSpeakButton = findViewById(R.id.speakButton) as ImageButton

        mInputText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s == null || s.isEmpty()) {
                    mClearButton.visibility = View.INVISIBLE
                } else {
                    mClearButton.visibility = View.VISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing
            }
        })

        if (savedInstanceState == null) {
            mCurrentMode = mSharedPrefs.getString(PREF_KEY_MODE, getForwardMode())
        } else {
            mCurrentMode = savedInstanceState.getString(TRANSLATION_MODE_KEY, getForwardMode())
            mTranslationCardView.visibility = savedInstanceState.getInt(TRANSLATION_VISIBILITY_KEY)
            mTranslationText.text = savedInstanceState.getString(TRANSLATION_TEXT_KEY)
            mIsTranslationInProgress = savedInstanceState.getBoolean(IS_TRANSLATION_IN_PROGRESS_KEY,
                    false)

            // Grab any translation that completed while this activity was being restarted.
            if (mIsTranslationInProgress != null && mIsTranslationInProgress!!
                    && mTranslatorFragment!!.mTranslatedText != null) {
                onTranslationCompleted(mTranslatorFragment!!.mTranslatedText!!)
            }
        }

        val inboundText = handleInboundIntent(intent)
        if (inboundText != "") {
            mInputText.setText(inboundText)
        }

        startShowingAds()
    }

    open fun assignContentView() {
        setContentView(R.layout.activity_main)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(TRANSLATION_MODE_KEY, mCurrentMode)
        outState?.putInt(TRANSLATION_VISIBILITY_KEY, mTranslationCardView.visibility)
        outState?.putString(TRANSLATION_TEXT_KEY, mTranslationText.text.toString())
        if (mIsTranslationInProgress != null) {
            outState?.putBoolean(IS_TRANSLATION_IN_PROGRESS_KEY, mIsTranslationInProgress!!)
        }
        mSharedPrefs.edit().putString(PREF_KEY_MODE, mCurrentMode).apply()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_item_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                intent.putExtra(AboutActivity.EXTRA_KEY_IS_BILLING_AVAILABLE,
                        mIsBillingAvailable);
                intent.putExtra(AboutActivity.EXTRA_KEY_IS_ADVERTISING_DISABLED,
                        mIsAdvertisingDisabled);
                startActivityForResult(intent, ABOUT_ACTIVITY_REQUEST_CODE)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ABOUT_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Advertising was disabled in AboutActivity. Hiding ads...")
            mIsAdvertisingDisabled = true
            showAdView(false)
        }
    }

    private fun getForwardMode(): String {
        return String.format(Locale.US, "%s-%s", getString(R.string.lang_code_one),
                getString(R.string.lang_code_two))
    }

    private fun getReverseMode(): String {
        return String.format(Locale.US, "%s-%s", getString(R.string.lang_code_two),
                getString(R.string.lang_code_one))
    }

    private fun updateModeLabel() {
        if (mCurrentMode == getForwardMode()) {
            mSourceLanguage.text = getString(R.string.lang_name_one)
            mTargetLanguage.text = getString(R.string.lang_name_two)
            mInputLanguageLabel.text = getString(R.string.lang_name_one)
            mTargetLanguageLabel.text = getString(R.string.lang_name_two)
        } else {
            mSourceLanguage.text = getString(R.string.lang_name_two)
            mTargetLanguage.text = getString(R.string.lang_name_one)
            mInputLanguageLabel.text = getString(R.string.lang_name_two)
            mTargetLanguageLabel.text = getString(R.string.lang_name_one)
        }
    }

    private fun updateTtsAvailability() {
        if (mTtsFragment!!.updateTtsAvailability(getTtsCode())) {
            mSpeakButton.visibility = View.VISIBLE
        } else {
            mSpeakButton.visibility = View.GONE
        }
    }

    fun swapDirection(view: View?) {
        val animationDurationMs = 100L
        val animation = RotateAnimation(0.0f, 180.0f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)
        animation.duration = animationDurationMs
        animation.interpolator = LinearInterpolator()
        view?.startAnimation(animation)
        Handler().postDelayed({ swapMode() }, (animationDurationMs + 20))
    }

    fun swapMode() {
        if (mCurrentMode == getForwardMode()) {
            mCurrentMode = getReverseMode()
        } else {
            mCurrentMode = getForwardMode()
        }
    }

    fun clearText(view: View?) {
        mInputText.setText("")
        mTranslationCardView.visibility = View.GONE
    }

    fun translate(view: View?) {
        if (mInputText.text.isNotEmpty()) {
            updateTtsAvailability()
            mTranslationText.text = ""
            mProgressBar.progress = 0
            mProgressBar.visibility = View.VISIBLE
            mTranslationCardView.visibility = View.GONE
            hideKeyboard()
            mTranslatorFragment!!.translate(mCurrentMode!!, mInputText.text.toString())
            mIsTranslationInProgress = true
        }
    }

    override fun onProgressUpdate(indeterminate: Boolean, progress: Int) {
        mProgressBar.visibility = View.VISIBLE
        mProgressBar.progress = progress
        mProgressBar.isIndeterminate = indeterminate
    }

    override fun onTranslationCompleted(translatedText: String) {
        mProgressBar.visibility = View.GONE
        mTranslationCardView.visibility = View.VISIBLE
        mTranslationText.text = translatedText
        mIsTranslationInProgress = false
    }

    @TargetApi(21)
    fun speakTargetText(view: View) {
        mTtsFragment!!.speakText(mTranslationText.text.toString())
    }

    private fun getTtsCode(): String {
        if (mCurrentMode == getForwardMode()) {
            return getString(R.string.tts_code_two)
        } else {
            return getString(R.string.tts_code_one)
        }
    }

    fun shareTargetText(view: View) {
            val shareTextIntent = Intent(android.content.Intent.ACTION_SEND)
            shareTextIntent.type = "text/plain"
            shareTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, mTranslationText.text)
            try {
                startActivity(Intent.createChooser(shareTextIntent, getString(R.string.share_with)))
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "Caught ActivityNotFoundException when trying to share")
            }
    }

    fun copyTargetText(view: View) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(mTranslationText.text, mTranslationText.text)

        try {
            clipboard.primaryClip = clip
            Toast.makeText(this, getString(R.string.text_copied), Toast.LENGTH_SHORT).show()
        } catch (e: NullPointerException) {
            Log.w(TAG, "Clipboard bug, e")
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Clipboard bug, e")
        } catch (e: SecurityException) {
            Log.w(TAG, "Clipboard bug, e")
        }
    }

    private fun handleInboundIntent(intent: Intent?): String {
        val action = intent?.action
        if (action == Intent.ACTION_SEND && intent.type != null && "text/plain" == intent.type) {
            // Handle plain text without further information
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null) {
                return text
            }
        } else if (action == Intent.ACTION_PROCESS_TEXT
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            if (text != null) {
                return text.toString()
            }
        }
        return ""
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /** Saves the versionCode of this app when first installed. */
    private fun saveFirstVersionInfo() {
        val firstVersion = mSharedPrefs.getInt(PREF_KEY_FIRST_VERSION, -1)
        if (firstVersion == -1) {
            val versionCode = packageManager.getPackageInfo(packageName, 0).versionCode;
            val editor = mSharedPrefs.edit()
            editor.putInt(PREF_KEY_FIRST_VERSION, versionCode).apply()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private val ABOUT_ACTIVITY_REQUEST_CODE = 1

        private val TEXT_TO_SPEECH_FRAGMENT_TAG = "text_to_speech_fragment_tag"
        private val TRANSLATOR_FRAGMENT_TAG = "translator_fragment_tag"

        private val TRANSLATION_MODE_KEY = "translation_mode_key"
        private val TRANSLATION_VISIBILITY_KEY = "translation_visibility_key"
        private val TRANSLATION_TEXT_KEY = "translation_text_key"
        private val IS_TRANSLATION_IN_PROGRESS_KEY = "is_translation_in_progress_key"

        private val PREF_KEY_FIRST_VERSION = "key_first_version"
        private val PREF_KEY_MODE = "key_mode"
    }
}
