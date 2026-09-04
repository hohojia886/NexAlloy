package io.github.nexalloy.cache

import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object ReflectionCache {
    private val fieldCache = ConcurrentHashMap<String, Field>()
    private val methodCache = ConcurrentHashMap<String, Method>()
    private val classCache = ConcurrentHashMap<String, Class<*>>()

    fun findFirstFieldByExactType(clazz: Class<*>, type: Class<*>): Field {
        val cacheKey = "${clazz.name}#fieldByExactType#${type.name}"
        return fieldCache.computeIfAbsent(cacheKey) {
            XposedHelpers.findFirstFieldByExactType(clazz, type)
        }
    }

    fun findFirstFieldByExactTypeOrNull(clazz: Class<*>, type: Class<*>?): Field? {
        if (type == null) return null
        val cacheKey = "${clazz.name}#fieldByExactTypeOrNull#${type.name}"
        return fieldCache.computeIfAbsent(cacheKey) {
            runCatching { XposedHelpers.findFirstFieldByExactType(clazz, type) }.getOrNull() ?: NO_FIELD
        }.takeIf { it !== NO_FIELD }
    }

    fun findMethodExact(clazz: Class<*>, methodName: String, vararg parameterTypes: Any?): Method {
        val paramKey = parameterTypes.joinToString(",") { it.toString() }
        val cacheKey = "${clazz.name}#$methodName#$paramKey"
        return methodCache.computeIfAbsent(cacheKey) {
            XposedHelpers.findMethodExact(clazz, methodName, *parameterTypes)
        }
    }

    fun findClass(className: String, classLoader: ClassLoader?): Class<*> {
        val cacheKey = "${className}@${classLoader?.hashCode() ?: 0}"
        return classCache.computeIfAbsent(cacheKey) {
            XposedHelpers.findClass(className, classLoader)
        }
    }

    fun clear() {
        fieldCache.clear()
        methodCache.clear()
        classCache.clear()
    }

    private val NO_FIELD: Field by lazy {
        Dummy::class.java.getDeclaredField("dummy").apply { isAccessible = true }
    }

    private class Dummy {
        @Suppress("unused")
        private val dummy: Any? = null
    }
}
