package link.standen.michael.phonesaver.util

import android.app.Activity
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import link.standen.michael.phonesaver.R
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * A log wrapper that also logs to the user.
 * logLevel: 0 = None, 1 = Error, 2 = Warn, 3 = Info, 4 = Debug, 5 = Verbose
 */
class DebugLogger(private val context: Context, forceTag: String? = null) {

	private val tag = forceTag?.let {
		it
	}?: context::class.java.simpleName

	private val logLevel = context.resources.getStringArray(R.array.pref_list_values_log_to_user).indexOf(
			PreferenceManager.getDefaultSharedPreferences(context).getString(
					"log_to_user", context.resources.getString(R.string.pref_default_value_log_to_user)))

	companion object {
		private var EXECUTOR: ScheduledExecutorService? = null
		private val TOAST_QUEUE: Queue<Runnable> = ConcurrentLinkedQueue()
	}

	init {
		if (EXECUTOR == null){
			EXECUTOR = Executors.newSingleThreadScheduledExecutor()
			EXECUTOR?.scheduleAtFixedRate({
				TOAST_QUEUE.poll()?.run()
			}, 0, 100, TimeUnit.MILLISECONDS)
		}
	}

	fun v(msg: String){
		Log.v(tag, msg)
		if (logLevel >= 5){
			makeToast("VERBOSE: $msg")
		}
	}

	fun d(msg: String){
		Log.d(tag, msg)
		if (logLevel >= 4){
			makeToast("DEBUG: $msg")
		}
	}

	fun i(msg: String){
		Log.i(tag, msg)
		if (logLevel >= 3){
			makeToast("INFO: $msg")
		}
	}

	fun w(msg: String){
		Log.w(tag, msg)
		if (logLevel >= 2){
			makeToast("WARN: $msg")
		}
	}

	fun e(msg: String, e: Exception? = null){
		Log.e(tag, msg, e)
		if (logLevel >= 1){
			makeToast("ERROR: $msg")
			e?.let {
				makeToast("ERROR: ${it.message}")
			}
		}
	}

	private fun makeToast(msg: String){
		if (EXECUTOR == null){
			Log.e(tag, "No executor for debug toaster")
		} else {
			if (context is Activity) {
				TOAST_QUEUE.offer(Runnable {
					context.runOnUiThread {
						Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
					}
					// Sleep for length of a short toast
					Thread.sleep(2000)
				})
			}
		}
	}
}
