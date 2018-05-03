package link.standen.michael.phonesaver.receiver

import android.content.BroadcastReceiver
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import link.standen.michael.phonesaver.R

/**
 * A class for monitoring downloads and toasting the user on completion.
 */
class DownloadBroadcastReceiver: BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action) {
			Toast.makeText(context, R.string.toast_save_successful, Toast.LENGTH_SHORT).show()
		}
	}
}
