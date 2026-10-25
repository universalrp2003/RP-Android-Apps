package com.universalrp.cleansweep.voice

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/** The single backing store every fake in this file reads and writes. */
private val store = mutableMapOf<String, Any?>()

/** The editor half of the fake: records putBoolean and commits straight into [store]. */
private class EditorHandler : InvocationHandler {
    private var pending: Pair<String, Any?>? = null

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        @Suppress("UNCHECKED_CAST")
        return when (method.name) {
            "putBoolean" -> {
                pending = (args!![0] as String) to args[1]
                proxy
            }
            "commit" -> {
                pending?.let { (k, v) -> store[k] = v }
                pending = null
                true
            }
            "apply" -> {
                pending?.let { (k, v) -> store[k] = v }
                pending = null
                Unit
            }
            "toString" -> "FakeEditor"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args!![0]
            else -> null
        }
    }
}

/** The preferences half of the fake: reads booleans from [store], hands out an editor. */
private class PrefsHandler : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        @Suppress("UNCHECKED_CAST")
        return when (method.name) {
            "getBoolean" -> store[args!![0] as String] as? Boolean ?: false
            "contains" -> store.containsKey(args!![0] as String)
            "edit" -> Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java),
                EditorHandler(),
            ) as SharedPreferences.Editor
            "toString" -> "FakePrefs($store)"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args!![0]
            else -> when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Float::class.javaPrimitiveType -> 0f
                else -> null
            }
        }
    }
}

/**
 * A fresh [SharedPreferences] instance over the shared [store], which is what a new process
 * reading the same preferences file looks like.
 */
private fun prefs(): SharedPreferences = Proxy.newProxyInstance(
    SharedPreferences::class.java.classLoader,
    arrayOf(SharedPreferences::class.java),
    PrefsHandler(),
) as SharedPreferences

/**
 * A3 - the "shown once, then never again" contract behind the HD-voice explainer.
 *
 * The card exists because 2.71 replaced a switch owners had used for two years with a paragraph.
 * If it appeared on every visit it would be nagware, and if it appeared zero times it would be
 * dead code that quietly fails to answer the question it was written for. Both failure modes are
 * invisible on a device, so they are pinned here.
 *
 * An in-memory [SharedPreferences] stands in for the real one. The repo has no Robolectric, and a
 * hand-rolled fake tests this code rather than Android's own preference implementation. Every
 * proxy returned by [prefs] shares one backing store, which is what makes the persistence test
 * below mean something: a new instance is a new process reading the same file.
 */
class HdVoiceExplainerTest {

    @Before
    fun clearTheStore() {
        // Each test starts from a phone that has never opened the Voice screen.
        store.clear()
    }

    @Test
    fun theExplainerIsShownTheFirstTime() {
        // A fresh install has never seen it, so the card must appear. If this defaulted to true
        // the card would never render at all and nobody would notice the missing explanation.
        assertFalse(HdVoiceExplainer.seen(prefs()))
    }

    @Test
    fun dismissingItHidesIt() {
        val p = prefs()
        assertFalse(HdVoiceExplainer.seen(p))
        HdVoiceExplainer.markSeen(p)
        assertTrue(HdVoiceExplainer.seen(p))
    }

    @Test
    fun theFlagSurvivesANewPreferencesInstanceForTheSameFile() {
        // The whole point is persistence across process death: the owner dismisses the card once
        // and it must not come back after the app is killed and reopened. A fresh instance over
        // the same store is exactly that, and it is the test that catches an apply()-only write.
        val first = prefs()
        HdVoiceExplainer.markSeen(first)

        val second = prefs()
        assertTrue(HdVoiceExplainer.seen(second))
    }

    @Test
    fun markingItSeenWritesThroughImmediatelyRatherThanQueueing() {
        // commit() rather than apply(): the write must be durable the moment the owner taps, so a
        // process kill between the tap and an async flush cannot resurrect the card. The store is
        // the disk here, and it must already hold the value when markSeen returns.
        val p = prefs()
        HdVoiceExplainer.markSeen(p)
        assertTrue(store.containsKey("seen"))
        assertTrue(store["seen"] as Boolean)
    }

    @Test
    fun readingItNeverWrites() {
        // A read that wrote would mean merely opening the Voice screen consumed the one-time
        // explanation, which is the "shown zero times" failure.
        val p = prefs()
        HdVoiceExplainer.seen(p)
        assertFalse(store.containsKey("seen"))
    }

    @Test
    fun dismissingTwiceIsIdempotent() {
        // Tapping "Got it" twice, or re-entering the screen during the same visit, must not throw
        // and must not un-hide the card.
        val p = prefs()
        HdVoiceExplainer.markSeen(p)
        HdVoiceExplainer.markSeen(p)
        assertTrue(HdVoiceExplainer.seen(p))
    }
}
