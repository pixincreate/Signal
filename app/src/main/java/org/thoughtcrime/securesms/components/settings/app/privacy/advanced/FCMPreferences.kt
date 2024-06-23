/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.privacy.advanced

//-----------------------------------------------------------------------------
// JW: added
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.whispersystems.signalservice.api.SignalServiceAccountManager
import java.io.IOException
import java.util.Optional
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import org.thoughtcrime.securesms.jobs.FcmRefreshJob
import org.thoughtcrime.securesms.jobs.RefreshAttributesJob
import org.thoughtcrime.securesms.messages.IncomingMessageObserver
import org.thoughtcrime.securesms.util.PlayServicesUtil

//-----------------------------------------------------------------------------
object FCMPreferenceFunctions {
  private val TAG: String = Log.tag(FCMPreferenceFunctions::class.java)

  fun cleanGcmId(context: Context?) {
    try {
      val accountManager: SignalServiceAccountManager = AppDependencies.getSignalServiceAccountManager()
      accountManager.setGcmId(Optional.< String > empty < String ? > ())
    } catch (e: IOException) {
      Log.w(TAG, e.getMessage())
      Toast.makeText(context, R.string.ApplicationPreferencesActivity_error_connecting_to_server, Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
      Log.w(TAG, e.getMessage())
      Toast.makeText(context, "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show()
    }
  }

  fun exitAndRestart(context: Context) {
    // JW: Restart after OK press
    val builder: AlertDialog.Builder = Builder(context)
    builder.setMessage(context.getString(R.string.preferences_advanced__need_to_restart))
      .setCancelable(false)
      .setPositiveButton(context.getString(R.string.ImportFragment_restore_ok), object : OnClickListener() {
        fun onClick(dialog: DialogInterface?, id: Int) {
          restartApp(context)
        }
      })
    val alert: AlertDialog = builder.create()
    alert.show()
  }

  // Create a pending intent to restart Signal
  fun restartApp(context: Context?) {
    try {
      if (context != null) {
        val pm: PackageManager = context.getPackageManager()

        if (pm != null) {
          val startActivity: Intent = pm.getLaunchIntentForPackage(context.getPackageName())
          if (startActivity != null) {
            startActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntentId = 223344
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT)
            val mgr: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
            System.exit(0)
          } else {
            Log.e(TAG, "restartApp: unable to restart application, startActivity == null")
            System.exit(0)
          }
        } else {
          Log.e(TAG, "restartApp: unable to restart application, Package manager == null")
          System.exit(0)
        }
      } else {
        Log.e(TAG, "restartApp: unable to restart application, Context == null")
        System.exit(0)
      }
    } catch (e: Exception) {
      Log.e(TAG, "restartApp: unable to restart application: " + e.getMessage())
      System.exit(0)
    }
  }

  fun onFCMPreferenceChange(context: Context, isFcm: Boolean) {
    if (isFcm) {
      val status: PlayServicesUtil.PlayServicesStatus = PlayServicesUtil.getPlayServicesStatus(context)

      if (status === PlayServicesUtil.PlayServicesStatus.SUCCESS) {
        SignalStore.account().setFcmEnabled(true)
        Toast.makeText(context, "Setting setFcmDisabled to false", Toast.LENGTH_LONG).show()
        AppDependencies.getJobManager().startChain(FcmRefreshJob())
          .then(RefreshAttributesJob())
          .enqueue()

        context.stopService(Intent(context, IncomingMessageObserver.ForegroundService::class.java))

        Log.i(TAG, "onFCMPreferenceChange: enabled fcm")
        exitAndRestart(context)
      } else {
        // No Play Services found
        Toast.makeText(context, R.string.preferences_advanced__play_services_not_found, Toast.LENGTH_LONG).show()
      }
    } else { // switch to websockets
      SignalStore.account().setFcmEnabled(false)
      SignalStore.account().setFcmToken(null)
      cleanGcmId(context)
      AppDependencies.getJobManager().add(RefreshAttributesJob())
      Log.i(TAG, "onFCMPreferenceChange: disabled fcm")
      Toast.makeText(context, "Switching to Websockets", Toast.LENGTH_LONG).show()
      exitAndRestart(context)
    }
  }
}
