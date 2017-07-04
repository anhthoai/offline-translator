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

import android.view.View

/**
 * Processes text shared through custom text selection actions.
 *
 * Taps that are outside the area handled by the included inner layout close the window:
 * - taps within the padding of the layout initiate a call to finishActivity()
 * - taps outside the window finish the activity because setFinishOnTouchOutside is set to true
 */
class ProcessTextActivity : MainActivity() {

    override fun assignContentView() {
        setContentView(R.layout.reduced)
        setFinishOnTouchOutside(true)
    }

    fun finishActivity(view: View) {
        if (!isFinishing) {
            finish()
        }
    }

}
