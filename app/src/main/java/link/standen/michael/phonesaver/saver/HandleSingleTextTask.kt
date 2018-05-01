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
import link.standen.michael.phonesaver.util.data.Pair
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
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

	override fun doInBackground(vararg params: Unit?) {
		val saverActivity = saverActivityRef.get()

		if (saverActivity == null || saverActivity.isFinishing){
			callback(false)
			return
		}
		log = DebugLogger(SaverActivity.TAG, saverActivity)

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
					} else if (saverActivity.forceSaving && !dryRun){
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
	 * Save the given url to the filesystem.
	 */
	private fun saveUrl(saverActivity: SaverActivity, uri: Uri, filename: String, callback: (success: Boolean?) -> Unit, dryRun: Boolean) {
		if (dryRun){
			// This entire method can be skipped when doing a dry run
			return callback(true)
		}

		var success: Boolean? = false

		saverActivity.location?.let {
			val sourceFilename = uri.toString()
			val destinationFilename = LocationHelper.safeAddPath(saverActivity.location, filename)

			log.d("Saving $sourceFilename to $destinationFilename")

			val downloader = DownloadManager.Request(uri)
			downloader.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
					.setAllowedOverRoaming(true)
					.setTitle(filename)
					.setDescription(saverActivity.resources.getString(R.string.downloader_description, sourceFilename))
					.setDestinationInExternalPublicDir(LocationHelper.removeRoot(it), filename)

			if (saverActivity.registerMediaServer){
				downloader.allowScanningByMediaScanner()
			}

			(saverActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(downloader)

			success = null
		}

		callback(success)
	}

	/**
	 * Save a string to the filesystem.
	 */
	private fun saveString(saverActivity: SaverActivity, s: String, filename: String, callback: (success: Boolean?) -> Unit, dryRun: Boolean) {
		if (dryRun){
			// This entire method can be skipped when doing a dry run
			return callback(true)
		}

		val destinationFilename = LocationHelper.safeAddPath(saverActivity.location, filename)
		var success = false
		var bw: BufferedWriter? = null

		try {
			val fout = File(destinationFilename)
			if (!fout.exists()){
				fout.createNewFile()
			}
			bw = BufferedWriter(FileWriter(destinationFilename))
			bw.write(s)

			// Done
			success = true

			if (saverActivity.registerMediaServer){
				MediaScannerConnection.scanFile(saverActivity, arrayOf(destinationFilename), null, null)
			}
		} catch (e: IOException) {
			log.e("Unable to save file", e)
		} finally {
			try {
				bw?.close()
			} catch (e: IOException) {
				log.e("Unable to close stream", e)
			}
		}
		callback(success)
	}
}
