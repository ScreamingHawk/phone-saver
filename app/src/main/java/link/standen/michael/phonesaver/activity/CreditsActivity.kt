package link.standen.michael.phonesaver.activity

import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView

import link.standen.michael.phonesaver.R

/**
 * Credits activity.
 */
class CreditsActivity : AppCompatActivity() {

	companion object {
		const private val TAG = "CreditsActivity"
	}

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
	}
}
