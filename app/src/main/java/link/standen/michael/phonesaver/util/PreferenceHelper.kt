package link.standen.michael.phonesaver.util

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import link.standen.michael.phonesaver.R

/**
 * A helper class for managing preferences.
 */
class PreferenceHelper(private val context: Context) {

	companion object {
		var saveStrategy = 0
		var locationSelectEnabled = false
		var registerMediaServer = false
		var useLenientRegex = false
		var forceSaving = false
		var specialImgur = false
		var logLevel = 0
	}

	/**
	 * Force loading of the preferences
	 */
	fun loadPreferences() {
		val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

		forceSaving = sharedPrefs.getBoolean("force_saving", false)
		specialImgur = sharedPrefs.getBoolean("special_imgur", false)
		registerMediaServer = sharedPrefs.getBoolean("register_file", false)
		useLenientRegex = sharedPrefs.getBoolean("lenient_regex", false)
		locationSelectEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
				sharedPrefs.getBoolean("location_select", false)
		saveStrategy = context.resources.getStringArray(R.array.pref_list_values_file_exists).indexOf(
				PreferenceManager.getDefaultSharedPreferences(context).getString(
						"file_exists", context.resources.getString(R.string.pref_default_value_file_exists)))
		logLevel = context.resources.getStringArray(R.array.pref_list_values_log_to_user).indexOf(
				PreferenceManager.getDefaultSharedPreferences(context).getString(
						"log_to_user", context.resources.getString(R.string.pref_default_value_log_to_user)))
	}
}
