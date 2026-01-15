package com.astech.buysell

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.astech.buysell.databinding.ActivityMainBinding
import com.astech.buysell.fragments.AddProductFragment
import com.astech.buysell.fragments.StockFragment
import com.astech.buysell.fragments.ReportsFragment
import com.astech.buysell.fragments.SellFragment

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // Adjusted bottom for nav bar
            insets
        }
        
        setupBottomNavigation()
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(ReportsFragment())
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_sell -> SellFragment()
                R.id.nav_buy -> StockFragment()
                R.id.nav_add_product -> AddProductFragment()
                R.id.nav_view_reports -> ReportsFragment()
                else -> ReportsFragment()
            }
            loadFragment(fragment)
            true
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}