package link.standen.michael.phonesaver.util

import android.content.Context
import link.standen.michael.phonesaver.data.ConnectionPair

import java.net.URL
import java.net.HttpURLConnection
import java.net.MalformedURLException

/**
 * A helper class for managing JSON files
 */
object LinkHelper {

	private fun getLogger(context: Context) = DebugLogger(context, LinkHelper::class.java.simpleName)

	/**
	 * Resolves redirects
	 * @return The URL after resolving redirects, or the input parameter
	 */
	fun resolveRedirects(context: Context, url: String): ConnectionPair {
		val log = getLogger(context)
		var u = url
		var pair = ConnectionPair(u)

		try {
			log.d("Checking for redirects for $u")

			var conn = URL(u).openConnection() as HttpURLConnection
			conn.connect()
			var code = conn.responseCode
			while (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP){
				// Follow redirects
				conn.disconnect()
				u = conn.getHeaderField("Location")
				log.d("Following redirect to $u")
				conn = URL(u).openConnection() as HttpURLConnection
				conn.connect()
				code = conn.responseCode
			}
			log.d("Final status code: $code")
			pair = ConnectionPair(u, conn)
		} catch (e: MalformedURLException){
			// Something went wrong, fall through will return the original URL
			log.e("Error resolving redirects", e)
		}

		return pair
	}
}
