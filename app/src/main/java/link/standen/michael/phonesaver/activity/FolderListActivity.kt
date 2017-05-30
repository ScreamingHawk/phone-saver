package link.standen.michael.phonesaver.activity

import android.content.Intent
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ListView
import link.standen.michael.phonesaver.util.LocationHelper

class FolderListActivity : ListActivity() {

	private val TAG = "FolderListActivity"

	private val FOLDER_SELECT_REQUEST_CODE = 1

	private lateinit var listView: ListView

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

		// Init list view
		listView = findViewById(android.R.id.list) as ListView
		listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
			//TODO Do something with it?
		}

		loadFolderList()

		listView.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, folderList)
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
		val id = item.itemId

		if (id == link.standen.michael.phonesaver.R.id.action_settings) {
			return true
		}

		return super.onOptionsItemSelected(item)
	}

	public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
		super.onActivityResult(requestCode, resultCode, data)
		if (resultCode == RESULT_OK) {
			if (requestCode == 1) {
				val folder = LocationHelper.removeRoot(data.getStringExtra(FolderSelectActivity.FOLDER_SELECTED))
				// Don't add duplicates
				if (!folderList.contains(folder)) {
					folderList.add(folder)
					folderList.sort()
					if (listView.adapter is BaseAdapter) {
						(listView.adapter as BaseAdapter).notifyDataSetChanged()
					}
				}
				LocationHelper.saveFolderList(this, folderList)
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
	}
}
