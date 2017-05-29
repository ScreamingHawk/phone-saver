package link.standen.michael.phonesaver.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * A helper class for managing the stored locations
 */
object LocationHelper {

	private val TAG = "LocationHelper"

	private val FOLDER_LIST_STORE = "FOLDER_STORE"

	/**
	 * Loads the list of folder paths.
	 */
	fun loadFolderList(context: Context): MutableList<String>? {
		val type = object : TypeToken<MutableList<String>>() {}.type
		return Gson().fromJson<MutableList<String>>(JsonFileHelper.getJsonFromFile(context, FOLDER_LIST_STORE), type)
	}

	/**
	 * Save the list of folder paths.
	 */
	fun saveFolderList(context: Context, folderList: MutableList<String>) {
		if (JsonFileHelper.saveJsonToFile(context, Gson().toJson(folderList), FOLDER_LIST_STORE)){
			// Success
		} else {
			//TODO Toast an error? Or pass it up
		}
	}

}
