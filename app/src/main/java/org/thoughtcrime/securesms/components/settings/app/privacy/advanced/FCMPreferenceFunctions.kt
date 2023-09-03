package org.thoughtcrime.securesms.components.settings.app.privacy.advanced
//-----------------------------------------------------------------------------
// JW: added
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.thoughtcrime.securesms.jobs.FcmRefreshJob
import org.thoughtcrime.securesms.jobs.RefreshAttributesJob
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.messages.IncomingMessageObserver.ForegroundService
import org.thoughtcrime.securesms.util.PlayServicesUtil
import org.thoughtcrime.securesms.util.PlayServicesUtil.PlayServicesStatus
import java.io.IOException
import java.util.Optional
import kotlin.system.exitProcess

//-----------------------------------------------------------------------------
object FCMPreferenceFunctions {
  private val TAG = Log.tag(FCMPreferenceFunctions::class.java)
  private fun cleanGcmId(context: Context?) {
    try {
      val accountManager = ApplicationDependencies.getSignalServiceAccountManager()
      accountManager.setGcmId(Optional.empty())
    } catch (e: IOException) {
      Log.w(TAG, e.message)
      Toast.makeText(context, R.string.ApplicationPreferencesActivity_error_connecting_to_server, Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
      Log.w(TAG, e.message)
      Toast.makeText(context, "Exception: " + e.message, Toast.LENGTH_LONG).show()
    }
  }

  private fun exitAndRestart(context: Context) {
    // JW: Restart after OK press
    val builder = AlertDialog.Builder(context)
    builder.setMessage(context.getString(R.string.preferences_advanced__need_to_restart))
      .setCancelable(false)
      .setPositiveButton(context.getString(R.string.ImportFragment_restore_ok)) { dialog, id -> restartApp(context) }
    val alert = builder.create()
    alert.show()
  }

  // Create a pending intent to restart Signal
  private fun restartApp(context: Context?) {
    try {
      if (context != null) {
        val pm = context.packageManager
        if (pm != null) {
          val startActivity = pm.getLaunchIntentForPackage(context.packageName)
          if (startActivity != null) {
            startActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntentId = 223344
            val pendingIntent = PendingIntent.getActivity(context, pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT)
            val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr[AlarmManager.RTC, System.currentTimeMillis() + 100] = pendingIntent
            exitProcess(0)
          } else {
            Log.e(TAG, "restartApp: unable to restart application, startActivity == null")
            exitProcess(0)
          }
        } else {
          Log.e(TAG, "restartApp: unable to restart application, Package manager == null")
          exitProcess(0)
        }
      } else {
        Log.e(TAG, "restartApp: unable to restart application, Context == null")
        exitProcess(0)
      }
    } catch (e: Exception) {
      Log.e(TAG, "restartApp: unable to restart application: " + e.message)
      exitProcess(0)
    }
  }

  fun onFCMPreferenceChange(context: Context, isFcm: Boolean) {
    if (isFcm) {
      val status = PlayServicesUtil.getPlayServicesStatus(context)
      if (status == PlayServicesStatus.SUCCESS) {
        SignalStore.account().fcmEnabled = true
        //TextSecurePreferences.setWebsocketRegistered(context, false);
        Toast.makeText(context, "Setting setFcmDisabled to false", Toast.LENGTH_LONG).show()
        ApplicationDependencies.getJobManager().startChain(FcmRefreshJob())
          .then(RefreshAttributesJob())
          .enqueue()
        context.stopService(Intent(context, ForegroundService::class.java))
        Log.i(TAG, "onFCMPreferenceChange: enabled fcm")
        exitAndRestart(context)
      } else {
        // No Play Services found
        Toast.makeText(context, R.string.preferences_advanced__play_services_not_found, Toast.LENGTH_LONG).show()
      }
    } else { // switch to websockets
      SignalStore.account().fcmEnabled = false
      //TextSecurePreferences.setWebsocketRegistered(context, true);
      SignalStore.account().fcmToken = null
      cleanGcmId(context)
      ApplicationDependencies.getJobManager().add(RefreshAttributesJob())
      Log.i(TAG, "onFCMPreferenceChange: disabled fcm")
      Toast.makeText(context, "Switching to Websockets", Toast.LENGTH_LONG).show()
      exitAndRestart(context)
    }
  }
}