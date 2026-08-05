package com.colisa.podplay.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.colisa.podplay.R
import com.colisa.podplay.databinding.FragmentSettingsBinding
import com.colisa.podplay.ui.UIControlInterface

class SettingsFragment : Fragment() {
    private var fragmentSettingsBinding: FragmentSettingsBinding? = null

    private lateinit var uiControlInterface: UIControlInterface

    private var preferenceFragment: PreferencesFragment? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Make sure the container activity has implemented
        // callback interface
        try {
            uiControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentSettingsBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return fragmentSettingsBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentSettingsBinding?.settingsToolbar?.run {
            inflateMenu(R.menu.settings_menu)
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
            setOnMenuItemClickListener {
                // TODO: Implement menu click
                return@setOnMenuItemClickListener true
            }

            preferenceFragment = PreferencesFragment.newInstance()
            preferenceFragment?.let { fm ->
                childFragmentManager.commit {
                    replace(R.id.fragment_layout, fm)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentSettingsBinding = null
    }
}