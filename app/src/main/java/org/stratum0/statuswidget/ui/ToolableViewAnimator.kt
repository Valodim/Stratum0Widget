package org.stratum0.statuswidget.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ViewAnimator
import org.stratum0.statuswidget.R


/** This view is essentially identical to ViewAnimator, but allows specifying the initial view
 * for preview as an xml attribute.  */
class ToolableViewAnimator : ViewAnimator {

    private var mInitChild = -1

    var displayedChildId: Int
        get() = getChildAt(displayedChild).id
        set(id) {
            if (displayedChildId == id) {
                return
            }
            var i = 0
            val count = childCount
            while (i < count) {
                if (getChildAt(i).id == id) {
                    displayedChild = i
                    return
                }
                i++
            }
            val name = resources.getResourceEntryName(id)
            throw IllegalArgumentException("No view with ID " + name)
        }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        if (isInEditMode) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ToolableViewAnimator)
            mInitChild = a.getInt(R.styleable.ToolableViewAnimator_initialView, -1)
            a.recycle()
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs) {

        if (isInEditMode) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ToolableViewAnimator, defStyleAttr, 0)
            mInitChild = a.getInt(R.styleable.ToolableViewAnimator_initialView, -1)
            a.recycle()
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (isInEditMode && mInitChild-- > 0) {
            return
        }
        super.addView(child, index, params)
    }

    override fun setDisplayedChild(whichChild: Int) {
        if (whichChild != displayedChild) {
            super.setDisplayedChild(whichChild)
        }
    }

    fun setDisplayedChild(whichChild: Int, animate: Boolean) {
        if (animate) {
            displayedChild = whichChild
            return
        }

        val savedInAnim = inAnimation
        val savedOutAnim = outAnimation
        inAnimation = null
        outAnimation = null

        displayedChild = whichChild

        inAnimation = savedInAnim
        outAnimation = savedOutAnim
    }
}
