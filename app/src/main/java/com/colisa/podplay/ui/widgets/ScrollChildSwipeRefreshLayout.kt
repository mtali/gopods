package com.colisa.podplay.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class ScrollChildSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null
) : SwipeRefreshLayout(context, attr) {
    var scrollUpChild: View? = null

    override fun canChildScrollUp(): Boolean {
        return scrollUpChild?.canScrollVertically(-1) ?: super.canChildScrollUp()
    }
}