package link.standen.michael.phonesaver.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import link.standen.michael.phonesaver.R
import link.standen.michael.phonesaver.util.LocationHelper
import android.provider.OpenableColumns
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException

/**
 * An activity to handle saving files.
 * https://developer.android.com/training/sharing/receive.html
 */
class SaverActivity : AppCompatActivity() {

	private val TAG = "SaverActivity"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.saver_activity)

		// Get intent, action and MIME type
		val action: String? = intent.action
		val type: String? = intent.type

		var done = false

		type?.let {
			if (Intent.ACTION_SEND == action) {
				if (type.startsWith("image/")) {
					// Handle single image being sent
					done = handleSendImage()
				}
			} else if (Intent.ACTION_SEND_MULTIPLE == action) {
				if (type.startsWith("image/")) {
					// Handle multiple images being sent
					done = handleSendMultipleImages()
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

	fun handleSendImage(): Boolean {
		intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
			return saveUri(it, getFilename(it), getLocation())
		}
		return false
	}

	fun handleSendMultipleImages(): Boolean {
		val imageUris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
		imageUris?.let {
			val location = getLocation()
			var success = true
			imageUris.forEach {
				success = success && saveUri(it, getFilename(it), location)
			}
			return success
		}
		return false
	}

	/**
	 * Get the location to save the file at
	 */
	fun getLocation(): String? {
		LocationHelper.loadFolderList(this)?.let {
			//FIXME User select from the list
			return LocationHelper.addRoot(it[0])
		}
		return null
	}

	/**
	 * Save the given uri to file
	 */
	fun saveUri(uri: Uri, filename: String, location: String?): Boolean {
		var success = false

		location?.let {
			val sourceFilename = uri.path
			val destinationFilename = location + File.separatorChar + filename

			Log.d(TAG, "Saving $sourceFilename to $destinationFilename")

			var bis: InputStream? = null
			var bos: BufferedOutputStream? = null

			try {
				val fout = File(destinationFilename)
				if (!fout.exists()){
					fout.createNewFile()
				}
				bis = contentResolver.openInputStream(uri)
				bos = BufferedOutputStream(FileOutputStream(fout, false))
				val buf = ByteArray(1024)
				bis.read(buf)
				do {
					bos.write(buf)
				} while (bis.read(buf) != -1)

				// Done
				success = true
			} catch (e: IOException) {
				Log.e(TAG, "Unable to save file", e)
			} finally {
				try {
					bis?.close()
					bos?.close()
				} catch (e: IOException) {
					Log.e(TAG, "Unable to close stream", e)
				}
			}
		}

		return success
	}

	private fun getFilename(uri: Uri): String {
		var result: String? = null
		if (uri.scheme == "content") {
			contentResolver.query(uri, null, null, null, null)?.use {
				if (it.moveToFirst()) {
					result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
				}
			}
		}

		// Default to last path if null
		result = result?: uri.lastPathSegment

		return result!!
	}
}
