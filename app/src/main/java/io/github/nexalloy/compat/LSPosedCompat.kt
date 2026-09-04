package io.github.nexalloy.compat

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.libxposed.api.XposedInterface
import io.github.nexalloy.IHookCallback
import java.lang.reflect.Executable
import java.lang.reflect.Member

object LSPosedCompat {
    @Volatile
    private var xposedInterface: XposedInterface? = null

    private val returnEarlyField by lazy {
        runCatching {
            XC_MethodHook.MethodHookParam::class.java.getDeclaredField("returnEarly").apply {
                isAccessible = true
            }
        }.getOrNull()
    }

    private fun createParam(): XC_MethodHook.MethodHookParam {
        return XC_MethodHook.MethodHookParam::class.java.getDeclaredConstructor().apply {
            isAccessible = true
        }.newInstance()
    }

    private fun isReturnEarly(param: XC_MethodHook.MethodHookParam): Boolean {
        return returnEarlyField?.getBoolean(param) ?: (param.result != null || param.hasThrowable())
    }

    fun init(xposed: XposedInterface) {
        xposedInterface = xposed
        Logger.printInfo { "LSPosedCompat initialized with API ${xposed.apiVersion} (${xposed.frameworkName} ${xposed.frameworkVersion})" }
    }

    fun isApi102Available(): Boolean {
        val xposed = xposedInterface ?: return false
        return runCatching { xposed.apiVersion >= XposedInterface.API_101 }.getOrDefault(false)
    }

    fun hookMember(
        member: Member,
        before: IHookCallback? = null,
        after: IHookCallback? = null,
        priority: Int = XposedInterface.PRIORITY_DEFAULT
    ) {
        val xposed = xposedInterface
        if (xposed != null && isApi102Available() && member is Executable) {
            try {
                xposed.hook(member)
                    .setPriority(priority)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        val param = createParam()
                        param.method = chain.executable
                        param.thisObject = chain.thisObject
                        param.args = chain.args.toTypedArray()

                        if (before != null) {
                            try {
                                before(param)
                            } catch (t: Throwable) {
                                Logger.printException({ "Error in beforeHookedMethod for ${member.name}" }, t)
                            }
                        }

                        if (param.hasThrowable()) {
                            throw param.throwable
                        }

                        if (!isReturnEarly(param)) {
                            val result = chain.proceed(param.args)
                            param.result = result
                        }

                        if (after != null) {
                            try {
                                after(param)
                            } catch (t: Throwable) {
                                Logger.printException({ "Error in afterHookedMethod for ${member.name}" }, t)
                            }
                        }

                        if (param.hasThrowable()) {
                            throw param.throwable
                        }

                        param.result
                    }
                return
            } catch (e: Throwable) {
                Logger.printException({ "Failed to hook via LibXposed API 102, falling back to legacy XposedBridge for ${member.name}" }, e)
            }
        }

        // Fallback to legacy XposedBridge
        XposedBridge.hookMethod(member, object : XC_MethodHook(priority) {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (before != null) {
                    try {
                        before(param)
                    } catch (t: Throwable) {
                        Logger.printException({ "Error in legacy beforeHookedMethod for ${member.name}" }, t)
                    }
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                if (after != null) {
                    try {
                        after(param)
                    } catch (t: Throwable) {
                        Logger.printException({ "Error in legacy afterHookedMethod for ${member.name}" }, t)
                    }
                }
            }
        })
    }
}
