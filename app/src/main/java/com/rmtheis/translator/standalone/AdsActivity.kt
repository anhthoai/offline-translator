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
import android.os.CountDownTimer
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.android.gms.ads.*

open class AdsActivity : AppCompatActivity(), BillingFragment.BillingListener  {

    private var mBannerAdView: AdView? = null
    private var mShouldBeginShowingInterstitialAds: Boolean = false
    private var mInterstitialAd: InterstitialAd? = null

    private var mBillingFragment: BillingFragment? = null
    protected var mIsBillingAvailable: Boolean = false
    protected var mIsAdvertisingDisabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AdRequest.Builder().addTestDevice("B5B48E895797CA5A93E24D119E45445B")

        if (mBannerAdView == null) {
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

    override fun onResume() {
        super.onResume()

        if (shouldShowAds() && mInterstitialAd == null) {
            mInterstitialAd = InterstitialAd(this)
            mInterstitialAd!!.adUnitId = getString(R.string.admob_ad_unit_id_interstitial_ad)
            mInterstitialAd!!.loadAd(AdRequest.Builder().build())

            mInterstitialAd!!.adListener = object : AdListener() {
                override fun onAdClosed() {
                    mInterstitialAd!!.loadAd(AdRequest.Builder().build())
                }
            }

            val timeDurationMs: Long = 30000
            object : CountDownTimer(timeDurationMs, timeDurationMs) {
                override fun onTick(millisUntilFinished: Long) {
                    // Do nothing
                }

                override fun onFinish() {
                    mShouldBeginShowingInterstitialAds = true
                }
            }.start()
        }
        if (mShouldBeginShowingInterstitialAds && mInterstitialAd!!.isLoaded) {
            mInterstitialAd!!.show()
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
        mIsAdvertisingDisabled = disabled

        // Cache as a preference value so don't need to check billing service in the future,
        // or revert setting to value retrieved successfully from Google Play.
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putBoolean(AdsActivity.SHOW_ADS_PREFERENCE_KEY, !disabled).apply()
    }

    private fun shouldShowAds() : Boolean {
        return !mIsAdvertisingDisabled && resources.getBoolean(R.bool.show_ads) &&
                !BuildConfig.DEBUG
    }

    protected fun startShowingAds() {
        if (!shouldShowAds()) {
            mBannerAdView?.visibility = View.GONE
            return
        }
        if (mBannerAdView == null) {
            mBannerAdView = findViewById(R.id.adView)
            mBannerAdView!!.loadAd(AdRequest.Builder().build())

            mBannerAdView!!.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    showBannerAdView(true)
                }
            }
        }
    }

    fun showBannerAdView(show: Boolean) {
        mBannerAdView?.visibility = if (show && !BuildConfig.DEBUG) View.VISIBLE else View.GONE
    }

    companion object {
        private val TAG = AdsActivity::class.java.simpleName

        val SHOW_ADS_PREFERENCE_KEY = "show_ads_preference_key"

        private val BILLING_FRAGMENT_TAG = "billing_fragment_tag"

        val IS_BILLING_AVAILABLE_TAG = "is_billing_available_tag"
        val IS_ADVERTISING_DISABLED_TAG = "is_advertising_disabled_tag"
    }
}
