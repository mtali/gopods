package com.colisa.podplay.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.colisa.podplay.R
import com.colisa.podplay.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
        return findNavController(R.id.main_nav_host).navigateUp()
                || super.onSupportNavigateUp()
    }
}