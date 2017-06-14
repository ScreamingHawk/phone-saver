package link.standen.michael.phonesaver.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import link.standen.michael.phonesaver.R
import link.standen.michael.phonesaver.util.LocationHelper
import android.provider.OpenableColumns
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.*
import java.io.*

/**
 * An activity to handle saving files.
 * https://developer.android.com/training/sharing/receive.html
 */
class SaverActivity : ListActivity() {

	private val TAG = "SaverActivity"

	private var location: String? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.saver_activity)

		if (testSupported()) {
			LocationHelper.loadFolderList(this)?.let {
				if (it.size > 1) {
					// Init list view
					val listView = findViewById(android.R.id.list) as ListView
					listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
						location = LocationHelper.addRoot((view as TextView).text.toString())
						useIntent()
					}
					listView.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, it)
					return // await selection
				} else if (it.size == 1) {
					// Only one location, just use it
					location = LocationHelper.addRoot(it[0])
					useIntent()
					return // activity dead
				} else {
					Toast.makeText(this, R.string.toast_save_init_no_locations, Toast.LENGTH_LONG).show()
					exitApplication()
					return // activity dead
				}
			}

			Toast.makeText(this, R.string.toast_save_init_error, Toast.LENGTH_LONG).show()
		} else {
			showNotSupported()
		}
	}

	fun testSupported(): Boolean {
		// Get intent, action and MIME type
		val action: String? = intent.action
		val type: String? = intent.type

		type?.let {
			if (Intent.ACTION_SEND == action) {
				if (type.startsWith("image/")) {
					return true
				}
			} else if (Intent.ACTION_SEND_MULTIPLE == action) {
				if (type.startsWith("image/")) {
					return true
				}
			}
		}
		return false
	}

	fun useIntent() {
		// Get intent, action and MIME type
		val action: String? = intent.action
		val type: String? = intent.type

		var done = false

		type?.let {
			if (Intent.ACTION_SEND == action) {
				if (type.startsWith("image/")) {
					// Handle single image being sent
					done = handleImage()
				}
			} else if (Intent.ACTION_SEND_MULTIPLE == action) {
				if (type.startsWith("image/")) {
					// Handle multiple images being sent
					done = handleMultipleImages()
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

		exitApplication()
	}

	/**
	 * Show the not supported information.
	 */
	fun showNotSupported() {
		val type: String? = intent.type
		// Hide list
		findViewById(android.R.id.list).visibility = View.GONE
		// Build and show unsupported message
		val supportView = findViewById(R.id.not_supported2) as TextView
		// Generate issue text here as should always be English and does not need to be in strings.xml
		val issueLink = String.format(
				"https://github.com/ScreamingHawk/phone-saver/issues/new?title=%s%s&body=%s%s",
				"Unsupported Content Type - ",
				type,
				type,
				" is not supported. Please add support for this type.%0D%0A%0D%0AIssue via Phone Saver")
				.replace(" ", "%20")
		supportView.text = Html.fromHtml(resources.getString(R.string.not_supported2, issueLink))
		supportView.movementMethod = LinkMovementMethod.getInstance()
		findViewById(R.id.not_supported).visibility = View.VISIBLE
	}

	/**
	 * Exists the application is the best way available for the Android version
	 */
	fun exitApplication() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			finishAndRemoveTask()
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			finishAffinity()
		} else {
			finish()
		}
	}

	fun handleImage(): Boolean {
		intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
			return saveUri(it, getFilename(it))
		}
		return false
	}

	fun handleMultipleImages(): Boolean {
		val imageUris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
		imageUris?.let {
			var success = true
			imageUris.forEach {
				success = success && saveUri(it, getFilename(it))
			}
			return success
		}
		return false
	}

	/**
	 * Save the given uri to file
	 */
	fun saveUri(uri: Uri, filename: String): Boolean {
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
		// Default to last path if null
		var result: String = uri.lastPathSegment

		// Find the actual filename
		if (uri.scheme == "content") {
			contentResolver.query(uri, null, null, null, null)?.use {
				if (it.moveToFirst()) {
					result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
				}
			}
		}

		return result
	}
}
