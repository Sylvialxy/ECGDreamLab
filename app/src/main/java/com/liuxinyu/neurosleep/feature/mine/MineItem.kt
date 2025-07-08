package com.liuxinyu.neurosleep.feature.mine

data class MineItem(
    val iconResId: Int,
    val title: String,
    val action: () -> Unit // 点击事件的回调
)
