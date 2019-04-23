package link.standen.michael.phonesaver.util

import android.content.Context
import link.standen.michael.phonesaver.data.ConnectionPair

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * A helper class for managing links from Imgur
 */
object ImgurHelper {

	private fun getLogger(context: Context) = DebugLogger(context, ImgurHelper::class.java.simpleName)

	/**
	 * Returns the first image from an Imgur link
	 * @return The URL of the first Imgur image, or the input url parameter
	 */
	fun getImageUrl(context: Context, pair: ConnectionPair): ConnectionPair {

		var p = pair
		val u = pair.url

		val log = getLogger(context)
		if (isImgurPage(u)){
			log.d("Getting imgur image string from $u")

			if (p.conn == null){
				// Try to redirect and connect here
				p = LinkHelper.resolveRedirects(context, u)
				if (p.conn == null) {
					// Fail to make connection / follow redirects
					return pair
				}
			}
			val inputStream = p.conn?.inputStream
			if (inputStream == null) {
				// Nothing to do.
				log.d("No input stream")
				return p
			}
			val buffer = StringBuffer()
			val reader = BufferedReader(InputStreamReader(inputStream))
			var line: String?
			do  {
				line = reader.readLine()
				buffer.append("$line\n")
			} while (line != null)
			if (buffer.isEmpty()) {
				// No content
				return p
			}
			val text = buffer.toString()

			"""https?://i.imgur.com/[0-z]+\.[a-z]{3,4}""".toRegex().find(text)?.value?.let {
				log.d("Imgur image url is $it")
				return ConnectionPair(it, null)
			}
		} else {
			log.d("Not an imgur url $u")
		}

		return p
	}

	/**
	 * Checks a URL is for an Imgur page (not image)
	 */
	private fun isImgurPage(url: String): Boolean {
		return !("""https?://[m.]?imgur.com/[0-z/]+""".toRegex().matchEntire(url)?.groups?.isEmpty() ?: true)
	}
}
