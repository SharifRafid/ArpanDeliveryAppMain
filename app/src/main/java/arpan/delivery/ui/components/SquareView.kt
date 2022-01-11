package arpan.delivery.ui.components

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView

class SquareView: CardView {

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (widthMeasureSpec < heightMeasureSpec)
            super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        else
            super.onMeasure(heightMeasureSpec, heightMeasureSpec)
    }
}