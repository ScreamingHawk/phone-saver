package link.standen.michael.phonesaver.util

import android.content.Context

import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection


/**
 * A helper class for managing JSON files
 */
object ImgurHelper {

	private fun getLogger(context: Context) = DebugLogger(context, ImgurHelper::class.java.simpleName)

	/**
	 * Returns the first image from an Imgur link
	 * @return The URL of the first Imgur image, or the input url parameter
	 */
	fun getImageUrl(context: Context, url: String): String {

		var u = url

		val log = getLogger(context)
		if (isImgurPage(url)){
			log.d("Getting imgur image string from $url")

			// Find image in page
			var conn = URL(url).openConnection() as HttpURLConnection
			conn.connect()
			var code = conn.responseCode
			while (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP){
				// Follow redirects
				conn.disconnect()
				val redirectTo = conn.getHeaderField("Location")
				log.d("Following redirect to $redirectTo")
				conn = URL(redirectTo).openConnection() as HttpURLConnection
				conn.connect()
				code = conn.responseCode
			}
			log.d("Status: $code")
			val inputStream = conn.inputStream
			val buffer = StringBuffer()
			if (inputStream == null) {
				// Nothing to do.
				log.d("No input stream")
				return u
			}
			val reader = BufferedReader(InputStreamReader(inputStream))
			var line: String?
			do  {
				line = reader.readLine()
				buffer.append("$line\n")
			} while (line != null)
			if (buffer.isEmpty()) {
				// No content
				return u
			}
			val text = buffer.toString()

			"""https?://i.imgur.com/[0-z]+\.[a-z]{3,4}""".toRegex().find(text)?.value?.let {
				log.d("Imgur image url is $it")
				u = it
			}
		} else {
			log.d("Not an imgur url $url")
		}

		return u
	}

	private fun isImgurPage(url: String): Boolean {
		return !("""https?://[m.]?imgur.com/[0-z/]+""".toRegex().matchEntire(url)?.groups?.isEmpty() ?: true)
	}
}
