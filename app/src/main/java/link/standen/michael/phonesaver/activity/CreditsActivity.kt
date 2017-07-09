package link.standen.michael.phonesaver.activity

import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView

import link.standen.michael.phonesaver.R
import java.util.*
import android.os.Build
import android.view.View

/**
 * Credits activity.
 */
class CreditsActivity : AppCompatActivity() {

	private val TAG = "CreditsActivity"
	private val DEFAULT_LOCALE = Locale("en").language

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.credits_activity)

		// Version
		try {
			val versionName = packageManager.getPackageInfo(packageName, 0).versionName
			(findViewById(R.id.credits_version) as TextView).text = resources.getString(R.string.credits_version, versionName)
		} catch (e: PackageManager.NameNotFoundException) {
			Log.e(TAG, "Unable to get package version", e)
		}

		// Linkify
		(findViewById(R.id.credits_creator) as TextView).movementMethod = LinkMovementMethod.getInstance()
		(findViewById(R.id.credits_content1) as TextView).movementMethod = LinkMovementMethod.getInstance()
		(findViewById(R.id.credits_content2) as TextView).movementMethod = LinkMovementMethod.getInstance()
		(findViewById(R.id.credits_content3) as TextView).movementMethod = LinkMovementMethod.getInstance()
		if (getCurrentLocale().language == DEFAULT_LOCALE){
			// English, hide the translator info
			(findViewById(R.id.credits_content_translator) as TextView).visibility = View.GONE
		} else {
			(findViewById(R.id.credits_content_translator) as TextView).movementMethod = LinkMovementMethod.getInstance()
		}
	}

	/**
	 * A version safe way to get the currently applied locale.
	 */
	fun getCurrentLocale(): Locale {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return this.resources.configuration.locales.get(0)
		} else {
			return this.resources.configuration.locale
		}
	}
}
