package link.standen.michael.phonesaver.activity

import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView

import link.standen.michael.phonesaver.R
import java.util.Locale
import android.os.Build
import android.view.View
import link.standen.michael.phonesaver.util.DebugLogger

/**
 * Credits activity.
 */
class CreditsActivity : AppCompatActivity() {

	private val TAG = "CreditsActivity"
	private val DEFAULT_LOCALE = Locale("en").language

	private var log: DebugLogger? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.credits_activity)

		log = DebugLogger(TAG, this)

		// Version
		try {
			val versionName = packageManager.getPackageInfo(packageName, 0).versionName
			findViewById<TextView>(R.id.credits_version).text = resources.getString(R.string.credits_version, versionName)
		} catch (e: PackageManager.NameNotFoundException) {
			log!!.e("Unable to get package version", e)
		}

		// Linkify
		findViewById<TextView>(R.id.credits_creator).movementMethod = LinkMovementMethod.getInstance()
		findViewById<TextView>(R.id.credits_content1).movementMethod = LinkMovementMethod.getInstance()
		findViewById<TextView>(R.id.credits_content2).movementMethod = LinkMovementMethod.getInstance()
		findViewById<TextView>(R.id.credits_content3).movementMethod = LinkMovementMethod.getInstance()
		if (getCurrentLocale().language == DEFAULT_LOCALE){
			// English, hide the translator info
			findViewById<TextView>(R.id.credits_content_translator).visibility = View.GONE
		} else {
			findViewById<TextView>(R.id.credits_content_translator).movementMethod = LinkMovementMethod.getInstance()
		}
	}

	/**
	 * A version safe way to get the currently applied locale.
	 */
	private fun getCurrentLocale(): Locale =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			this.resources.configuration.locales.get(0)
		} else {
			@Suppress("DEPRECATION")
			this.resources.configuration.locale
		}
}
