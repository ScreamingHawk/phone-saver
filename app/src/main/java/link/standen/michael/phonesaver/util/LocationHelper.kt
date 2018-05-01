package link.standen.michael.phonesaver.util

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * A helper class for managing the stored locations
 */
object LocationHelper {

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
	fun removeRoot(location: String): String = location.replace(rootLocation, "")

	/**
	 * Add the root location to the given path.
	 */
	fun addRoot(location: String): String = rootLocation + location



	/**
	 * Add the location path if not null and not already added.
	 */
	fun safeAddPath(location: String?, filename: String): String {
		location?.let {
			if (!filename.startsWith(it)){
				return it + File.separatorChar + filename
			}
		}
		return filename
	}

}
