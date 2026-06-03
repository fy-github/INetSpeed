package com.ikuai.inetspeed.core.ads

import android.content.Context
import android.view.View
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 广告管理器接口
 * 一期 NoOp 实现，不采集任何数据
 * 未来替换为真实广告 SDK 实现
 */
interface AdManager {
    fun createBannerAd(context: Context): View?
    fun showInterstitial(): Boolean
    fun isEnabled(): Boolean
}

@Singleton
class NoOpAdManager @Inject constructor() : AdManager {
    override fun createBannerAd(context: Context): View? = null
    override fun showInterstitial(): Boolean = false
    override fun isEnabled(): Boolean = false
}
