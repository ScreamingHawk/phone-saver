package link.standen.michael.phonesaver.util

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * A helper class for managing the stored locations
 */
object LocationHelper {

	const private val TAG = "LocationHelper"
	const private val FOLDER_LIST_STORE = "FOLDER_STORE"

	val rootLocation = Environment.getExternalStorageDirectory().absolutePath!!

	/**
	 * Loads the list of folder paths.
	 */
	fun loadFolderList(context: Context): List<String>? {
		val type = object : TypeToken<List<String>>() {}.type
		return Gson().fromJson<List<String>>(JsonFileHelper.getJsonFromFile(context, FOLDER_LIST_STORE), type)
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

	/**
	 * Remove the root location from the given path.
	 */
	fun removeRoot(location: String): String {
		return location.replace(rootLocation, "")
	}

	/**
	 * Add the root location to the given path.
	 */
	fun addRoot(location: String): String {
		return rootLocation + location
	}

}
