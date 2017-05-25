package link.standen.michael.phonesaver.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import android.os.Parcelable

/**
 * An activity for selecting a folder in the file system.
 */
open class ListActivity : AppCompatActivity() {

	private val LIST_STATE = "listState"
	private var listState: Parcelable? = null

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

}
