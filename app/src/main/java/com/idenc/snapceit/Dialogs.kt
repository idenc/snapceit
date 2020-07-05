package com.idenc.snapceit

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class PersonSelectorDialogFragment : DialogFragment() {
    private val selectedItems = ArrayList<Int>() // Where we track the selected items
    private lateinit var listener: MyDialogListener

    interface MyDialogListener {
        fun onDialogPositiveClick(selectedPeople: ArrayList<Int>)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val numPeople = 2
            val checkItems = BooleanArray(numPeople)
            println(selectedItems)
            for (i in 0 until numPeople) {
                checkItems[i] = selectedItems.contains(i)
            }
            // Set the dialog title
            builder.setTitle(R.string.select_users)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setMultiChoiceItems(R.array.userNames, checkItems)
                { _, which, isChecked ->
                    if (isChecked) {
                        // If the user checked the item, add it to the selected items
                        selectedItems.add(which)
                    } else if (selectedItems.contains(which)) {
                        // Else, if the item is already in the array, remove it
                        selectedItems.remove(Integer.valueOf(which))
                    }
                }
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener.onDialogPositiveClick(selectedItems)
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