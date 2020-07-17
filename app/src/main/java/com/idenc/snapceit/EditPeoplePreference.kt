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
import android.content.DialogInterface
import android.os.Bundle
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.preference.MultiSelectListPreference
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat
import androidx.preference.PreferenceDialogFragmentCompat


class CustomListPreference(context: Context?, attrs: AttributeSet?) :
    MultiSelectListPreference(context, attrs) {

    override fun setEntries(entries: Array<out CharSequence>?) {
        super.setEntries(entries)

        persistStringSet(entries)
    }


    private fun onPrepareDialogBuilder(
        builder: Builder?,
        listener: DialogInterface.OnClickListener?
    ) {
    }

    private fun onDialogCreated(dialog: Dialog?) {}

    private fun onDialogStateRestored(
        dialog: Dialog?,
        savedInstanceState: Bundle?
    ) {
    }

    class CustomListPreferenceDialogFragment : MultiSelectListPreferenceDialogFragmentCompat(),
        AddPersonDialogFragment.MyDialogListener {
        private var mClickedItems = setOf<String>()
        private val customizablePreference: CustomListPreference
            get() = preference as CustomListPreference
        private val addPersonDialog = AddPersonDialogFragment()

        init {
            addPersonDialog.setTargetFragment(this, 0)

//            customizablePreference.entries = setOf<String>().toTypedArray()
//            customizablePreference.entryValues = setOf<String>().toTypedArray()
        }

//        override fun onPrepareDialogBuilder(builder: Builder) {
//            super.onPrepareDialogBuilder(builder)
//            customizablePreference.onPrepareDialogBuilder(builder, onItemClickListener)
////            builder.setPositiveButton(R.string.ok
////            ) { dialog, which -> onItemChosen() }
//        }

        override fun onPersonDialogPositiveClick(enteredName: String) {
            val entries = Array<CharSequence>(customizablePreference.entries.size + 1) { _ -> "" }
            for (i in customizablePreference.entries.indices) {
                entries[i] = customizablePreference.entries[i]
            }
            entries[entries.size - 1] = enteredName
            customizablePreference.entries = entries
            customizablePreference.entryValues = entries
        }


        fun newInstance(key: String?): CustomListPreferenceDialogFragment? {
            val fragment = CustomListPreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//            if (savedInstanceState != null) {
//                mClickedItems = savedInstanceState.getStringArrayList(
//                    KEY_CLICKED_ENTRIES
//                ) as Set<String>
//            }
            val dialog = activity?.let {
                val builder = Builder(it)
                builder.setTitle("Select People")

                builder.apply {
                    setPositiveButton(R.string.ok) { _, _ ->
                    }
                    setNeutralButton("Add person") { _, _ -> }
                    setNegativeButton(R.string.cancel) { _, _ ->
                    }
                }
                builder.create()

            } ?: throw IllegalStateException("Activity cannot be null")
            dialog.setOnShowListener {
                val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                neutralButton.setOnClickListener {
                    addPersonDialog.show(parentFragmentManager, "add_person")
                }
            }
            return dialog
        }

//        override fun onSaveInstanceState(outState: Bundle) {
//            super.onSaveInstanceState(outState)
//            outState.putStringArrayList(
//                KEY_CLICKED_ENTRIES,
//                mClickedItems as ArrayList<String>
//            )
//        }
//
//        override fun onActivityCreated(savedInstanceState: Bundle?) {
//            super.onActivityCreated(savedInstanceState)
//            customizablePreference.onDialogStateRestored(dialog, savedInstanceState)
//        }
//
//        private val onItemClickListener: DialogInterface.OnClickListener
//            get() = DialogInterface.OnClickListener { dialog, which ->
//                val item = customizablePreference.values.elementAt(which)
//                mClickedItems = if (mClickedItems.contains(item)) {
//                    mClickedItems.minus(item)
//                } else {
//                    mClickedItems.plus(item)
//                }
//            }

//        override fun onDialogClosed(positiveResult: Boolean) {
//            customizablePreference.onDialogClosed(positiveResult)
//            val preference: ListPreference = customizablePreference
//            val value: String? = value
//            if (positiveResult && value != null) {
//                if (preference.callChangeListener(value)) {
//                    preference.value = value
//                }
//            }
//        }

        companion object {
            private const val KEY_CLICKED_ENTRIES: String =
                "settings.CustomListPrefDialog.KEY_CLICKED_ENTRIES"

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

//    class ConfirmDialogFragment() : InstrumentedDialogFragment() {
//        fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//            return Builder(getActivity())
//                .setMessage(getArguments().getCharSequence(Intent.EXTRA_TEXT))
//                .setPositiveButton(R.string.ok, object : DialogInterface.OnClickListener {
//                    override fun onClick(dialog: DialogInterface, which: Int) {
//                        val f: Fragment? = getTargetFragment()
//                        if (f != null) {
//                            (f as CustomListPreferenceDialogFragment).onItemConfirmed()
//                        }
//                    }
//                })
//                .setNegativeButton(R.string.cancel, null)
//                .create()
//        }
//
//        val metricsCategory: Int
//            get() {
//                return SettingsEnums.DIALOG_CUSTOM_LIST_CONFIRMATION
//            }
//    }
}