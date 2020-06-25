package com.idenc.snapceit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter(private val items: ArrayList<Pair<String, String>>) :
    RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    // class MyViewHolder(val textView: View) : RecyclerView.ViewHolder(textView)
    inner class MyViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val itemName: TextView = itemView.findViewById(R.id.item_name)
        val itemPrice: TextView = itemView.findViewById(R.id.item_price)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val context = parent.context;
        // create a new view
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_item_row, parent, false)

        return MyViewHolder(textView)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = items[position]
        val nameTextView = holder.itemName
        val priceTextView = holder.itemPrice
        nameTextView.text = item.first
        priceTextView.text = item.second
    }
}