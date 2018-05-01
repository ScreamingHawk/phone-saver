package link.standen.michael.phonesaver.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.database.DataSetObserver
import android.os.Build
import android.view.View
import android.widget.ListView
import de.cketti.library.changelog.ChangeLog
import link.standen.michael.phonesaver.R
import link.standen.michael.phonesaver.adapter.DeletableStringArrayAdapter
import link.standen.michael.phonesaver.util.DebugLogger
import link.standen.michael.phonesaver.util.LocationHelper

/**
 * The entry point activity.
 * This activity shows the list of user selected folders.
 */
class FolderListActivity : ListActivity() {

	companion object {
		const val TAG = "FolderListActivity"
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

	private lateinit var adapter: DeletableStringArrayAdapter

	private val folderList: MutableList<String> = ArrayList()

	override fun onCreate(savedInstanceState: android.os.Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(link.standen.michael.phonesaver.R.layout.folder_list_activity)
		val toolbar = findViewById<android.support.v7.widget.Toolbar>(link.standen.michael.phonesaver.R.id.toolbar)
		setSupportActionBar(toolbar)

		val fab = findViewById<android.support.design.widget.FloatingActionButton>(link.standen.michael.phonesaver.R.id.fab)
		fab.setOnClickListener { _ ->
			val intent = android.content.Intent(this@FolderListActivity, FolderSelectActivity::class.java)
			this@FolderListActivity.startActivityForResult(intent, FOLDER_SELECT_REQUEST_CODE)
		}

		log = DebugLogger(TAG, this)

		// Check for permissions
		testPermissions()

		// Init list items
		loadFolderList()

		// Init list view
		adapter = DeletableStringArrayAdapter(this, R.layout.folder_list_item, folderList)
		adapter.registerDataSetObserver(object: DataSetObserver() {
			override fun onChanged() {
				checkEmptyCharacterList()
			}
		})
		findViewById<ListView>(android.R.id.list).adapter = adapter

		// Show the change log
		showChangeLog(false)
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
		menuInflater.inflate(link.standen.michael.phonesaver.R.menu.folder_list_menu, menu)
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
			R.id.action_change_log -> {
				showChangeLog(true)
				true
			}
			else ->
				super.onOptionsItemSelected(item)
		}

	public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
		super.onActivityResult(requestCode, resultCode, data)
		if (resultCode == RESULT_OK) {
			if (requestCode == 1) {
				val folder = LocationHelper.removeRoot(data.getStringExtra(FolderSelectActivity.FOLDER_SELECTED))
				// Don't add duplicates
				if (!folderList.contains(folder)) {
					folderList.add(folder)
					folderList.sortBy { it.toLowerCase() }
					adapter.notifyDataSetChanged()
					LocationHelper.saveFolderList(this, folderList)
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
		checkEmptyCharacterList()
	}

	/**
	 * Shows or hides the empty list layout as required.
	 */
	fun checkEmptyCharacterList() {
		if (folderList.isEmpty()) {
			findViewById<View>(android.R.id.empty).visibility = View.VISIBLE
		} else {
			findViewById<View>(android.R.id.empty).visibility = View.GONE
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
