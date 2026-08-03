package com.riloc.app.xposed.util

import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Chain
import java.lang.reflect.Field
import java.lang.reflect.Method

/** Small reflection + hooking helpers used by the hook installers. */
object HookUtil {

    fun findClass(classLoader: ClassLoader, vararg names: String): Class<*>? {
        for (name in names) {
            try {
                return Class.forName(name, false, classLoader)
            } catch (_: Throwable) {
                // try next candidate (AOSP moved classes across releases)
            }
        }
        return null
    }

    fun findField(clazz: Class<*>, fieldName: String): Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }

    fun findMethod(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                current = current.superclass
            }
        }
        return clazz.methods.firstOrNull {
            it.name == methodName && it.parameterTypes.contentEquals(parameterTypes)
        }?.apply { isAccessible = true }
    }

    /** Hooks every declared overload with the given name. */
    fun hookAll(
        module: XposedInterface,
        clazz: Class<*>,
        methodName: String,
        tag: String,
        hooker: (Chain) -> Any?,
    ): Int {
        var hooked = 0
        clazz.declaredMethods.filter { it.name == methodName }.forEach { method ->
            try {
                module.hook(method).intercept { chain -> hooker(chain) }
                hooked++
            } catch (e: Throwable) {
                module.log(Log.WARN, tag, "Failed hooking ${clazz.name}#$methodName: ${e.message}")
            }
        }
        return hooked
    }

    /** Hooks a single method by exact name + parameter types. */
    fun hookMethod(
        module: XposedInterface,
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>,
        tag: String,
        hooker: (Chain) -> Any?,
    ): Boolean {
        val method = findMethod(clazz, methodName, *parameterTypes) ?: return false
        return try {
            module.hook(method).intercept { chain -> hooker(chain) }
            true
        } catch (e: Throwable) {
            module.log(Log.WARN, tag, "Failed hooking ${clazz.name}#$methodName: ${e.message}")
            false
        }
    }

    fun defaultReturnValue(method: Method?): Any? = when (method?.returnType) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0F
        java.lang.Double.TYPE -> 0.0
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Character.TYPE -> '\u0000'
        else -> null
    }

    fun looksLikePackageName(value: String?): Boolean =
        value != null && "." in value && !value.startsWith("android.") && !value.startsWith("java.")

    /**
     * Best-effort package-name attribution for binder call arguments.
     * Mirrors the strategy used by XposedFakeLocation's system hooks.
     */
    fun collectPackageNames(value: Any?): Set<String> {
        val result = linkedSetOf<String>()
        val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Any, Boolean>())
        collectPackageNames(value, result, visited, 0)
        return result
    }

    private fun collectPackageNames(
        value: Any?,
        out: MutableSet<String>,
        visited: MutableSet<Any>,
        depth: Int,
    ) {
        if (value == null || depth > 4) return
        if (!visited.add(value)) return

        if (value is String) {
            value.takeIf(::looksLikePackageName)?.let(out::add)
            return
        }
        if (value is Iterable<*>) {
            value.forEach { collectPackageNames(it, out, visited, depth + 1) }
            return
        }
        if (value is Map<*, *>) {
            value.forEach { (k, v) ->
                collectPackageNames(k, out, visited, depth + 1)
                collectPackageNames(v, out, visited, depth + 1)
            }
            return
        }

        listOf(
            "mPackageName", "packageName", "callingPackage", "mCallingPackage",
            "mCallerPackageName", "callerPackageName", "mOpPackageName", "opPackageName",
        ).forEach { fieldName ->
            (findField(value.javaClass, fieldName)?.get(value) as? String)
                ?.takeIf(::looksLikePackageName)?.let(out::add)
        }
        listOf(
            "getPackageName", "getCallingPackage", "getCallerPackageName", "getOpPackageName",
        ).forEach { methodName ->
            runCatching { findMethod(value.javaClass, methodName)?.invoke(value) as? String }
                .getOrNull()?.takeIf(::looksLikePackageName)?.let(out::add)
        }
        listOf(
            "mIdentity", "mCallerIdentity", "callerIdentity", "mCallingIdentity", "callingIdentity",
            "mAttributionSource", "attributionSource", "mNext", "next",
            "mWorkSource", "workSource", "mRequest", "request",
            "mLocationRequest", "locationRequest", "mCallerPackage",
        ).forEach { fieldName ->
            collectPackageNames(findField(value.javaClass, fieldName)?.get(value), out, visited, depth + 1)
        }
        listOf(
            "getAttributionSource", "getNext", "getWorkSource", "getLocationRequest", "getCallerPackageName",
        ).forEach { methodName ->
            runCatching { findMethod(value.javaClass, methodName)?.invoke(value) }
                .getOrNull()?.let { collectPackageNames(it, out, visited, depth + 1) }
        }
    }
}
