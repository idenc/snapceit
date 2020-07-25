package com.idenc.snapceit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageButton
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes

class PreferenceAdapter(
    context: Context,
    @LayoutRes private val layoutResource: Int,
    @IdRes private val textViewResourceId: Int = 0,
    private val values: MutableList<String>
) : ArrayAdapter<String>(context, layoutResource, values) {
    var onDeleteClick: ((String) -> Unit)? = null
    private val checkedItems = ArrayList<Boolean>()
    private val PREFS_NAME = "ListFile"
    private val mSharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        restoreCheckedItems()
    }

    override fun getItem(position: Int): String = values[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView ?: LayoutInflater.from(context).inflate(layoutResource, parent, false)

        val textView = view.findViewById<CheckedTextView>(textViewResourceId)
        val removeButton = view.findViewById<ImageButton>(R.id.prefDeleteButton)

        removeButton.setOnClickListener {
            removeItem(position)
        }

        textView.text = getItem(position)
        textView.isChecked = checkedItems[position]

        return view
    }

    private fun removeItem(position: Int) {
        onDeleteClick?.invoke(values[position])
        values.removeAt(position)
        checkedItems.removeAt(position)
        val editor = mSharedPrefs.edit()
        editor.putInt("checked_size", checkedItems.size)
        editor.remove("checked_$position")
        editor.apply()

        notifyDataSetChanged()
    }

    fun addItem(entry: String) {
        values.add(entry)
        checkedItems.add(true)
        val editor = mSharedPrefs.edit()
        editor.putInt("checked_size", checkedItems.size)
        editor.putBoolean("checked_$checkedItems.size", true)
        editor.apply()

        notifyDataSetChanged()
    }

    fun toggleItemChecked(position: Int) {
        checkedItems[position] = !checkedItems[position]
        val editor = mSharedPrefs.edit()
        editor.putBoolean("checked_$position", checkedItems[position])
        editor.apply()
    }

    private fun restoreCheckedItems() {
        var listSize = mSharedPrefs.getInt("checked_size", 0)
        if (listSize != values.size) {
            listSize = values.size
        }
        for (i in 0 until listSize) {
            checkedItems.add(mSharedPrefs.getBoolean("checked_$i", true))
        }
    }

}