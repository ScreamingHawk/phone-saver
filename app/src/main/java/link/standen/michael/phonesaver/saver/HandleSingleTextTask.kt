package link.standen.michael.phonesaver.saver

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import android.webkit.MimeTypeMap
import link.standen.michael.phonesaver.R
import link.standen.michael.phonesaver.activity.SaverActivity
import link.standen.michael.phonesaver.util.DebugLogger
import link.standen.michael.phonesaver.util.LocationHelper
import link.standen.michael.phonesaver.data.Pair
import link.standen.michael.phonesaver.util.PreferenceHelper
import java.io.*
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.net.URL

/**
 * An async task to handle saving a single text entry
 */
class HandleSingleTextTask
internal constructor(
		context: SaverActivity,
		private val text: String,
		private val intent: Intent,
		private val dryRun: Boolean,
		private val callback: (success: Boolean?) -> Unit)
	: AsyncTask<Unit, Unit, Unit>() {

	private val saverActivityRef: WeakReference<SaverActivity> = WeakReference(context)

	private lateinit var log: DebugLogger
	companion object {
		const val TAG = "HandleSingleTextTask"
	}

	override fun doInBackground(vararg params: Unit?) {
		val saverActivity = saverActivityRef.get()

		if (saverActivity == null || saverActivity.isFinishing){
			callback(false)
			return
		}
		log = DebugLogger(TAG, saverActivity)

		try {
			val url = URL(text)
			// It's a URL
			log.d("Text with URL")
			val mime = MimeTypeMap.getSingleton()
			val urlContentType = mime.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(text))
			// Fall back to checking URL content type
					?: url.openConnection().getHeaderField("Content-Type")
			urlContentType?.toLowerCase()?.let { contentType ->
				log.d("URL Content-Type: $contentType")
				saverActivity.debugInfo.add(Pair("URL Content-Type", contentType))
				saverActivity.getFilename(intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: Uri.parse(text).lastPathSegment,
						contentType, dryRun, { filename ->
					if (contentType.startsWith("image/") ||
							contentType.startsWith("video/") ||
							contentType.startsWith("audio/")) {
						saveUrl(saverActivity, Uri.parse(text), filename, callback, dryRun)
					} else if (contentType.startsWith("text/")){
						saveString(saverActivity, text, filename, callback, dryRun)
					} else if (PreferenceHelper.forceSaving && !dryRun){
						// Fallback to saving with saveUrl
						saveUrl(saverActivity, Uri.parse(text), filename, callback, dryRun)
					} else {
						callback(false)
					}
				})
			}?: callback(false)
		} catch (e: MalformedURLException){
			log.d("Text without URL")
			// It's just some text
			val mimeType: String = intent.type?.toLowerCase() ?: "text/plain"
			saverActivity.getFilename(intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: text,
					mimeType, dryRun, { filename ->
				saveString(saverActivity, text, filename, callback, dryRun)
			})
		}
	}

	/**
	 * Save the given url to the filesystem
	 */
	private fun saveUrl(saverActivity: SaverActivity, uri: Uri, filename: String, callback: (success: Boolean?) -> Unit, dryRun: Boolean) {
		if (dryRun){
			// This entire method can be skipped when doing a dry run
			return callback(true)
		}

		val sourceFilename = uri.toString()

		if (saverActivity.location == null) {
			// No location, use documentUI
			saverActivity.returnFromActivityResult = { destination ->
				if (destination == null){
					callback(false)
				} else {
					log.d("Calling to save URL $sourceFilename to $destination")
					SaveFromUrlTask(saverActivity, sourceFilename, destination, filename, dryRun, callback).execute()
				}
			}
			LocationSelectTask(saverActivity).save(filename, saverActivity.convertedMime!!)
		} else {
			// Locate passed
			val destinationFilename = LocationHelper.safeAddPath(saverActivity.location, filename)

			log.d("Saving $sourceFilename to $destinationFilename")

			val downloader = DownloadManager.Request(uri)
			downloader.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
					.setDestinationInExternalPublicDir(LocationHelper.removeRoot(saverActivity.location!!), filename)
					.setAllowedOverRoaming(true)
					.setTitle(filename)
					.setDescription(saverActivity.resources.getString(R.string.downloader_description, sourceFilename))

			if (PreferenceHelper.registerMediaServer){
				downloader.allowScanningByMediaScanner()
			}

			(saverActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(downloader)

			callback(null)
		}
	}

	/**
	 * Save a string to the filesystem
	 */
	private fun saveString(saverActivity: SaverActivity, s: String, filename: String, callback: (success: Boolean?) -> Unit, dryRun: Boolean) {
		if (dryRun){
			// This entire method can be skipped when doing a dry run
			return callback(true)
		}

		if (saverActivity.location == null) {
			// No location, use documentUI
			saverActivity.returnFromActivityResult = {
				if (it == null){
					callback(false)
				} else {
					val pfd = saverActivity.contentResolver.openFileDescriptor(it, "w")
					val bos = BufferedOutputStream(FileOutputStream(pfd.fileDescriptor))
					doSaveString(saverActivity, bos, s, null, callback)
				}
			}
			LocationSelectTask(saverActivity).save(filename, saverActivity.convertedMime!!)
		} else {
			// Manual
			val destinationFilename = LocationHelper.safeAddPath(saverActivity.location, filename)
			var bos: BufferedOutputStream? = null
			try {
				val fout = File(destinationFilename)
				if (!fout.exists()){
					fout.createNewFile()
				}
				bos = BufferedOutputStream(FileOutputStream(destinationFilename))
				doSaveString(saverActivity, bos, s, destinationFilename, callback)
			} catch (e: IOException) {
				log.e("Unable to save file", e)
			} finally {
				try {
					bos?.close()
				} catch (e: IOException) {
					log.e("Unable to close stream", e)
				}
			}
		}
	}

	/**
	 * Do the saving of the string
	 */
	private fun doSaveString(saverActivity: SaverActivity, bos: BufferedOutputStream, s: String,
							 destinationFilename: String?, callback: (success: Boolean?) -> Unit){
		var success = false

		try {
			bos.write(s.toByteArray())

			// Done
			success = true

			if (PreferenceHelper.registerMediaServer && destinationFilename != null){
				MediaScannerConnection.scanFile(saverActivity, arrayOf(destinationFilename), null, null)
			}
		} catch (e: IOException) {
			log.e("Unable to save file", e)
		}
		callback(success)
	}
}
