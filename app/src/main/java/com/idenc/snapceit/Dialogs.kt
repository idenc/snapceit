package com.idenc.snapceit

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class PersonSelectorDialogFragment : DialogFragment() {
    private val selectedItems = HashMap<Int, ArrayList<Int>>() // Where we track the selected items
    private lateinit var listener: MyDialogListener
    var position = 0
    private val numPeople = 2
    private var lastSelection = BooleanArray(numPeople)

    interface MyDialogListener {
        fun onDialogPositiveClick(selectedPeople: ArrayList<Int>)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            var checkedItems = BooleanArray(numPeople)
            if (selectedItems.containsKey(position)) {
                for (i in 0 until numPeople) {
                    checkedItems[i] = selectedItems[position]!!.contains(i)
                }
            } else {
                checkedItems = lastSelection
            }

            // Set the dialog title
            builder.setTitle(R.string.select_users)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setMultiChoiceItems(R.array.userNames, checkedItems)
                { _, which, isChecked ->
                    if (!selectedItems.containsKey(position)) {
                        selectedItems[position] = ArrayList()
                    }
                    if (isChecked) {
                        // If the user checked the item, add it to the selected items
                        selectedItems[position]?.add(which)
                    } else if (selectedItems.contains(which)) {
                        // Else, if the item is already in the array, remove it
                        selectedItems[position]?.remove(Integer.valueOf(which))
                    }
                }
                .setPositiveButton(R.string.ok) { _, _ ->
                    selectedItems[position]?.let { items ->
                        for (i in 0 until numPeople) {
                            lastSelection[i] = items.contains(i)
                        }
                        listener.onDialogPositiveClick(items)
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = targetFragment as MyDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(
                (targetFragment.toString() +
                        " must implement NoticeDialogListener")
            )
        }
    }
}

class FinalSplitDialogFragment : DialogFragment() {

}