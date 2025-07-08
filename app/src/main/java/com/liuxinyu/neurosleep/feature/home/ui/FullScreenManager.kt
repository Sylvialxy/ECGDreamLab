package com.liuxinyu.neurosleep.feature.home.ui

import android.content.pm.ActivityInfo
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.feature.home.view.EcgShowView

class FullScreenManager(
    private val activity: AppCompatActivity,
    private val rootLayout: ConstraintLayout,
    private val ecgFrame: FrameLayout,
    private val hrvFrame: FrameLayout,
    private val ecgView: EcgShowView
) {
    private var isEcgFullScreen = false
    private var isHrvFullScreen = false
    private var originalEcgConstraints: ConstraintLayout.LayoutParams? = null
    private var originalHrvConstraints: ConstraintLayout.LayoutParams? = null

    init {
        // 保存原始布局参数
        originalEcgConstraints = ecgFrame.layoutParams as? ConstraintLayout.LayoutParams
        originalHrvConstraints = hrvFrame.layoutParams as? ConstraintLayout.LayoutParams
    }

    fun toggleEcgFullScreen(): Boolean {
        if (isEcgFullScreen) {
            exitEcgFullScreen()
        } else {
            enterEcgFullScreen()
        }
        isEcgFullScreen = !isEcgFullScreen
        return isEcgFullScreen
    }

    fun toggleHrvFullScreen(): Boolean {
        if (isHrvFullScreen) {
            exitHrvFullScreen()
        } else {
            enterHrvFullScreen()
        }
        isHrvFullScreen = !isHrvFullScreen
        return isHrvFullScreen
    }

    private fun enterEcgFullScreen() {
        // 保存原始 layoutParams
        originalEcgConstraints = ecgFrame.layoutParams as? ConstraintLayout.LayoutParams

        // 隐藏所有其他视图
        hrvFrame.visibility = View.GONE
        activity.findViewById<View>(R.id.recycler_view).visibility = View.GONE
        activity.findViewById<View>(R.id.status_card).visibility = View.GONE
        activity.findViewById<View>(R.id.control_card).visibility = View.GONE
        activity.findViewById<View>(R.id.bleButton).visibility = View.GONE
        activity.findViewById<View>(R.id.chart_selector).visibility = View.GONE
        activity.findViewById<View>(R.id.chart_selector2).visibility = View.GONE
        activity.findViewById<View>(R.id.returnHomeButton).visibility = View.GONE
        activity.findViewById<View>(R.id.promptmodule).visibility = View.GONE
        activity.findViewById<View>(R.id.selectormodule).visibility = View.GONE

        // 使用 ConstraintSet 设置全屏约束
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        // 清除所有约束
        constraintSet.clear(ecgFrame.id, ConstraintSet.TOP)
        constraintSet.clear(ecgFrame.id, ConstraintSet.BOTTOM)
        constraintSet.clear(ecgFrame.id, ConstraintSet.START)
        constraintSet.clear(ecgFrame.id, ConstraintSet.END)

        // 设置全屏约束
        constraintSet.connect(ecgFrame.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(ecgFrame.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.connect(ecgFrame.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(ecgFrame.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // 设置宽高为 MATCH_CONSTRAINT
        constraintSet.constrainWidth(ecgFrame.id, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(ecgFrame.id, ConstraintSet.MATCH_CONSTRAINT)

        // 应用约束
        constraintSet.applyTo(rootLayout)

        // 强制横屏
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 更新显示模式
        ecgView.SHOW_MODEL = EcgShowView.SHOW_MODEL_ALL

        // 强制刷新布局
        ecgFrame.requestLayout()
    }

    private fun exitEcgFullScreen() {
        // 显示所有其他视图
        hrvFrame.visibility = View.VISIBLE
        activity.findViewById<View>(R.id.recycler_view).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.status_card).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.control_card).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.bleButton).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.chart_selector).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.chart_selector2).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.returnHomeButton).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.promptmodule).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.selectormodule).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.btnZoomIn).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.btnZoomOut).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.hrvbtnZoomIn).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.hrvbtnZoomOut).visibility = View.VISIBLE

        // 恢复原始 layoutParams
        originalEcgConstraints?.let {
            ecgFrame.layoutParams = it
            ecgFrame.requestLayout()
        }

        // 恢复竖屏
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 更新显示模式
        ecgView.SHOW_MODEL = EcgShowView.SHOW_MODEL_DYNAMIC_SCROLL
    }

    private fun enterHrvFullScreen() {
        // 保存原始 layoutParams
        originalHrvConstraints = hrvFrame.layoutParams as? ConstraintLayout.LayoutParams

        // 隐藏所有其他视图
        ecgFrame.visibility = View.GONE
        activity.findViewById<View>(R.id.recycler_view).visibility = View.GONE
        activity.findViewById<View>(R.id.status_card).visibility = View.GONE
        activity.findViewById<View>(R.id.control_card).visibility = View.GONE
        activity.findViewById<View>(R.id.bleButton).visibility = View.GONE
        activity.findViewById<View>(R.id.chart_selector).visibility = View.GONE
        activity.findViewById<View>(R.id.chart_selector2).visibility = View.GONE
        activity.findViewById<View>(R.id.returnHomeButton).visibility = View.GONE
        activity.findViewById<View>(R.id.promptmodule).visibility = View.GONE
        activity.findViewById<View>(R.id.selectormodule).visibility = View.GONE

        // 使用 ConstraintSet 设置全屏约束
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        // 清除所有约束
        constraintSet.clear(hrvFrame.id, ConstraintSet.TOP)
        constraintSet.clear(hrvFrame.id, ConstraintSet.BOTTOM)
        constraintSet.clear(hrvFrame.id, ConstraintSet.START)
        constraintSet.clear(hrvFrame.id, ConstraintSet.END)

        // 设置全屏约束
        constraintSet.connect(hrvFrame.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(hrvFrame.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.connect(hrvFrame.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(hrvFrame.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // 设置宽高为 MATCH_CONSTRAINT
        constraintSet.constrainWidth(hrvFrame.id, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(hrvFrame.id, ConstraintSet.MATCH_CONSTRAINT)

        // 应用约束
        constraintSet.applyTo(rootLayout)

        // 强制横屏
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 强制刷新布局
        hrvFrame.requestLayout()
    }

    private fun exitHrvFullScreen() {
        // 显示所有其他视图
        ecgFrame.visibility = View.VISIBLE
        activity.findViewById<View>(R.id.recycler_view).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.status_card).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.control_card).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.bleButton).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.chart_selector).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.chart_selector2).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.returnHomeButton).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.promptmodule).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.selectormodule).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.btnZoomIn).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.btnZoomOut).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.hrvbtnZoomIn).visibility = View.VISIBLE
        activity.findViewById<View>(R.id.hrvbtnZoomOut).visibility = View.VISIBLE

        // 恢复原始 layoutParams
        originalHrvConstraints?.let {
            hrvFrame.layoutParams = it
            hrvFrame.requestLayout()
        }

        // 恢复竖屏
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun isInEcgFullScreen(): Boolean = isEcgFullScreen
    
    fun isInHrvFullScreen(): Boolean = isHrvFullScreen
} 