package link.standen.michael.phonesaver.saver

import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import link.standen.michael.phonesaver.activity.SaverActivity
import link.standen.michael.phonesaver.util.DebugLogger
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.net.URL

/**
 * An async task to handle saving a URL into a destination uri
 */
class SaveFromUrlTask
internal constructor(
		context: SaverActivity,
		private val sourceFilename: String,
		private val destination: Uri,
		private val filename: String,
		private val dryRun: Boolean,
		private val callback: (success: Boolean?) -> Unit)
	: AsyncTask<Unit, Unit, Unit>() {

	private val saverActivityRef: WeakReference<SaverActivity> = WeakReference(context)

	private lateinit var log: DebugLogger

	override fun doInBackground(vararg params: Unit?) {
		saverActivityRef.get()?.let {saverActivity ->
			log = DebugLogger(saverActivity, SaveFromUrlTask::class.java.simpleName)

			log.d("Saving URL $sourceFilename to $destination")

			// Open streams for saving
			URL(sourceFilename).openStream().use {
				val pfd = saverActivity.contentResolver.openFileDescriptor(destination, "w")
				val bos = BufferedOutputStream(FileOutputStream(pfd.fileDescriptor))
				saverActivity.saveStream(it, bos, filename, callback, dryRun)
			}
		}?: run {
			Log.e(SaveFromUrlTask::class.java.simpleName, "Saver Activity is gone")
			callback(false)
		}
	}
}
