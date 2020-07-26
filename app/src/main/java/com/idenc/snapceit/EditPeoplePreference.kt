/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.idenc.snapceit

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.CheckedTextView
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.preference.MultiSelectListPreference
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat
import androidx.preference.PreferenceDialogFragmentCompat


class CustomListPreference(context: Context?, attrs: AttributeSet?) :
    MultiSelectListPreference(context, attrs) {
    private var mSharedPrefs = context!!.getSharedPreferences("select_people", Context.MODE_PRIVATE)
    private var myEntries: Set<String> = setOf()

    override fun onSetInitialValue(defaultValue: Any?) {
        myEntries = getPersistedStringSet(setOf<String>())
        val editor = mSharedPrefs.edit()
        editor.putStringSet("people_names", myEntries)
        editor.apply()
        entries = myEntries.toTypedArray()
        entryValues = myEntries.toTypedArray()
    }

    fun addEntry(entry: String) {
        myEntries = myEntries.plus(entry)
        val editor = mSharedPrefs.edit()
        editor.putStringSet("people_names", myEntries)
        editor.apply()
        persistStringSet(myEntries)
        entries = myEntries.toTypedArray()
        entryValues = myEntries.toTypedArray()
    }

    fun removeEntry(entry: String) {
        myEntries = myEntries.minus(entry)
        val editor = mSharedPrefs.edit()
        editor.putStringSet("people_names", myEntries)
        editor.apply()
        persistStringSet(myEntries)
        entries = myEntries.toTypedArray()
        entryValues = myEntries.toTypedArray()
    }

    class CustomListPreferenceDialogFragment : MultiSelectListPreferenceDialogFragmentCompat(),
        AddPersonDialogFragment.MyDialogListener {
        private val customizablePreference: CustomListPreference
            get() = preference as CustomListPreference
        private val addPersonDialog = AddPersonDialogFragment()
        private lateinit var myAdapter: PreferenceAdapter

        init {
            addPersonDialog.setTargetFragment(this, 0)
        }

        override fun onPersonDialogPositiveClick(enteredName: String) {
            customizablePreference.addEntry(enteredName)
            myAdapter.addItem(enteredName)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = Builder(it)
                myAdapter = PreferenceAdapter(
                    it,
                    R.layout.people_select_row,
                    R.id.prefPersonName,
                    customizablePreference.myEntries.toMutableList()
                )
                myAdapter.onDeleteClick = { entry ->
                    customizablePreference.removeEntry(entry)
                }

                builder.apply {
                    setTitle("Select People")
                    setAdapter(myAdapter, null)
                    setPositiveButton(R.string.ok) { _, _ ->
                    }
                    setNeutralButton("Add person") { _, _ -> }
                    setNegativeButton(R.string.cancel) { _, _ ->
                    }
                }

                val dialog = builder.create()

                val listView: ListView = dialog.listView
                listView.adapter = myAdapter
                listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
                listView.setOnItemClickListener { _: AdapterView<*>, v: View, i: Int, _: Long ->
                    val chkBox = v.findViewById<CheckedTextView>(R.id.prefPersonName)
                    chkBox.toggle()
                    myAdapter.toggleItemChecked(i)
                }

                dialog.setOnShowListener {
                    val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    neutralButton.setOnClickListener {
                        addPersonDialog.show(parentFragmentManager, "add_person")
                    }
                }

                return dialog
            } ?: throw IllegalStateException("Activity cannot be null")
        }

        companion object {
            fun newInstance(key: String?): CustomListPreferenceDialogFragment {
                val fragment =
                    CustomListPreferenceDialogFragment()
                val b = Bundle(1)
                b.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
                fragment.arguments = b
                return fragment
            }
        }

    }
}