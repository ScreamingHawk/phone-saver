package link.standen.michael.phonesaver.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import link.standen.michael.phonesaver.R
import link.standen.michael.phonesaver.util.LocationHelper
import android.provider.OpenableColumns
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import link.standen.michael.phonesaver.util.DebugLogger
import java.io.*
import android.widget.TextView
import link.standen.michael.phonesaver.saver.LocationSelectTask
import link.standen.michael.phonesaver.saver.HandleSingleTextTask
import link.standen.michael.phonesaver.util.data.Pair

/**
 * An activity to handle saving files.
 * https://developer.android.com/training/sharing/receive.html
 */
class SaverActivity : ListActivity() {

	companion object {
		const val TAG = "SaverActivity"

		const val FILENAME_REGEX = "[^-_.A-Za-z0-9]"
		const val FILENAME_LENIENT_REGEX = "[\\p{Cntrl}]"
		const val FILENAME_LENGTH_LIMIT = 100

		const val FILENAME_EXT_MATCH_LIMIT = 1000
	}

	val requestCodeLocationSelect = 1

	var forceSaving = false
	var registerMediaServer = false
	private var useLenientRegex = false
	private var locationSelectEnabled = false
	private var saveStrategy = 0

	private lateinit var log: DebugLogger

	var location: String? = null
	var convertedMime: String? = null

	var debugInfo: MutableList<Pair> = mutableListOf()

	lateinit var returnFromActivityResult: (fd: FileDescriptor?) -> Unit

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.saver_activity)

		log = DebugLogger(TAG, this)

		val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
		forceSaving = sharedPrefs.getBoolean("force_saving", false)
		registerMediaServer = sharedPrefs.getBoolean("register_file", false)
		useLenientRegex = sharedPrefs.getBoolean("lenient_regex", false)
		locationSelectEnabled = sharedPrefs.getBoolean("location_select", false)
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
		saveStrategy = resources.getStringArray(R.array.pref_list_values_file_exists).indexOf(
				PreferenceManager.getDefaultSharedPreferences(this).getString(
						"file_exists", resources.getString(R.string.pref_default_value_file_exists)))

		when {
			forceSaving -> loadList()
			else -> {
				useIntent({ success ->
					log.i("Supported: $success")
					// Success should never be null on a dryRun
					if (success!!){
						loadList()
					} else {
						showNotSupported()
					}
				}, dryRun=true)
			}
		}
	}

	/**
	 * Load the list of locations
	 */
	private fun loadList() {
		LocationHelper.loadFolderList(this)?.let {
			val locations = if (locationSelectEnabled){
				val locationSelectLabel = resources.getString(R.string.location_select_label)
				listOf(locationSelectLabel, *it.toTypedArray())
			} else it

			log.d("Locations:")
			for (loc in locations){
				log.d("\t$loc")
			}

			when {
				locations.size > 1 -> {
					runOnUiThread {
						findViewById<View>(R.id.loading).visibility = View.GONE
						// Init list view
						val listView = findViewById<ListView>(android.R.id.list)
						listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
							if (!locationSelectEnabled || position != 0){
								// The first item is the location select. Set location otherwise
								location = LocationHelper.addRoot(locations[position])
							}
							useIntent({ finishIntent(it) })
						}
						listView.adapter = ArrayAdapter<String>(this, R.layout.saver_list_item, locations.map {
							if (it.isBlank()) File.separator else it
						})
					}
					return // await selection
				}
				locations.size == 1 -> {
					// Only one location, just use it
					if (!locationSelectEnabled) {
						// Only set location when not using location select
						location = LocationHelper.addRoot(locations[0])
					}
					useIntent({ finishIntent(it) })
					return // activity dead
				}
				else -> {
					runOnUiThread {
						Toast.makeText(this, R.string.toast_save_init_no_locations, Toast.LENGTH_LONG).show()
						exitApplication()
					}
					return // activity dead
				}
			}
		}

		runOnUiThread {
			Toast.makeText(this, R.string.toast_save_init_error, Toast.LENGTH_LONG).show()
			exitApplication()
		}
		return // activity dead
	}

	private fun useIntent(callback: (success: Boolean?) -> Unit, dryRun: Boolean = false) {
		// Get intent action and MIME type
		val action: String? = intent.action
		val type: String? = intent.type

		log.i("Action: $action")
		log.i("Type: $type")

		type?.toLowerCase()?.let {
			if (Intent.ACTION_SEND == action) {
				return handleSingle(callback, dryRun)
			} else if (Intent.ACTION_SEND_MULTIPLE == action) {
				return handleMultiple(callback, dryRun)
			}

			if (forceSaving) {
				// Save the file the best way we can
				return handleSingle(callback, dryRun)
			}
		}

		log.i("No supporting method")

		// Failed to reach callback
		finishIntent(false)
	}

	/**
	 * Show the not supported information.
	 */
	private fun showNotSupported() {
		// Hide list
		runOnUiThread {
			findViewById<View>(R.id.loading).visibility = View.GONE
			findViewById<View>(android.R.id.list).visibility = View.GONE
			// Generate issue text here as should always be English and does not need to be in strings.xml
			val bobTitle = StringBuilder()
			bobTitle.append("Support Request - ")
			bobTitle.append(intent.type)
			val bobBody = StringBuilder()
			bobBody.append("Support request. Generated by Phone Saver.%0D%0A")
			bobBody.append("%0D%0AIntent type: ")
			bobBody.append(intent.type)
			bobBody.append("%0D%0AIntent action: ")
			bobBody.append(intent.action)
			intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
				bobBody.append("%0D%0AText: ")
				bobBody.append(it)
			}
			intent.getStringExtra(Intent.EXTRA_SUBJECT)?.let {
				bobBody.append("%0D%0ASubject: ")
				bobBody.append(it)
			}
			debugInfo.forEach {
				bobBody.append("%0D%0A${it.key}: ")
				bobBody.append(it.value)
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				intent.getStringExtra(Intent.EXTRA_HTML_TEXT)?.let {
					bobBody.append("%0D%0AHTML Text: ")
					bobBody.append(it)
				}
			}
			// Version
			try {
				val versionName = packageManager.getPackageInfo(packageName, 0).versionName
				bobBody.append("%0D%0AApplication Version: ")
				bobBody.append(versionName)
			} catch (e: PackageManager.NameNotFoundException) {
				log.e("Unable to get package version", e)
			}
			bobBody.append("%0D%0A%0D%0AMore information: TYPE_ADDITIONAL_INFORMATION_HERE")
			bobBody.append("%0D%0A%0D%0AThank you")
			val issueLink = "https://github.com/ScreamingHawk/phone-saver/issues/new?title=" +
					bobTitle.toString().replace(" ", "%20") +
					"&body=" +
					bobBody.toString().replace(" ", "%20").replace("=", "%3D")
			log.i(issueLink)

			// Build and show unsupported message
			val supportView = findViewById<TextView>(R.id.not_supported)
			@Suppress("DEPRECATION")
			supportView.text = Html.fromHtml(resources.getString(R.string.not_supported, issueLink))
			supportView.movementMethod = LinkMovementMethod.getInstance()
			findViewById<View>(R.id.not_supported_wrapper).visibility = View.VISIBLE
		}
	}

	/**
	 * Call when the intent is finished
	 */
	private fun finishIntent(success: Boolean?, messageId: Int? = null) {
		// Notify user
		runOnUiThread {
			when {
				messageId != null -> Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show()
				success == null -> Toast.makeText(this, R.string.toast_save_in_progress, Toast.LENGTH_SHORT).show()
				success -> Toast.makeText(this, R.string.toast_save_successful, Toast.LENGTH_SHORT).show()
				else -> Toast.makeText(this, R.string.toast_save_failed, Toast.LENGTH_SHORT).show()
			}
		}

		exitApplication()
	}

	/**
	 * Exists the application is the best way available for the Android version
	 */
	@SuppressLint("NewApi")
	private fun exitApplication() {
		when {
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> finishAndRemoveTask()
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN -> finishAffinity()
			else -> finish()
		}
	}

	/**
	 * Handle the saving of single items.
	 */
	private fun handleSingle(callback: (success: Boolean?) -> Unit, dryRun: Boolean) {
		// Try save stream first
		intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
			log.d("Text has stream")
			getFilename(it, intent.type, dryRun, {filename ->
				saveUri(it, filename, callback, dryRun)
			})
			return
		}

		// Save the text
		intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
			log.d("Text Extra: $it")
			HandleSingleTextTask(this, it, intent, dryRun, callback).execute()
		} ?: callback(false)
	}

	/**
	 * Handle the saving of multiple streams.
	 */
	private fun handleMultiple(callback: (success: Boolean?) -> Unit, dryRun: Boolean) {
		val imageUris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
		imageUris?.let {
			var counter = 0
			var completeSuccess = true
			imageUris.forEach {
				getFilename(it, intent.type, dryRun, { filename->
					saveUri(it, filename, { success ->
						counter++
						success?.let {
							completeSuccess = completeSuccess && it
						}
						if (counter == imageUris.size){
							callback(completeSuccess)
						}
					}, dryRun)
				})
			}
		} ?: callback(false)
	}

	/**
	 * Save the given uri to filesystem.
	 */
	private fun saveUri(uri: Uri, filename: String, callback: (success: Boolean?) -> Unit, dryRun: Boolean) {
		val destinationFilename = LocationHelper.safeAddPath(location, filename)

		if (!dryRun) {
			val sourceFilename = uri.path
			log.d("Saving $sourceFilename to $destinationFilename")
		} else {
			// This method can be skipped when doing a dry run
			return callback(true)
		}

		if (location == null){
			// No location, use location select
			returnFromActivityResult = {
				if (it == null){
					callback(false)
				} else {
					val bos = BufferedOutputStream(FileOutputStream(it))
					contentResolver.openInputStream(uri)?.use { bis ->
						saveStream(bis, bos, null, callback, dryRun)
					} ?: callback(false)
				}
			}
			LocationSelectTask(this).save(filename, convertedMime!!)
		} else {
			try {
				contentResolver.openInputStream(uri)?.use { bis ->
					val fout = File(destinationFilename)
					if (!fout.exists()) {
						fout.createNewFile()
					}
					val bos = BufferedOutputStream(FileOutputStream(fout, false))
					saveStream(bis, bos, destinationFilename, callback, dryRun)
				} ?: callback(false)
			} catch (e: FileNotFoundException) {
				log.e("File not found. Perhaps you are overriding the same file and just deleted it?", e)
				callback(false)
			}
		}
	}

	/**
	 * Save a stream to the filesystem.
	 */
	private fun saveStream(bis: InputStream, bos: OutputStream, destinationFilename: String?,
						   callback: (success: Boolean?) -> Unit, dryRun: Boolean) {
		if (dryRun){
			// This entire method can be skipped when doing a dry run
			return callback(true)
		}

		var success = false

		try {
			val buf = ByteArray(1024)
			var bytesRead = bis.read(buf)
			while (bytesRead != -1) {
				bos.write(buf, 0, bytesRead)
				bytesRead = bis.read(buf)
			}

			// Done
			success = true

			if (registerMediaServer && destinationFilename != null){
				MediaScannerConnection.scanFile(this, arrayOf(destinationFilename), null, null)
			}
		} catch (e: IOException) {
			log.e("Unable to save file", e)
		} finally {
			try {
				bos.close()
			} catch (e: IOException) {
				log.e("Unable to close stream", e)
			}
		}
		callback(success)
	}

	/**
	 * Get the filename from a Uri.
	 */
	private fun getFilename(uri: Uri, mime: String, dryRun: Boolean, callback: (filename: String) -> Unit) {
		// Find the actual filename
		if (uri.scheme == "content") {
			contentResolver.query(uri, null, null, null, null)?.use {
				if (it.moveToFirst()) {
					return getFilename(it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME)), mime, dryRun, callback, uri)
				}
			}
		}
		getFilename(uri.lastPathSegment, mime, dryRun, callback, uri)
	}

	/**
	 * Get the filename from a string.
	 */
	fun getFilename(s: String, mime: String, dryRun: Boolean, callback: (filename: String) -> Unit, uri: Uri? = null) {
		// Validate the mime type
		log.d("Converting mime: $mime")
		convertedMime = mime.replaceAfter(";", "").replace(";", "")
		log.d("Converted mime: $convertedMime")

		log.d("Converting filename: $s")

		var result = s
				// Take last section after a slash (excluding the slash)
				.replaceBeforeLast("/", "").replace("/", "")
				// Take first section before a space (excluding the space)
				.replaceAfter(" ", "").replace(" ", "")
				// Remove non-filename characters
				.replace(Regex(if (useLenientRegex) FILENAME_LENIENT_REGEX else FILENAME_REGEX), "")

		if (result.length > FILENAME_LENGTH_LIMIT) {
			// Do not go over the filename length limit
			result = result.substring(0, FILENAME_LENGTH_LIMIT)
		}

		var ext = result.substringAfterLast('.', "")
		if (!MimeTypeMap.getSingleton().hasExtension(ext)){
			// Add file extension
			MimeTypeMap.getSingleton().getExtensionFromMimeType(convertedMime)?.let {
				ext = it
				log.d("Adding extension $it to $result")
				result += ".$it"
			}
		}

		log.d("Converted filename: $result")

		if (!dryRun) {
			location?.let {
				val destinationFilename = LocationHelper.safeAddPath(location, result)
				val f = File(destinationFilename)
				if (f.exists()) {
					when (saveStrategy) {
						0 -> {
							// Overwrite. Delete the file, so that it will be overridden
							uri?.let { u ->
								val sourceFilename = u.path
								if (sourceFilename.contains(destinationFilename)) {
									if (saveStrategy == 0) {
										log.w("Aborting! It appears you are saving the file over itself")
										finishIntent(false, R.string.toast_save_file_exists_self_abort)
										return
									} else {
										log.i("Continuing! It appears you are saving the file over itself")
										Toast.makeText(this, R.string.toast_save_file_exists_self_continue, Toast.LENGTH_SHORT).show()
									}
								}
							}
							Toast.makeText(this, R.string.toast_save_file_exists_overwrite, Toast.LENGTH_SHORT).show()
							log.w("Overwriting $result")
							f.delete()
						}
						1 -> {
							// Nothing. Quit
							log.d("Quitting due to duplicate $result")
							finishIntent(false, R.string.toast_save_file_exists_fail)
							return
						}
						2 -> {
							// Postfix. Add counter before extension
							log.d("Adding postfix to $result")
							var i = 1
							val before = LocationHelper.safeAddPath(location, result.substringBeforeLast('.', "")) + "."
							if (ext.isNotBlank()) {
								ext = ".$ext"
							}
							while (File(before + i + ext).exists()) {
								i++
								if (i > FILENAME_EXT_MATCH_LIMIT) {
									// We have a lot of matches. This is too hard
									log.w("There are over $FILENAME_EXT_MATCH_LIMIT matches for $before$ext. Aborting.")
									finishIntent(false, R.string.toast_save_file_exists_fail)
									return
								}
							}
							result = before + i + ext
						}
						3 -> {
							// Request
							log.e("Not implemented!")
							throw NotImplementedError("Requesting filename not yet implemented.")
						}
					}
				}
			}
		}

		callback(result)
	}

	fun failOnActivityResult(requestCode: Int) {
		onActivityResult(requestCode, Activity.RESULT_CANCELED, null)
	}

	/**
	 * Return from location select
	 */
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		when (requestCode){
			requestCodeLocationSelect -> {
				if (resultCode == RESULT_OK){
					data?.data.let {uri ->
						val pfd = contentResolver.openFileDescriptor(uri, "w")
						return returnFromActivityResult(pfd.fileDescriptor)
					}
				} else {
					// Selection cancelled, fail save
					returnFromActivityResult(null)
				}
			}
		}
	}

}
