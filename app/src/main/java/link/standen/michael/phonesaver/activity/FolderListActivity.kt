package link.standen.michael.phonesaver.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.database.DataSetObserver
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ListView
import link.standen.michael.phonesaver.R
import link.standen.michael.phonesaver.adapter.DeletableStringArrayAdapter
import link.standen.michael.phonesaver.util.LocationHelper

class FolderListActivity : ListActivity() {

	companion object {
		const val TAG = "FolderListActivity"
		const val FOLDER_SELECT_REQUEST_CODE = 1
		const val PERMISSION_REQUEST_CODE = 2
	}

	private lateinit var adapter: DeletableStringArrayAdapter

	private val folderList: MutableList<String> = ArrayList()

	override fun onCreate(savedInstanceState: android.os.Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(link.standen.michael.phonesaver.R.layout.folder_list_activity)
		val toolbar = findViewById(link.standen.michael.phonesaver.R.id.toolbar) as android.support.v7.widget.Toolbar
		setSupportActionBar(toolbar)

		val fab = findViewById(link.standen.michael.phonesaver.R.id.fab) as android.support.design.widget.FloatingActionButton
		fab.setOnClickListener { _ ->
			val intent = android.content.Intent(this@FolderListActivity, FolderSelectActivity::class.java)
			this@FolderListActivity.startActivityForResult(intent, FOLDER_SELECT_REQUEST_CODE)
		}

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
		(findViewById(android.R.id.list) as ListView).adapter = adapter
	}

	override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(link.standen.michael.phonesaver.R.menu.folder_list_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		when (item.itemId) {
			R.id.action_credits ->
				{
					startActivity(Intent(this, CreditsActivity::class.java))
					return true
				}
			R.id.action_settings ->
				{
					startActivity(Intent(this, SettingsActivity::class.java))
					return true
				}
			else ->
				return super.onOptionsItemSelected(item)
		}
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
			findViewById(android.R.id.empty).visibility = View.VISIBLE
		} else {
			findViewById(android.R.id.empty).visibility = View.GONE
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
				Log.i(TAG, "Requesting permission for " + permissions.reduce { total, next -> total + ", " + next })
				// Request permissions
				requestPermissions(permissions, PERMISSION_REQUEST_CODE)
			}
		} else {
			// Permission is automatically granted on sdk<23 upon installation
			Log.v(TAG, "Permission is granted")
		}
	}
}
