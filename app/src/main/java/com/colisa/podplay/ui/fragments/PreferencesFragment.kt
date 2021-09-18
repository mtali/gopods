package com.colisa.podplay.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.colisa.podplay.R
import com.colisa.podplay.ui.UIControlInterface
import com.colisa.podplay.util.ThemeUtils
import timber.log.Timber

class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private var themePreference: Preference? = null

    private lateinit var uiControlInterface: UIControlInterface

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            uiControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        themePreference = findPreference<Preference>(getString(R.string.pref_theme))?.apply {
            icon = ContextCompat.getDrawable(
                requireContext(),
                ThemeUtils.resolveThemeIcon(requireContext())
            )
        }
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
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.pref_theme) -> {
                themePreference?.icon = ContextCompat.getDrawable(
                    requireContext(),
                    ThemeUtils.resolveThemeIcon(requireActivity())
                )
                uiControlInterface.onAppearanceChanged(isThemeChanged = true)

            }
        }
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