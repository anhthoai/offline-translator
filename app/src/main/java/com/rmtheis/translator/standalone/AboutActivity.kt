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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import de.psdev.licensesdialog.LicensesDialog
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense20
import de.psdev.licensesdialog.model.Notice
import de.psdev.licensesdialog.model.Notices


class AboutActivity : Activity(), BillingFragment.BillingListener {

    private var mIsBillingAvailable: Boolean = false
    private var mIsAdvertisingDisabled: Boolean = false

    private var mBillingFragment: BillingFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        if (savedInstanceState == null) {
            mIsBillingAvailable = intent.getBooleanExtra(EXTRA_KEY_IS_BILLING_AVAILABLE, false)
            mIsAdvertisingDisabled = intent.getBooleanExtra(EXTRA_KEY_IS_ADVERTISING_DISABLED, false)
        } else {
            mIsBillingAvailable = savedInstanceState.getBoolean(IS_BILLING_AVAILABLE_TAG)
            mIsAdvertisingDisabled = savedInstanceState.getBoolean(IS_ADVERTISING_DISABLED_TAG)
        }

        // Lazily instantiate and attach our retained fragment for handling Google Play billing.
        // This fragment does not have a UI, and does not receive a call to onCreateView().
        mBillingFragment = fragmentManager.findFragmentByTag(BILLING_FRAGMENT_TAG)
                as BillingFragment?
        if (mBillingFragment == null) {
            mBillingFragment = BillingFragment.newInstance()
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.add(mBillingFragment, BILLING_FRAGMENT_TAG)
            fragmentTransaction.commit()
        }

        // Handle "Remove ads"
        if (!mIsAdvertisingDisabled && mIsBillingAvailable && resources.getBoolean(R.bool.show_ads)) {
            // User hasn't done in-app purchase already
            val removeAdsTextView = findViewById<LinearLayout>(R.id.remove_ads)
            removeAdsTextView.visibility = View.VISIBLE
            removeAdsTextView.setOnClickListener {
                Log.e("About", "clicked")
                mBillingFragment!!.startBuy()
            }
        }

        val sourceCode = findViewById<TextView>(R.id.source_code) as TextView
        sourceCode.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_CODE_URL)))
        }

        val licensesHeader = this.findViewById<TextView>(R.id.licenses_header) as TextView
        licensesHeader.setOnClickListener {
            val notices = Notices()

            notices.addNotice(Notice("lttoolbox-java",
                    "http://wiki.apertium.org/wiki/Lttoolbox-java",
                    "", GnuGeneralPublicLicense20()))

            notices.addNotice(Notice("material-design-icons",
                    "https://github.com/google/material-design-icons",
                    "", ApacheSoftwareLicense20()))

            LicensesDialog.Builder(this)
                    .setNotices(notices)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show()
        }

        val badge = findViewById<ImageButton>(R.id.googlePlayBadge)
        badge.setOnClickListener {
            linkToMarket()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(AdsActivity.IS_BILLING_AVAILABLE_TAG, mIsBillingAvailable)
        outState?.putBoolean(AdsActivity.IS_ADVERTISING_DISABLED_TAG, mIsAdvertisingDisabled)
    }

    override fun setBillingAvailable(available: Boolean) {
        mIsBillingAvailable = available
    }

    override fun setAdvertisingDisabled(disabled: Boolean) {
        Log.e("AboutActivity", "setAdvertisingDisabled() disabled=" + disabled)

        mIsAdvertisingDisabled = disabled

        // Cache as a preference value so don't need to check billing service in the future,
        // or revert setting to value retrieved successfully from Google Play.
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putBoolean(AdsActivity.SHOW_ADS_PREFERENCE_KEY, !disabled).apply()
    }

    private fun linkToMarket() {
        val utmSource = if (BuildConfig.DEBUG) "app" else "app-debug"
        val tracker = "&referrer=utm_source%3D$utmSource%26utm_medium%3Dabout-app-link"

        var marketLaunchFailed = false
        var uri = Uri.parse("market://details?id=" + packageName + tracker)
        var intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (anfe: ActivityNotFoundException) {
            marketLaunchFailed = true
        }

        if (marketLaunchFailed) {
            try {
                // Hmm, market is not installed, so try opening using browser
                uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName
                        + tracker)
                intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (anfe: ActivityNotFoundException) {
                Log.e(TAG, "Could not launch Google Play app or website")
            }
        }
    }

    companion object {
        private val TAG = AboutActivity::class.java.simpleName

        val BILLING_FRAGMENT_TAG = "billing_fragment_tag"

        private val IS_BILLING_AVAILABLE_TAG = "is_billing_available_tag"
        private val IS_ADVERTISING_DISABLED_TAG = "is_advertising_disabled_tag"

        val EXTRA_KEY_IS_BILLING_AVAILABLE = "extra_key_is_billing_available"
        val EXTRA_KEY_IS_ADVERTISING_DISABLED = "extra_key_is_advertising_disabled"

        private val SOURCE_CODE_URL = "https://www.github.com/rmtheis/offline-translator"
    }
}
