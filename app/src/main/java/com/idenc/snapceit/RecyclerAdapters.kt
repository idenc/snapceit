package com.idenc.snapceit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.ivbaranov.mli.MaterialLetterIcon
import me.abhinay.input.CurrencyEditText


class ItemRecyclerAdapter(private val items: ArrayList<Item>) :
    RecyclerView.Adapter<ItemRecyclerAdapter.MyViewHolder>() {
    var onAssignClick: ((Int) -> Unit)? = null
    var onDeleteClick: ((Int) -> Unit)? = null
    var onEditPrice: ((Int, String) -> Unit)? = null
    var onEditName: ((Int, String) -> Unit)? = null

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    // class MyViewHolder(val textView: View) : RecyclerView.ViewHolder(textView)
    inner class MyViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val itemName: EditText = itemView.findViewById(R.id.item_name)
        val itemPrice: CurrencyEditText = itemView.findViewById(R.id.item_price)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val assignButton: Button = itemView.findViewById(R.id.assign_button)
        val peopleString: TextView = itemView.findViewById(R.id.assigned_people)

        init {
            assignButton.setOnClickListener {
                onAssignClick?.invoke(this.adapterPosition)
            }

            itemPrice.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    onEditPrice?.invoke(
                        this.adapterPosition,
                        (v as CurrencyEditText).text.toString()
                    )
                }
            }
            itemName.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    // the user is done typing.
                    onEditName?.invoke(
                        this.adapterPosition,
                        (v as EditText).text.toString()
                    )
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        // create a new view
        val rowView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_item_row, parent, false)

        return MyViewHolder(rowView)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = items[position]
        val nameTextView = holder.itemName
        val priceTextView = holder.itemPrice
        priceTextView.setCurrency("$")
        nameTextView.setText(item.itemName)
        priceTextView.setText(item.itemPrice)
        holder.peopleString.text = item.peopleSplitting.joinToString { it.name }

        holder.deleteButton.setOnClickListener {
            onDeleteClick?.invoke(holder.adapterPosition)
            removeItem(holder.adapterPosition)
        }
    }

    private fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }
}

class FinalSplitRecyclerAdapter(private val items: ArrayList<Person>) :
    RecyclerView.Adapter<FinalSplitRecyclerAdapter.MyViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    // class MyViewHolder(val textView: View) : RecyclerView.ViewHolder(textView)
    inner class MyViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val personIcon: MaterialLetterIcon = itemView.findViewById(R.id.personIcon)
        val personName: TextView = itemView.findViewById(R.id.personName)
        val personPrice: TextView = itemView.findViewById(R.id.personPrice)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        // create a new view
        val rowView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_person_row, parent, false)

        return MyViewHolder(rowView)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = items[position]
        val nameTextView = holder.personName
        val priceTextView = holder.personPrice
        val icon = holder.personIcon

        nameTextView.text = item.name
        priceTextView.text = item.getPriceString()
        icon.letter = item.name
    }
}