package com.astech.buysell.utils

import android.content.Context
import android.widget.TextView
import com.astech.buysell.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)

    // Method for setting the content every time it is drawn
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            tvContent.text = "৳${e.y.toInt()}"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Center the marker horizontally and position it above the entry
        return MPPointF(-(width / 2).toFloat(), -height.toFloat() - 15f)
    }
}
