package com.liuxinyu.neurosleep

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.liuxinyu.neurosleep.databinding.ActivityMainBinding
import com.liuxinyu.neurosleep.feature.home.HomeFragment
import com.liuxinyu.neurosleep.feature.record.RecordFragment
import com.liuxinyu.neurosleep.feature.mine.MineFragment
import com.liuxinyu.neurosleep.feature.train.TrainFragment
import com.liuxinyu.neurosleep.feature.trend.TrendFragment



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 检查是否需要显示 HomeFragment
        if (intent.getBooleanExtra("showHomeFragment", false)) {
            navigateToFragment(HomeFragment())
            binding.bottomNavigationView.selectedItemId = R.id.navigation_home
        } else if (savedInstanceState == null) {
            navigateToFragment(HomeFragment()) // 只有第一次创建时加载HomeFragment
        }

        setupBottomNavigationView()
    }

    private fun setupBottomNavigationView() {
        // 设置底部导航栏选中监听
        binding.bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    navigateToFragment(HomeFragment())
                    true
                }

                R.id.navigation_record -> {
                    navigateToFragment(RecordFragment())
                    true
                }

                R.id.navigation_train -> {
                    navigateToFragment(TrainFragment())
                    true
                }

                R.id.navigation_trend -> {
                    navigateToFragment(TrendFragment())
                    true
                }

                R.id.navigation_mine -> {
                    navigateToFragment(MineFragment())
                    true
                }

                else -> false
            }
        }

        // 设置底部导航栏默认选中项为Home
        binding.bottomNavigationView.selectedItemId = R.id.navigation_home
    }

    private fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}