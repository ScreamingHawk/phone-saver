package link.standen.michael.phonesaver.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import link.standen.michael.phonesaver.R
import link.standen.michael.phonesaver.util.LocationHelper
import java.io.File

/**
 * Manages a list of deletable strings.
 */
class DeletableStringArrayAdapter(context: Context, private val resourceId: Int, private val items: MutableList<String>) :
		ArrayAdapter<String>(context, resourceId, items) {

	private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

	override fun getItem(index: Int): String? {
		return items[index]
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = convertView?: inflater.inflate(resourceId, null)

		// Delete button
		view.findViewById(R.id.delete).setOnClickListener {
			items.removeAt(position)
			notifyDataSetChanged()
			LocationHelper.saveFolderList(context, items)
		}

		// Description
		val s = getItem(position)
		(view.findViewById(R.id.description) as TextView).text = if (s.isNullOrBlank()) File.separator else s

		return view
	}
}
