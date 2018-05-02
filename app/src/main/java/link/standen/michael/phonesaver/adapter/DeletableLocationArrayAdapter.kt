package link.standen.michael.phonesaver.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import link.standen.michael.phonesaver.R
import link.standen.michael.phonesaver.util.LocationHelper
import link.standen.michael.phonesaver.util.data.LocationWithData
import java.io.File

/**
 * Manages a list of deletable strings.
 */
class DeletableLocationArrayAdapter(context: Context, private val resourceId: Int, private val items: MutableList<LocationWithData>) :
		ArrayAdapter<LocationWithData>(context, resourceId, items) {

	private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

	override fun getItem(index: Int): LocationWithData? = items[index]

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = convertView?: inflater.inflate(resourceId, null)

		// Delete button
		val item = getItem(position)
		if (item == null || item.deletable) {
			view.findViewById<View>(R.id.delete).setOnClickListener {
				items.removeAt(position)
				notifyDataSetChanged()
				LocationHelper.saveFolderList(context, items.filter { !it.deletable }.map { it.location }.toMutableList())
			}
		} else {
			view.findViewById<View>(R.id.delete).visibility = View.GONE
		}

		// Description
		val s = item?.location
		view.findViewById<TextView>(R.id.description).text = if (s.isNullOrBlank()) File.separator else s

		return view
	}
}
