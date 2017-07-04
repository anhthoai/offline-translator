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

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

open class AdsActivity : AppCompatActivity(), BillingFragment.BillingListener  {

    protected var mAdView: AdView? = null
    private var mStartedShowingAds: Boolean = false

    private var mBillingFragment: BillingFragment? = null
    protected var mIsBillingAvailable: Boolean = false
    protected var mIsAdvertisingDisabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AdRequest.Builder().addTestDevice("B5B48E895797CA5A93E24D119E45445B")

        if (mAdView == null) {
            MobileAds.initialize(applicationContext, getString(R.string.admob_app_id));
        }

        if (savedInstanceState == null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            if (!prefs.getBoolean(SHOW_ADS_PREFERENCE_KEY, true)) {
                mIsAdvertisingDisabled = true
            }
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
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(IS_BILLING_AVAILABLE_TAG, mIsBillingAvailable)
        outState?.putBoolean(IS_ADVERTISING_DISABLED_TAG, mIsAdvertisingDisabled)
    }

    override fun setBillingAvailable(available: Boolean) {
        mIsBillingAvailable = available
    }

    override fun setAdvertisingDisabled(disabled: Boolean) {
        Log.e("AdsActivity", "setAdvertisingDisabled() disabled=" + disabled)

        mIsAdvertisingDisabled = disabled

        // Cache as a preference value so don't need to check billing service in the future,
        // or revert setting to value retrieved successfully from Google Play.
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putBoolean(AdsActivity.SHOW_ADS_PREFERENCE_KEY, !disabled).apply()
    }

    protected fun startShowingAds() {
        if (mIsAdvertisingDisabled || !resources.getBoolean(R.bool.show_ads)) {
            mAdView?.visibility = View.GONE
            return
        }
        if (mAdView == null) {
            mAdView = findViewById(R.id.adView) as AdView
            mAdView?.loadAd(AdRequest.Builder().build())

            mAdView?.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.i(TAG, "onAdLoaded")
                    // Ads need a width of 411 dp to show ad, so check this for reduced_layout
                    if (findViewById(R.id.reduced_layout) == null) {
                        showAdView(true)
                    } else {
                        // Can't directly measure adView width, so measure sibling layout width
                        val dpWidth = findViewById(R.id.primary_layout).width /
                                resources.displayMetrics.density
                        if (dpWidth > 411) {
                            showAdView(true)
                        }
                    }
                }

                override fun onAdFailedToLoad(errorCode: Int) {
                    // Code to be executed when an ad request fails.
                    Log.i(TAG, "onAdFailedToLoad")
                    showAdView(false)
                }

                override fun onAdOpened() {
                    // Code to be executed when an ad opens an overlay that covers the screen.
                }

                override fun onAdLeftApplication() {
                    // Code to be executed when the user has left the app.
                }

                override fun onAdClosed() {
                    // Code to be executed when when the user is about to return
                    // to the app after tapping on an ad.
                }
            }
        }
    }

    fun showAdView(show: Boolean) {
        mAdView?.visibility = if (show && !BuildConfig.DEBUG) View.VISIBLE else View.GONE
    }

    companion object {
        private val TAG = AdsActivity::class.java.simpleName

        val SHOW_ADS_PREFERENCE_KEY = "show_ads_preference_key"

        private val BILLING_FRAGMENT_TAG = "billing_fragment_tag"

        val IS_BILLING_AVAILABLE_TAG = "is_billing_available_tag"
        val IS_ADVERTISING_DISABLED_TAG = "is_advertising_disabled_tag"
    }
}
