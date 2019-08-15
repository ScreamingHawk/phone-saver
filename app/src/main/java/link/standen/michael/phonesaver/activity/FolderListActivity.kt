package link.standen.michael.phonesaver.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.database.DataSetObserver
import android.os.Build
import android.view.View
import android.widget.ListView
import de.cketti.library.changelog.ChangeLog
import link.standen.michael.phonesaver.R
import link.standen.michael.phonesaver.adapter.DeletableLocationArrayAdapter
import link.standen.michael.phonesaver.util.DebugLogger
import link.standen.michael.phonesaver.util.LocationHelper
import link.standen.michael.phonesaver.data.LocationWithData
import link.standen.michael.phonesaver.util.DialogHelper
import link.standen.michael.phonesaver.util.PreferenceHelper

/**
 * The entry point activity.
 * This activity shows the list of user selected folders.
 */
class FolderListActivity : ListActivity() {

	companion object {
		const val FOLDER_SELECT_REQUEST_CODE = 1
		const val PERMISSION_REQUEST_CODE = 2

		const val CHANGE_LOG_CSS = """
				body { padding: 0.8em; }
				h1 { margin-left: 0px; font-size: 1.2em; }
				ul { padding-left: 1.2em; }
				li { margin-left: 0px; }
			"""
	}

	private lateinit var log: DebugLogger

	private lateinit var adapter: DeletableLocationArrayAdapter
	private val folderList: MutableList<String> = ArrayList()

	private val preferenceHelper = PreferenceHelper(this)

	override fun onCreate(savedInstanceState: android.os.Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.folder_list_activity)
		val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)

		val fab = findViewById<android.support.design.widget.FloatingActionButton>(R.id.fab)
		fab.setOnClickListener {
			val intent = Intent(this@FolderListActivity, FolderSelectActivity::class.java)
			this@FolderListActivity.startActivityForResult(intent, FOLDER_SELECT_REQUEST_CODE)
		}

		log = DebugLogger(this)

		// Check for permissions
		testPermissions()

		// Show the thanks dialog
		preferenceHelper.loadPreferences()
		if (!PreferenceHelper.hideSupport) {
			DialogHelper.showSupportDialog(this)
		}

		// Show the change log over top of thanks
		showChangeLog(false)

		// Call to load lists
		onResume()
	}

	override fun onResume() {
		super.onResume()

		// Init list view
		val locationsWithData = folderList.map {
			LocationWithData(it)
		}.toMutableList()

		preferenceHelper.loadPreferences()

		if (PreferenceHelper.locationSelectEnabled){
			// Add select location to list view
			locationsWithData.add(0, LocationWithData(resources.getString(R.string.location_select_label), false))
		}

		adapter = DeletableLocationArrayAdapter(this, R.layout.folder_list_item, locationsWithData)
		adapter.registerDataSetObserver(object: DataSetObserver() {
			override fun onChanged() {
				// Reload the list
				loadFolderList()
				checkEmptyFolderList()
			}
		})
		findViewById<ListView>(android.R.id.list).adapter = adapter

		// Init list items
		loadFolderList()
	}

	/**
	 * Show the change log.
	 * Shows the full change log when nothing is in "What's New" log. Shows "What's New" log otherwise.
	 * @param force Force the change log to be displayed, if false only displayed if new content.
	 */
	private fun showChangeLog(force: Boolean) {
		val cl = ChangeLog(this, CHANGE_LOG_CSS)
		if (force || cl.isFirstRun) {
			if (cl.getChangeLog(false).size == 0){
				// Force the display of the full dialog list.
				cl.fullLogDialog.show()
			} else {
				// Show only the new stuff.
				cl.logDialog.show()
			}
		}
	}

	override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.folder_list_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean =
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		when (item.itemId) {
			R.id.action_credits -> {
				startActivity(Intent(this, CreditsActivity::class.java))
				true
			}
			R.id.action_settings -> {
				startActivity(Intent(this, SettingsActivity::class.java))
				true
			}
			R.id.action_support -> {
				DialogHelper.showSupportDialog(this)
				true
			}
			R.id.action_change_log -> {
				showChangeLog(true)
				true
			}
			else ->
				super.onOptionsItemSelected(item)
		}

	public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (data == null){
			return
		}
		if (resultCode == RESULT_OK) {
			if (requestCode == 1) {
				val folder = LocationHelper.removeRoot(data.getStringExtra(FolderSelectActivity.FOLDER_SELECTED))
				// Don't add duplicates
				if (!folderList.contains(folder)) {
					folderList.add(folder)
					folderList.sortBy { it.toLowerCase() }
					LocationHelper.saveFolderList(this, folderList)
					adapter.notifyDataSetChanged()
				}
			}
		}
	}

	/**
	 * Loads the list of folder paths.
	 */
	private fun loadFolderList(){
		folderList.clear()

		LocationHelper.loadFolderList(this)?.let {
			folderList.addAll(it)
		}
		checkEmptyFolderList()
	}

	/**
	 * Shows or hides the empty list layout as required.
	 */
	fun checkEmptyFolderList() {
		if (!PreferenceHelper.locationSelectEnabled && folderList.isEmpty()) {
			findViewById<View>(android.R.id.empty).visibility = View.VISIBLE
			findViewById<View>(android.R.id.list).visibility = View.GONE
		} else {
			findViewById<View>(android.R.id.empty).visibility = View.GONE
			findViewById<View>(android.R.id.list).visibility = View.VISIBLE
		}
	}

	/**
	 * Permissions checker
	 */
	private fun testPermissions() {
		if (Build.VERSION.SDK_INT >= 23) {
			val permissions = listOf(
					// All available permissions
					android.Manifest.permission.READ_EXTERNAL_STORAGE,
					android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
					android.Manifest.permission.INTERNET
				// Filter out granted permissions
			).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }.toTypedArray()
			if (permissions.isNotEmpty()){
				log.i("Requesting permission for " + permissions.reduce { total, next -> "$total, $next" })
				// Request permissions
				requestPermissions(permissions, PERMISSION_REQUEST_CODE)
			}
		} else {
			// Permission is automatically granted on sdk<23 upon installation
			log.v("Permission is granted")
		}
	}
}
