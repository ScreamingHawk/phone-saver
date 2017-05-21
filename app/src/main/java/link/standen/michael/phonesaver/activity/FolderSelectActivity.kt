package link.standen.michael.phonesaver.activity

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import link.standen.michael.phonesaver.R
import android.content.pm.PackageManager
import android.os.Build
import android.widget.AdapterView
import android.util.Log
import java.io.File
import android.os.Parcelable
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.TextView

class FolderSelectActivity : AppCompatActivity() {

	private val TAG = "FolderSelectActivity"

	private var rootLocation = Environment.getExternalStorageDirectory().absolutePath
	private var currentPath = Environment.getExternalStorageDirectory().absolutePath

	private lateinit var listView: ListView

	private var folderList: List<String>? = ArrayList()

	private val LIST_STATE = "listState"
	private var listState: Parcelable? = null

	private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
		when (item.itemId) {
			R.id.navigation_here -> {
				//FIXME Call back to previous activity with the location somehow
				return@OnNavigationItemSelectedListener true
			}
		}
		false
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.folder_select_activity)

		// Init list view
		listView = findViewById(android.R.id.list) as ListView
		listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
			currentPath += (view as TextView).text.toString()
			updateListView()
		}

		// Init bottom buttons
		val navigation = findViewById(R.id.navigation) as BottomNavigationView
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

	override fun onResume() {
		super.onResume()

		// Restore the list view scroll location
		listState?.let { (findViewById(android.R.id.list) as ListView).onRestoreInstanceState(listState) }
		listState = null
	}

	/**
	 * Load list view scroll position
	 */
	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		listState = savedInstanceState.getParcelable(LIST_STATE)
	}

	/**
	 * Save list view scroll position
	 */
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		listState = (findViewById(android.R.id.list) as ListView).onSaveInstanceState()
		outState.putParcelable(LIST_STATE, listState)
	}

	/**
	 * Update the folder list view
	 */
	private fun updateListView() {
		Log.v(TAG, "Path is: "+currentPath)

		folderList = File(currentPath).listFiles()
				// Directories only
				?.filter { it.isDirectory }
				// Get the file path
				?.map { removeCurrent(it.absolutePath) }
				// Sort it
				?.sorted()
				// Default to empty
				?: ArrayList<String>()

		Log.d(TAG, "Length: "+folderList?.size)

		// Set title
		this.title = removeRoot(currentPath) + File.separatorChar

		listView.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, folderList)
	}

	/**
	 * Remove the root location from the given path.
	 */
	private fun removeRoot(location: String): String {
		return location.replace(rootLocation, "")
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
				return true;
			}

		}
		return super.onOptionsItemSelected(item)
	}

	/**
	 * Goes up a directory, unless at the top, then exits
	 */
	override fun onBackPressed() {
		if (currentPath == rootLocation) {
			super.onBackPressed()
		} else {
			currentPath = currentPath.substring(0, currentPath.lastIndexOf(File.separatorChar))
			updateListView()
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
