package com.cocido.ramf.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cocido.ramf.ui.fragments.SingleChartFragment

class ChartsViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    
    private val chartParameters = listOf(
        "temperatura",
        "humedad", 
        "precipitacion",
        "vientoVel",
        "airPressure",
        "radiacion"
    )
    
    override fun getItemCount(): Int = chartParameters.size
    
    override fun createFragment(position: Int): Fragment {
        return SingleChartFragment.newInstance(chartParameters[position])
    }
}