package link.standen.michael.phonesaver.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import link.standen.michael.phonesaver.R

/**
 * An activity to handle saving files.
 * https://developer.android.com/training/sharing/receive.html
 */
class SaverActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.saver_activity)

		// Get intent, action and MIME type
		val action: String? = intent.action
		val type: String? = intent.type

		var done = false

		type?.let {
			if (Intent.ACTION_SEND == action) {
				if ("text/plain" == type) {
					// Handle text being sent
					handleSendText(intent)
					done = true
				} else if (type.startsWith("image/")) {
					// Handle single image being sent
					handleSendImage(intent)
					done = true
				}
			} else if (Intent.ACTION_SEND_MULTIPLE == action) {
				if (type.startsWith("image/")) {
					// Handle multiple images being sent
					handleSendMultipleImages(intent)
					done = true
				}
			} else {
				// Handle other intents, such as being started from the home screen
			}
		}

		// Notify user
		if (done){
			Toast.makeText(this, R.string.toast_save_successful, Toast.LENGTH_SHORT).show()
		} else {
			Toast.makeText(this, R.string.toast_save_failed, Toast.LENGTH_SHORT).show()
		}

		// Kill everything
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			finishAndRemoveTask()
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			finishAffinity()
		} else {
			finish()
		}
	}

	fun handleSendText(intent: Intent) {
		intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
			//it.
			Toast.makeText(this, "Got it!", Toast.LENGTH_LONG).show()
		}
	}

	fun handleSendImage(intent: Intent) {
		intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {

			Toast.makeText(this, "Got it!", Toast.LENGTH_LONG).show()
		}
	}

	fun handleSendMultipleImages(intent: Intent) {
		val imageUris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
		imageUris?.let {

			Toast.makeText(this, "Got it!", Toast.LENGTH_LONG).show()
		}
	}
}
