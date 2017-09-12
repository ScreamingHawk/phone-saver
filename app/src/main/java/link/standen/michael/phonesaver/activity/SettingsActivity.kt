package link.standen.michael.phonesaver.activity

import android.annotation.TargetApi
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.view.MenuItem
import link.standen.michael.phonesaver.R

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html) for design
 * guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity: AppCompatPreferenceActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		fragmentManager.beginTransaction().replace(android.R.id.content, PhoneSaverPreferenceFragment()).commit()
	}

	/**
	 * {@inheritDoc}
	 */
	override fun onIsMultiPane(): Boolean {
		return resources.configuration.screenLayout and
				Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
	}

	/**
	 * This method stops fragment injection in malicious applications.
	 * Make sure to deny any unknown fragments here.
	 */
	override fun isValidFragment(fragmentName: String): Boolean {
		return PreferenceFragment::class.java.name == fragmentName ||
				PhoneSaverPreferenceFragment::class.java.name == fragmentName
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	class PhoneSaverPreferenceFragment: PreferenceFragment() {
		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			addPreferencesFromResource(R.xml.preferences)
			setHasOptionsMenu(true)

			// Bind the summaries of file exists preferences to their value summary.
			val fileExistsPref = findPreference("file_exists")
			fileExistsPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
				updateListPreferenceSummary(R.array.pref_list_description_file_exists, preference, newValue.toString())
			}
			updateListPreferenceSummary(R.array.pref_list_description_file_exists, fileExistsPref)
		}

		override fun onOptionsItemSelected(item: MenuItem): Boolean {
			val id = item.itemId
			if (id == android.R.id.home) {
				activity.onBackPressed()
				return true
			}
			return super.onOptionsItemSelected(item)
		}

		private fun updateListPreferenceSummary(descriptionArray: Int, preference: Preference, newValue: String? = null): Boolean {
			with (preference as ListPreference) {
				val index = findIndexOfValue(newValue?: value.toString())
				// Set the summary to reflect the new value.
				summary = if (index >= 0) context.resources.getStringArray(descriptionArray)[index] else null
			}
			return true
		}
	}
}
