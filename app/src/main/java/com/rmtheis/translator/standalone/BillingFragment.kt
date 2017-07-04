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
import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingFlowParams

class BillingFragment : Fragment() {

    var mCallback: BillingListener? = null
    private var mBillingClient: BillingClient? = null

    interface BillingListener {
        fun setAdvertisingDisabled(disabled: Boolean)
        fun setBillingAvailable(available: Boolean)
    }

    companion object {
        fun newInstance(): BillingFragment {
            return BillingFragment()
        }

        private val TAG = BillingFragment::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        connectToBillingService()
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)

        try {
            mCallback = activity as BillingFragment.BillingListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity!!.toString() + " must implement callback interface")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mCallback = null
    }

    private fun connectToBillingService() {
        if (mBillingClient == null && activity != null) {
            Log.d(TAG, "Creating BillingClient...")
            mBillingClient = BillingClient.Builder(activity).setListener(object : PurchasesUpdatedListener {
                override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
                    if (purchases != null) {
                        checkPurchasedItemsList(purchases)
                    }
                }
            }).build()
        }
        if (!mBillingClient!!.isReady) {
            Log.d(TAG, "Connecting to billing service...")
            (mBillingClient as BillingClient).startConnection(object : BillingClientStateListener {

                override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponse: Int) {
                    Log.d(TAG, "Connected. Billing setup finished.")
                    if (billingResponse == BillingClient.BillingResponse.OK) {
                        mCallback?.setBillingAvailable(true)
                        checkPurchasedItems()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "The connection to the billing service was lost.")
                    mCallback?.setBillingAvailable(false)
                }
            })
        } else {
            mCallback?.setBillingAvailable(true)
        }
    }

    private fun checkPurchasedItems() {
        Log.d(TAG, "Checking for purchased items...")
        val purchasesResult = mBillingClient?.queryPurchases(BillingClient.SkuType.INAPP)
        val purchases = purchasesResult?.purchasesList ?: emptyList()
        checkPurchasedItemsList(purchases)
    }

    private fun checkPurchasedItemsList(purchases: MutableList<Purchase>) {
        Log.d(TAG, "purchases.size=" + purchases.size)
        for (purchase in purchases) {
            // Look for any purchase that's not canceled or refunded
            Log.d(TAG, "purchase=" + purchase)
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                mCallback?.setAdvertisingDisabled(true)
                return
            }
        }
        Log.e(TAG, "No qualifying purchases found. Not disabling advertising.")
        mCallback?.setAdvertisingDisabled(false)
    }

    /**
     * Connect to the billing service and show the buy prompt when ready.
     */
    fun startBuy() {
        if (mBillingClient == null && activity != null) {
            Log.d(TAG, "startBuy(): Creating BillingClient...")
            mBillingClient = BillingClient.Builder(activity).setListener(object : PurchasesUpdatedListener {
                override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
                    if (purchases != null) {
                        checkPurchasedItemsList(purchases)
                    }
                }
            }).build()
        }
        if (!mBillingClient!!.isReady) {
            Log.d(TAG, "startBuy(): Connecting to billing service...")
            (mBillingClient as BillingClient).startConnection(object : BillingClientStateListener {

                override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponse: Int) {
                    Log.d(TAG, "startBuy(): Connected. Billing setup finished.")
                    if (billingResponse == BillingClient.BillingResponse.OK) {
                        showBuyPrompt()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "startBuy(): The connection to the billing service was lost.")
                    mCallback?.setBillingAvailable(false)
                }
            })
        } else {
            showBuyPrompt()
        }
    }

    private fun showBuyPrompt() {
        val skuId = activity?.getString(R.string.in_app_purchase_sku_id)
        val builder = BillingFlowParams.Builder().setSku(skuId).setType(SkuType.INAPP)
        val responseCode = mBillingClient?.launchBillingFlow(activity, builder.build())
        if (responseCode != BillingClient.BillingResponse.OK && activity != null) {
            val error: String
            when (responseCode) {
                3 -> error = "BILLING_UNAVAILABLE"
                5 -> error = "DEVELOPER_ERROR"
                6 -> error = "ERROR"
                -2 -> error = "FEATURE_NOT_SUPPORTED"
                7 -> error = "ITEM_ALREADY_OWNED"
                8 -> error = "ITEM_UNAVAILABLE"
                0 -> error = "OK"
                -1 -> error = "SERVICE_DISCONNECTED"
                2 -> error = "SERVICE_UNAVAILABLE"
                1 -> error = "USER_CANCELED"
                else -> error = "Unknown"
            }
            Log.e(TAG, "Billing flow not launched. responseCode=" + error)
            Toast.makeText(activity, R.string.error_billing_flow_launch_failed, Toast.LENGTH_LONG)
                    .show()
        }
    }
}
