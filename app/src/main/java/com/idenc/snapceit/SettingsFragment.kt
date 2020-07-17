package com.idenc.snapceit

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val peopleSelect = findPreference<MultiSelectListPreference>("select_people")
        
        peopleSelect?.setOnPreferenceChangeListener { preference: Preference, any: Any ->
            println("")
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {

        // Try if the preference is one of our custom Preferences
        var dialogFragment: DialogFragment? = null
        if (preference is CustomListPreference) {
            // Create a new instance of TimePreferenceDialogFragment with the key of the related
            // Preference
            dialogFragment =
                CustomListPreference.CustomListPreferenceDialogFragment.newInstance(preference.getKey())
        }
        if (dialogFragment != null) {
            // The dialog was created (it was one of our custom Preferences), show the dialog for it
            dialogFragment.setTargetFragment(this, 0)
            this.parentFragmentManager.let {
                dialogFragment.show(
                    it, "android.support.v7.preference" +
                            ".PreferenceFragment.DIALOG"
                )
            }
        } else {
            // Dialog creation could not be handled here. Try with the super method.
            super.onDisplayPreferenceDialog(preference)
        }
    }
}