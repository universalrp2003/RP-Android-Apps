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
            assertFalse(sealed.contains("SyntheticFamily"));assertFalse(sealed.contains("f".repeat(64)))
            assertEquals("SyntheticFamily",FamilyStore.read(c).single().getString("name"))
            prefs.edit().putString("sealed",sealed.dropLast(5)+"AAAAA").commit()
            assertTrue(runCatching{FamilyStore.read(c)}.isFailure)
            assertEquals(0,FamilyStore.active(c))
        } finally { c.getSharedPreferences("oor_family_v1",0).edit().clear().commit() }
    }
    @Test fun twoLogicalPhonesRequireBothConfirmAndSeparateConsentAndStopRevokes()=runBlocking {
        check(FamilyRepository.configured){"Native family integration requires verified service configuration"}
        val a=phone("native_a_");val b=phone("native_b_")
        listOf(a,b).forEach{it.getSharedPreferences("oor_family_v1",0).edit().clear().commit()}
        AppPreferences.init(base);val tracking=AppPreferences.state.value.isTrackingEnabled
        try {
            val invitation=FamilyRepository.create(a,"Synthetic Child")
            val id=FamilyProtocol.invite(invitation).id
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
            assertTrue(runCatching{FamilyRepository.start(a,id,1)}.isFailure)
            FamilyRepository.confirm(b,id)
            FamilyRepository.start(a,id,1)
            assertEquals(1,FamilyStore.active(a));assertEquals(0,FamilyStore.active(b))
            val fix=Location("synthetic-family-ci").apply { time=System.currentTimeMillis();elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos();accuracy=100f;latitude=10.0;longitude=78.0 }
            FamilyRepository.publish(a,"Synthetic Town","Synthetic District",fix)
            assertEquals("Synthetic Town",FamilyRepository.views(b).single().town!!.town)
            assertNull(FamilyRepository.views(a).single().town)
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
