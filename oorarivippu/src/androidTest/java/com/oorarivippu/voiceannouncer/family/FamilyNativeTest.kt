package com.oorarivippu.voiceannouncer.family

import android.content.Context
import android.content.ContextWrapper
import android.location.Location
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oorarivippu.voiceannouncer.AppPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic logical phones with isolated prefs; real Android Keystore/JCA and deployed HTTPS. Not GPS/OEM proof. */
@RunWith(AndroidJUnit4::class)
class FamilyNativeTest {
    private val base get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun phone(prefix: String)=object:ContextWrapper(base){
        override fun getApplicationContext():Context=this
        override fun getSharedPreferences(name:String,mode:Int)=super.getSharedPreferences(prefix+name,mode)
    }
    @Test fun nativeKeystoreStoresNoPlaintextAndDetectsTampering() {
        val c=phone("native_local_")
        c.getSharedPreferences("oor_family_v1",0).edit().clear().commit()
        try {
            FamilyStore.change(c){it.add(org.json.JSONObject().put("name","SyntheticFamily").put("key","f".repeat(64)))}
            val prefs=c.getSharedPreferences("oor_family_v1",0);val sealed=prefs.getString("sealed",null)!!
            FamilyStore.change(c){}
            assertEquals(sealed,prefs.getString("sealed",null))
            assertFalse(sealed.contains("SyntheticFamily"));assertFalse(sealed.contains("f".repeat(64)))
            assertEquals("SyntheticFamily",FamilyStore.read(c).single().getString("name"))
            prefs.edit().putString("sealed",sealed.dropLast(5)+"AAAAA").commit()
            assertTrue(runCatching{FamilyStore.read(c)}.isFailure)
            assertEquals(0,FamilyStore.active(c))
        } finally { c.getSharedPreferences("oor_family_v1",0).edit().clear().commit() }
    }
    @Test fun twoLogicalPhonesRequireBothConfirmAndSeparateConsentAndStopRevokes()=runBlocking {
        check(FamilyRepository.configured){"Native family integration requires verified service configuration"}
        val a=base;val b=phone("native_b_")
        listOf(a,b).forEach{it.getSharedPreferences("oor_family_v1",0).edit().clear().commit()}
        AppPreferences.init(base);val tracking=AppPreferences.state.value.isTrackingEnabled
        try {
            val invitation=FamilyRepository.create(a,"Synthetic Child")
            val id=FamilyProtocol.invite(invitation).id
            // A timed 1.1.14 grant must not silently migrate into persistent consent.
            FamilyStore.change(a){it.single().remove("enabled");it.single().put("until",System.currentTimeMillis()+3600000)}
            FamilyRepository.views(a)
            assertFalse(FamilyStore.read(a).single().optBoolean("enabled"));assertEquals(0,FamilyStore.active(a))
            FamilyRepository.join(b,"Synthetic Parent",invitation)
            assertEquals(0,FamilyStore.active(a));assertEquals(0,FamilyStore.active(b))
            // Simulate lost setup acknowledgements: same identities must retry idempotently.
            FamilyStore.change(a){it.single().put("pending","create")}
            FamilyStore.change(b){it.single().put("pending","claim").put("invite",FamilyProtocol.invite(invitation).token)}
            val av=FamilyRepository.views(a).single();val bv=FamilyRepository.views(b).single()
            assertFalse(av.setupPending);assertFalse(bv.setupPending)
            assertEquals(av.code,bv.code);assertFalse(av.confirmed);assertNull(av.town)
            FamilyRepository.confirm(a,id)
            AppPreferences.setTrackingEnabled(true)
            assertTrue(runCatching{FamilyRepository.setSharing(a,id,true)}.isFailure)
            FamilyRepository.confirm(b,id)
            FamilyRepository.views(a)
            AppPreferences.setTrackingEnabled(false)
            FamilyRepository.setSharing(a,id,true)
            assertTrue(FamilyRepository.cached(a).single().sharing);assertEquals(0,FamilyStore.active(a))
            AppPreferences.setTrackingEnabled(true)
            FamilyRepository.views(a)
            assertEquals(1,FamilyStore.active(a));assertEquals(0,FamilyStore.active(b))
            val fix=Location("synthetic-family-ci").apply { time=System.currentTimeMillis();elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos();accuracy=100f;latitude=10.0;longitude=78.0 }
            FamilyRepository.publish(a,"Synthetic Town","Synthetic District",fix)
            val received=FamilyRepository.views(b).single().town!!
            assertEquals("Synthetic Town",received.town)
            assertTrue(FamilyRepository.claimSpeech(b,id,received.at));assertFalse(FamilyRepository.claimSpeech(b,id,received.at))
            FamilyRepository.views(b);assertFalse(FamilyRepository.claimSpeech(b,id,received.at))
            // Trip OFF pauses uploads but retains explicit ON for the next trip.
            AppPreferences.setTrackingEnabled(false);FamilyRepository.pauseTrip(a)
            assertEquals(0,FamilyStore.active(a));assertTrue(FamilyRepository.cached(a).single().sharing)
            FamilyRepository.views(a)
            AppPreferences.setTrackingEnabled(true);FamilyRepository.views(a)
            assertEquals(1,FamilyStore.active(a));assertTrue(FamilyRepository.cached(a).single().sharing)
            FamilyRepository.setSharing(b,id,true)
            val otherFix=Location(fix).apply{time=System.currentTimeMillis();elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos()}
            FamilyRepository.publish(b,"Other Synthetic Town","Synthetic District",otherFix)
            FamilyRepository.views(a)
            AppPreferences.setTrackingEnabled(false);FamilyRepository.pauseTrip(a)
            AppPreferences.setAppLanguage("en")
            val activity=InstrumentationRegistry.getInstrumentation().startActivitySync(android.content.Intent(base,com.oorarivippu.voiceannouncer.MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            try {
                fun nodes():List<android.view.accessibility.AccessibilityNodeInfo> {
                    val all=mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
                    fun visit(n:android.view.accessibility.AccessibilityNodeInfo?) {if(n==null)return;all.add(n);for(i in 0 until n.childCount)visit(n.getChild(i))}
                    visit(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow);return all
                }
                var tree=nodes()
                repeat(30){if(tree.none{it.text?.toString()=="Other Synthetic Town · Synthetic District"}){SystemClock.sleep(200);tree=nodes()}}
                assertTrue("Actual main window shows latest verified member",tree.any{it.text?.toString()=="Synthetic Child"})
                assertTrue("Actual main window shows received town",tree.any{it.text?.toString()=="Other Synthetic Town · Synthetic District"})
                val toggle=tree.firstOrNull{it.isCheckable && it.isChecked}
                assertNotNull("Main sharing switch is ON while uploads are paused",toggle)
                assertTrue(toggle!!.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
                repeat(30){if(FamilyRepository.cached(a).single().sharing)SystemClock.sleep(100)}
                assertFalse(FamilyRepository.cached(a).single().sharing)
            } finally {InstrumentationRegistry.getInstrumentation().runOnMainSync{activity.finish()}}
            AppPreferences.setTrackingEnabled(true)
            FamilyRepository.views(a);assertEquals(0,FamilyStore.active(a))
            assertEquals("Other Synthetic Town",FamilyRepository.views(a).single().town?.town)
            FamilyRepository.stop(a,id)
            assertEquals(0,FamilyStore.active(a))
            FamilyRepository.views(a) // Flushes durable stop even if asynchronous request has not run yet.
            assertNull(FamilyRepository.views(b).single().town)
            FamilyRepository.stop(b,id,remove=true)
            assertTrue(FamilyRepository.views(b).isEmpty())
            assertTrue(FamilyRepository.views(a).single().unavailable)
        } finally {
            runCatching{FamilyRepository.stopAll(a);FamilyRepository.stopAll(b)}
            AppPreferences.setTrackingEnabled(tracking)
            listOf(a,b).forEach{it.getSharedPreferences("oor_family_v1",0).edit().clear().commit()}
        }
    }
}
