package com.idenc.snapceit

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.abhinay.input.CurrencyEditText

class PersonSelectorDialogFragment : DialogFragment() {
    private val selectedItems = HashMap<Int, ArrayList<Int>>() // Where we track the selected items
    private lateinit var listener: MyDialogListener
    var position = 0
    private var numPeople: Int = 0
    private var lastSelection = BooleanArray(numPeople)

    interface MyDialogListener {
        fun onPersonDialogPositiveClick(selectedPeople: ArrayList<Int>)
    }

    private fun getPeople(activity: FragmentActivity): Array<String> {
        val peopleNames = activity.getSharedPreferences(
            "select_people",
            Context.MODE_PRIVATE
        ).getStringSet("people_names", setOf())!!.toMutableList()
        val checkedPeoplePref = activity.getSharedPreferences("ListFile", Context.MODE_PRIVATE)
        val numItems = checkedPeoplePref.getInt("checked_size", 0)
        for (i in 0 until numItems) {
            if (!checkedPeoplePref.getBoolean("checked_$i", true)) {
                peopleNames.removeAt(i)
            }
        }
        if (numPeople == 0) {
            lastSelection = BooleanArray(peopleNames.size)
        }
        numPeople = peopleNames.size
        return peopleNames.toTypedArray()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val peopleNames = getPeople(it)
            var checkedItems = BooleanArray(numPeople)
            if (selectedItems.containsKey(position)) {
                for (i in 0 until numPeople) {
                    checkedItems[i] = selectedItems[position]!!.contains(i)
                }
            } else {
                checkedItems = lastSelection
                selectedItems[position] = ArrayList()
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        selectedItems[position]?.add(i)
                    }
                }
            }

            // Set the dialog title
            builder.setTitle(R.string.select_users)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setMultiChoiceItems(peopleNames, checkedItems)
                { _, which, isChecked ->
                    if (isChecked) {
                        // If the user checked the item, add it to the selected items
                        selectedItems[position]?.add(which)
                    } else if (selectedItems[position]!!.contains(which)) {
                        // Else, if the item is already in the array, remove it
                        selectedItems[position]?.remove(Integer.valueOf(which))
                    }
                }
                .setPositiveButton(R.string.ok) { _, _ ->
                    if (!selectedItems.containsKey(position)) {
                        selectedItems[position] = ArrayList()
                        for (i in checkedItems.indices) {
                            if (checkedItems[i]) {
                                selectedItems[position]?.add(i)
                            }
                        }
                    }
                    selectedItems[position]?.let { items ->
                        for (i in 0 until numPeople) {
                            lastSelection[i] = items.contains(i)
                        }
                        listener.onPersonDialogPositiveClick(items)
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

class FinalSplitDialogFragment(private val people: ArrayList<Person>) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { fragmentActivity ->
            val builder = AlertDialog.Builder(fragmentActivity)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.person_split_recyclerview, null, false)
            val recyclerView = view.findViewById<RecyclerView>(R.id.personRecycler)
            if (!people.any { p -> p.name == "Total" }) {
                people.add(Person("Total", people.sumByDouble { it.owedPrice }))
            } else {
                val total = people.find { p -> p.name == "Total" }
                if (total != null) {
                    total.owedPrice = people.sumByDouble { it.owedPrice }
                }
            }
            val adapter = FinalSplitRecyclerAdapter(people)
            recyclerView.setHasFixedSize(false)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
            adapter.notifyDataSetChanged()

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.ok) { _, _ ->
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class AddTaxDialogFragment : DialogFragment() {
    private lateinit var listener: MyDialogListener
    private var currentTax = 0.0

    interface MyDialogListener {
        fun onTaxDialogPositiveClick(enteredTax: Double)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Add Tax")

            val input = CurrencyEditText(it)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.setCurrency("$")
            input.setText((currentTax * 10).toString())
            builder.setView(input)

            builder.apply {
                setPositiveButton(R.string.ok) { _, _ ->
                    currentTax = input.cleanDoubleValue
                    listener.onTaxDialogPositiveClick(currentTax)
                }
                setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
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

class AddItemDialogFragment : DialogFragment() {
    private lateinit var listener: MyDialogListener

    interface MyDialogListener {
        fun onAddItemDialogPositiveClick(itemName: String, itemPrice: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Add Receipt Item")
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.add_item_layout, null, false)
            val itemNameInput = view.findViewById<EditText>(R.id.addItemNameInput)
            val itemPriceInput = view.findViewById<CurrencyEditText>(R.id.addItemPriceInput)
            itemPriceInput.setCurrency("$")
            builder.setView(view)

            builder.apply {
                setPositiveButton(R.string.ok) { _, _ -> }
                setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
            }
            val dialog = builder.create()
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val nameText = itemNameInput.text.toString().trim()
                    val itemPriceText = itemPriceInput.text.toString().trim()
                    var inputsValidated = true
                    if (nameText.isEmpty()) {
                        itemNameInput.error = "Item Name cannot be empty"
                        inputsValidated = false
                    }
                    if (itemPriceText.isEmpty()) {
                        itemPriceInput.error = "Item Price cannot be empty"
                        inputsValidated = false
                    }
                    if (inputsValidated) {
                        listener.onAddItemDialogPositiveClick(
                            nameText,
                            itemPriceText
                        )
                        dismiss()
                    }
                }
            }

            return dialog

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

class AddPersonDialogFragment : DialogFragment() {
    private lateinit var listener: MyDialogListener

    interface MyDialogListener {
        fun onPersonDialogPositiveClick(enteredName: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Add Person")

            val input = EditText(it)
            input.isSelected = true
            builder.setView(input)

            builder.apply {
                setPositiveButton(R.string.ok) { _, _ ->
                    listener.onPersonDialogPositiveClick(input.text.toString())
                }
                setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
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