package link.standen.michael.phonesaver.activity

class FolderListActivity : android.support.v7.app.AppCompatActivity() {

	override fun onCreate(savedInstanceState: android.os.Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(link.standen.michael.phonesaver.R.layout.folder_list_activity)
		val toolbar = findViewById(link.standen.michael.phonesaver.R.id.toolbar) as android.support.v7.widget.Toolbar
		setSupportActionBar(toolbar)

		val fab = findViewById(link.standen.michael.phonesaver.R.id.fab) as android.support.design.widget.FloatingActionButton
		fab.setOnClickListener { _ ->
			val intent = android.content.Intent(this@FolderListActivity, FolderSelectActivity::class.java)
			this@FolderListActivity.startActivity(intent)
		}
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
}
