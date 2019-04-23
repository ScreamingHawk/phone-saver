package link.standen.michael.phonesaver.service

import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import android.support.annotation.RequiresApi
import com.amulyakhare.textdrawable.TextDrawable
import link.standen.michael.phonesaver.activity.SaverActivity
import link.standen.michael.phonesaver.util.DebugLogger
import link.standen.michael.phonesaver.util.LocationHelper
import java.io.File

/**
 * Service for creating direct share options
 */
@RequiresApi(Build.VERSION_CODES.M)
class LocationChooserTargetService : ChooserTargetService() {

	companion object {
		private const val targetRanking = 1.0f
		private const val targetNameMaxLength = 15
	}

	private val componentName = ComponentName(SaverActivity::class.java.`package`?.name ?: "LocationChooserTargetService", SaverActivity::class.java.name)
	private lateinit var log: DebugLogger

	override fun onGetChooserTargets(targetActivityName: ComponentName?, matchedFilter: IntentFilter?): MutableList<ChooserTarget> {
		log = DebugLogger(this)
		log.d("Creating direct share targets")

		return LocationHelper.loadFolderList(this)?.map {
				val name = if (it.length > targetNameMaxLength) {
					it.substring(it.length - targetNameMaxLength)
				} else it
				log.d("Adding $it to targets")
				val extras = Bundle()
				extras.putString(SaverActivity.DIRECT_SHARE_FOLDER, it)
				ChooserTarget(name, Icon.createWithBitmap(generateIcon(it)), targetRanking, componentName, extras)
			}?.toMutableList()
		 ?: mutableListOf()
	}

	/**
	 * Generate an icon using the first character after the last slash.
	 */
	private fun generateIcon(text: String): Bitmap{
		val width = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
		val height = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
		val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)

		val index = text.indexOfLast {
			it == File.separatorChar
		}
		val char = if (index >= 0 && index < text.length){
			text[index + 1]
		} else File.separatorChar
		val drawable = TextDrawable.builder().buildRound(char.toString(), Color.LTGRAY)
		drawable.setBounds(0, 0, canvas.width, canvas.height)
		drawable.draw(canvas)

		return bitmap
	}

}
