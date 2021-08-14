package com.colisa.podplay.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.colisa.podplay.R
import com.colisa.podplay.databinding.ActivityMainBinding
import com.colisa.podplay.ui.viewmodels.PodcastViewModel
import com.colisa.podplay.ui.viewmodels.factory.ViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val podcastViewModel: PodcastViewModel by viewModels { ViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarLayout.toolbar)
        setupActionBarWithNavController(findNavController(R.id.main_nav_host))
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
                || findNavController(R.id.main_nav_host).navigateUp()
    }
}