package com.colisa.podplay.fragments

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.colisa.podplay.R
import timber.log.Timber

class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(null)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }


    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        Timber.d("onSharedPreferenceChanged() - mtali")
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        Timber.d("onPreferenceClick() - mtali")
        return false
    }

    companion object {
        fun newInstance(): PreferencesFragment {
            return PreferencesFragment()
        }
    }
}