package link.standen.michael.phonesaver.util

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import link.standen.michael.phonesaver.R

/**
 * A log wrapper that also logs to the user.
 * logLevel: 0 = None, 1 = Error, 2 = Warn, 3 = Info, 4 = Debug
 */
class DebugLogger(private var TAG: String, private var CONTEXT: Context) {

	private val logLevel = CONTEXT.resources.getStringArray(R.array.pref_list_values_log_to_user).indexOf(
			PreferenceManager.getDefaultSharedPreferences(CONTEXT).getString(
					"log_to_user", CONTEXT.resources.getString(R.string.pref_default_value_log_to_user)))

	fun v(msg: String){
		Log.v(TAG, msg)
		if (logLevel >= 5){
			Toast.makeText(CONTEXT, "VERBOSE: $msg", Toast.LENGTH_SHORT).show()
		}
	}

	fun d(msg: String){
		Log.d(TAG, msg)
		if (logLevel >= 4){
			Toast.makeText(CONTEXT, "DEBUG: $msg", Toast.LENGTH_SHORT).show()
		}
	}

	fun i(msg: String){
		Log.i(TAG, msg)
		if (logLevel >= 3){
			Toast.makeText(CONTEXT, "INFO: $msg", Toast.LENGTH_SHORT).show()
		}
	}

	fun w(msg: String){
		Log.w(TAG, msg)
		if (logLevel >= 2){
			Toast.makeText(CONTEXT, "WARN: $msg", Toast.LENGTH_SHORT).show()
		}
	}

	fun e(msg: String, e: Exception? = null){
		Log.e(TAG, msg, e)
		if (logLevel >= 1){
			Toast.makeText(CONTEXT, "ERROR: $msg", Toast.LENGTH_SHORT).show()
			e?.let {
				Toast.makeText(CONTEXT, "ERROR: ${it.message}", Toast.LENGTH_SHORT).show()
			}
		}
	}
}
