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

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import dalvik.system.DexClassLoader
import org.apertium.Translator
import org.apertium.pipeline.Program
import java.io.*

/**
 * Retained fragment for handling translation.
 */
class TranslatorFragment : Fragment() {

    var mTranslatedText: String? = null

    private var mListener: TranslationListener? = null
    private var mClassLoader: DexClassLoader? = null
    private var mLastMode: String? = null
    private lateinit var mCurrentMode: String
    private lateinit var mSourceText: String

    interface TranslationListener {
        fun onTranslationCompleted(translatedText: String)
        fun onProgressUpdate(indeterminate: Boolean, progress: Int)
    }

    companion object {
        fun newInstance(): TranslatorFragment {
            return TranslatorFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mListener = activity as TranslationListener
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    fun translate(mode: String, sourceText: String) {
        mCurrentMode = mode
        mSourceText = sourceText
        mTranslatedText = null
        TranslateAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private inner class TranslateAsyncTask : AsyncTask<Void, Int, String>() {
        private var isIndeterminate: Boolean = false

        @Synchronized override fun doInBackground(vararg arg0: Void): String {
            try {
                if (mClassLoader == null) {
                    isIndeterminate = true
                    publishProgress(0)
                    mClassLoader = DexClassLoader(installLanguageData(), context.getDir("outdex",
                            Context.MODE_PRIVATE)?.path, null, javaClass.classLoader)
                    Translator.setBase(mClassLoader)
                    Translator.setCacheEnabled(false)
                    Translator.setDisplayMarks(false)
                    Translator.setDelayedNodeLoadingEnabled(true)
                    isIndeterminate = false
                }
                if (mCurrentMode != mLastMode) {
                    Translator.setMode(mCurrentMode)
                    mLastMode = mCurrentMode
                }
                val input = StringReader(mSourceText)
                val output = StringWriter()
                Translator.translate(input, output, Program("apertium-destxt"),
                        Program("apertium-retxt"), { _, progress, maxProgress ->
                    publishProgress(100 * progress / maxProgress)
                })
                return output.toString()
            } catch (e: OutOfMemoryError) {
                Log.e("TranslatorFragment", "Caught out of memory error", e)
            } catch (e: Exception) {
                Log.e("TranslatorFragment", "Caught exception", e)
            }
            return ""
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            mListener?.onProgressUpdate(isIndeterminate, values[0]!!)
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            mListener?.onTranslationCompleted(result)
            mTranslatedText = result
        }

        /**
         * Copies language data from application assets to device storage.
         */
        private fun installLanguageData(): String {
            val baseDir = context.getDir("apertium", Context.MODE_PRIVATE).toString()
            try {
                for (jarFilename in context.assets.list("")) {
                    if (jarFilename.startsWith("apertium-")) {
                        val fileToInstall = File(baseDir + File.separator + jarFilename)
                        if (!fileToInstall.exists()) {
                            // Remove old version of apertium jar file
                            File(baseDir).listFiles()
                                    .filter { it != fileToInstall && it.name.endsWith(".jar") }
                                    .forEach { it.delete() }

                            // Install new version of apertium jar file
                            fileToInstall.copyInputStreamToFile(BufferedInputStream(
                                    context.assets.open(jarFilename)))
                        }
                        return fileToInstall.toString()
                    }
                }
            } catch (e: IOException) {
                Log.e("TranslatorFragment", "Caught IOException trying to install language data", e)
            }
            return ""
        }

        private fun File.copyInputStreamToFile(inputStream: InputStream) {
            inputStream.use { input ->
                this.outputStream().use { fileOut ->
                    input.copyTo(fileOut)
                }
            }
        }
    }
}
