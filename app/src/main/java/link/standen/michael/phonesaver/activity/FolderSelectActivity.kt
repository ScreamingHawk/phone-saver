package link.standen.michael.phonesaver.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.BottomNavigationView
import android.widget.ListView
import android.content.pm.PackageManager
import android.os.Build
import android.widget.AdapterView
import android.util.Log
import java.io.File
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.TextView
import link.standen.michael.phonesaver.R.*
import link.standen.michael.phonesaver.util.LocationHelper

/**
 * An activity for selecting a folder in the file system.
 */
class FolderSelectActivity : ListActivity() {

	companion object {
		const val TAG = "FolderSelectActivity"
		const val FOLDER_SELECTED = "FolderSelected"
	}

	private var currentPath: String = Environment.getExternalStorageDirectory().absolutePath

	private lateinit var listView: ListView

	private var folderList: List<String>? = ArrayList()

	private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
		when (item.itemId) {
			id.navigation_here -> {
				// Call back to previous activity with the location
				val intent = Intent()
				intent.putExtra(FOLDER_SELECTED, currentPath)
				setResult(RESULT_OK, intent)
				finish()
				return@OnNavigationItemSelectedListener true
			}
		}
		false
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(layout.folder_select_activity)

		// Init list view
		listView = findViewById(android.R.id.list) as ListView
		listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
			with(view as TextView){
				if (view.text.toString() == resources.getString(string.back_folder)) {
					val back = getBackLocation()
					if (back == null) {
						cancelIntent()
					} else {
						currentPath = back
					}
				} else {
					currentPath += view.text.toString()
				}
				updateListView()
			}
		}

		// Init bottom buttons
		val navigation = findViewById(id.navigation) as BottomNavigationView
		navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
	}

	override fun onStart() {
		super.onStart()

		// Permission check
		if (isStoragePermissionGranted()) {
			updateListView()
		}
		// else wait for permission handler to continue
	}

	/**
	 * Update the folder list view
	 */
	private fun updateListView() {
		Log.v(TAG, "Path is: "+currentPath)

		val fList = File(currentPath).listFiles()
				// Directories only
				?.filter { it.isDirectory }
				// Get the file path
				?.map { removeCurrent(it.absolutePath) }
				// Sort it
				?.sortedBy { it.toLowerCase() }
				// Convert to mutable
				?.toMutableList()
				// Default to empty
				?: ArrayList<String>()

		// Add back button as first item
		fList.add(0, resources.getString(string.back_folder))
		
		folderList = fList.toList()
		Log.d(TAG, "Length: "+folderList?.size)

		// Set title
		this.title = LocationHelper.removeRoot(currentPath) + File.separatorChar

		listView.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, folderList)
	}

	/**
	 * Remove the current location from the given path.
	 */
	private fun removeCurrent(location: String): String {
		return location.replace(currentPath, "")
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		when (item?.itemId){
			android.R.id.home -> {
				// Back button
				onBackPressed()
				return true
			}

		}
		return super.onOptionsItemSelected(item)
	}

	/**
	 * Goes up a directory, unless at the top, then exits
	 */
	override fun onBackPressed() {
		val back = getBackLocation()
		if (back == null || currentPath == LocationHelper.rootLocation) {
			cancelIntent()
		} else {
			currentPath = back
			updateListView()
		}
	}

	/**
	 * Cancels the current activity
	 */
	private fun cancelIntent() {
		val intent = Intent()
		intent.putExtra(FOLDER_SELECTED, currentPath)
		setResult(RESULT_CANCELED, intent)
		finish()
	}

	/**
	 * Returns the back location for the current path
	 */
	private fun getBackLocation(): String? {
		if (currentPath.contains(File.separatorChar)) {
			return currentPath.substring(0, currentPath.lastIndexOf(File.separatorChar))
		} else {
			return null
		}
	}

	/**
	 * Permissions checker
	 */
	private fun isStoragePermissionGranted(): Boolean {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				Log.v(TAG, "Permission is granted")
				return true
			} else {
				Log.v(TAG, "Permission is revoked")
				requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
				return false
			}
		} else {
			//permission is automatically granted on sdk<23 upon installation
			Log.v(TAG, "Permission is granted")
			return true
		}
	}

	/**
	 * Permissions handler
	 */
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		Log.d(TAG, "Permission: " + permissions[0] + " was " + grantResults[0])
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			updateListView()
		}
	}

}
