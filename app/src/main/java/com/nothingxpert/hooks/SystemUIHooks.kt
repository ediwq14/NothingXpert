package com.nothingxpert.hooks

import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SystemUIHooks : BaseHook() {
    override val tag = "SystemUI"

    override fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("NothingXpert/SystemUI: install() called for ${lpparam.packageName}")
        
        if (lpparam.packageName != SYSTEMUI_PKG) {
            XposedBridge.log("NothingXpert/SystemUI: Not SystemUI, skipping")
            return
        }

        log("Installing SystemUI hooks")
        installPrivacyIndicatorsHook(lpparam)
        installQSBottomButtonsHook(lpparam)
        installEmergencyCallHook(lpparam)
        log("SystemUI hooks installed")
    }

    private fun installPrivacyIndicatorsHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("Installing privacy indicators hook")
        
        safeHook("StatusBar.onCreate") {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.StatusBar",
                lpparam.classLoader,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (getPreferenceBoolean(PREF_HIDE_PRIVACY_INDICATORS, false)) {
                            log("StatusBar created, will hide privacy indicators")
                            val statusBar = param.thisObject as? ViewGroup ?: return
                            hidePrivacyIndicatorsRecursive(statusBar)
                        }
                    }
                }
            )
        }

        safeHook("PhoneStatusBarView.onFinishInflate") {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                lpparam.classLoader,
                "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (getPreferenceBoolean(PREF_HIDE_PRIVACY_INDICATORS, false)) {
                            val view = param.thisObject as? ViewGroup ?: return
                            hidePrivacyIndicatorsRecursive(view)
                        }
                    }
                }
            )
        }
    }

    private fun hidePrivacyIndicatorsRecursive(viewGroup: ViewGroup) {
        try {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i) ?: continue
                val className = child.javaClass.name.lowercase()
                
                if (className.contains("privacy") || className.contains("indicator") || 
                    className.contains("pill") || className.contains("chip")) {
                    child.visibility = View.GONE
                    log("Hid privacy indicator: ${child.javaClass.name}")
                }
                
                if (child is ViewGroup) {
                    hidePrivacyIndicatorsRecursive(child)
                }
            }
        } catch (e: Exception) {
            log("Error hiding privacy indicators: $e")
        }
    }

    private fun installQSBottomButtonsHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("Installing QS bottom buttons hook")
        
        safeHook("QSPanel.onFinishInflate") {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.qs.QSPanel",
                lpparam.classLoader,
                "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (getPreferenceBoolean(PREF_HIDE_QS_BOTTOM_BTN, false)) {
                            val view = param.thisObject as? ViewGroup ?: return
                            hideQSBottomButtons(view)
                        }
                    }
                }
            )
        }
    }

    private fun hideQSBottomButtons(viewGroup: ViewGroup) {
        try {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i) ?: continue
                val className = child.javaClass.name.lowercase()
                
                if (className.contains("activeapp") || className.contains("running") ||
                    className.contains("footer") || className.contains("dragger") ||
                    className.contains("puck")) {
                    child.visibility = View.GONE
                    log("Hid QS bottom button: ${child.javaClass.name}")
                }
                
                if (child is ViewGroup) {
                    hideQSBottomButtons(child)
                }
            }
        } catch (e: Exception) {
            log("Error hiding QS buttons: $e")
        }
    }

    private fun installEmergencyCallHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        log("Installing emergency call hook")
        
        safeHook("KeyguardBottomAreaView.onFinishInflate") {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.phone.KeyguardBottomAreaView",
                lpparam.classLoader,
                "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (getPreferenceBoolean(PREF_DISABLE_EMERGENCY_CALL, false)) {
                            val view = param.thisObject as? ViewGroup ?: return
                            hideEmergencyButton(view)
                        }
                    }
                }
            )
        }
    }

    private fun hideEmergencyButton(viewGroup: ViewGroup) {
        try {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i) ?: continue
                val className = child.javaClass.name.lowercase()
                val contentDesc = child.contentDescription?.toString()?.lowercase() ?: ""
                
                if (className.contains("emergency") || contentDesc.contains("emergency")) {
                    child.visibility = View.GONE
                    log("Hid emergency button: ${child.javaClass.name}")
                }
                
                if (child is ViewGroup) {
                    hideEmergencyButton(child)
                }
            }
        } catch (e: Exception) {
            log("Error hiding emergency button: $e")
        }
    }

    companion object {
        const val PREF_HIDE_PRIVACY_INDICATORS = "pref_hide_privacy_indicators"
        const val PREF_HIDE_QS_BOTTOM_BTN = "pref_hide_qs_bottom_btn"
        const val PREF_DISABLE_EMERGENCY_CALL = "pref_disable_emergency_call"
        const val PREF_POWER_MENU_CUSTOM = "pref_power_menu_custom"
        const val PREF_POWER_MENU_ACTIONS = "pref_power_menu_actions"
        const val PREF_QS_GRID_CUSTOM = "pref_qs_grid_custom"
        const val PREF_QS_COLUMNS = "pref_qs_columns"
        const val PREF_QS_ROWS = "pref_qs_rows"
    }
}