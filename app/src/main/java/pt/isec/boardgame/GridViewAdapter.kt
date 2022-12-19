package pt.isec.boardgame

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView


internal class GridViewAdapter(
    private val items: ArrayList<Any>,
    private val context: Context
) :
    BaseAdapter() {
    private var layoutInflater: LayoutInflater? = null
    private lateinit var itemTV: TextView
    var selectedPositions: ArrayList<Int> = ArrayList()

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any? {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }

    // Get individual item of GridView
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var convertView = convertView
        // Initialize layout inflater if null
        if (layoutInflater == null) {
            layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }
        // Initialize convertView if null
        if (convertView == null) {
            // Pass the layout file which we have to inflate for each item of grid view
            convertView = layoutInflater!!.inflate(R.layout.card_item, null)
        }

        itemTV = convertView!!.findViewById(R.id.tvItem)
        itemTV.text = items[position].toString()
        itemTV.setBackgroundColor(if (selectedPositions.contains(position)) Color.rgb(150,150,255) else Color.WHITE)

        return convertView
    }

    fun selectedPositions(array : ArrayList<Int>) { this.selectedPositions = array }

    fun clearSelectedPositions() { this.selectedPositions.clear() }
}