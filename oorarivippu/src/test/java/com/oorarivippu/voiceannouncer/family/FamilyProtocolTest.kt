package com.oorarivippu.voiceannouncer.family
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class FamilyProtocolTest {
    private val id="a".repeat(32);private val key="b".repeat(64);private val now=1800000000000L
    private fun sealed()=FamilyProtocol.encrypt(id,"a",key,FamilyProtocol.Town("Synthetic Town","Synthetic District",now),1).put("expires",now+FamilyProtocol.TTL)
    @Test fun invitationRoundTrip() { val i=FamilyProtocol.Invite(id,"c".repeat(64),key);assertEquals(i,FamilyProtocol.invite(i.text())) }
    @Test fun invalidInvitationsFailClosed() { for(s in listOf("https://other.test", "oorfamily:v2:$id:${"c".repeat(64)}:$key","x".repeat(201))) assertTrue(runCatching{FamilyProtocol.invite(s)}.isFailure) }
    @Test fun knownBackendAuthHash() { assertEquals("a6028abe8ffe24130ea1d20ba509e78308a800567a7118d3da7ca2a612ec50a4",FamilyProtocol.authHash("a".repeat(64))) }
    @Test fun encryptDecryptTownOnly() { val row=sealed();val plain=FamilyProtocol.decrypt(id,"a",key,row,now,now);assertEquals("Synthetic Town",plain.town);assertEquals(now,plain.at);assertFalse(row.toString().contains("Synthetic"));assertEquals(setOf("sampleAt","generation","iv","ciphertext","expires"),row.keys().asSequence().toSet()) }
    @Test fun randomNoncePerEnvelope() { assertNotEquals(sealed().getString("iv"),sealed().getString("iv")) }
    @Test fun alteredCiphertextRejected() { val row=sealed();row.put("ciphertext","A".repeat(row.getString("ciphertext").length));assertTrue(runCatching{FamilyProtocol.decrypt(id,"a",key,row,now,now)}.isFailure) }
    @Test fun wrongKeyRejected() { assertTrue(runCatching{FamilyProtocol.decrypt(id,"a","c".repeat(64),sealed(),now,now)}.isFailure) }
    @Test fun roleReflectionRejected() { assertTrue(runCatching{FamilyProtocol.decrypt(id,"b",key,sealed(),now,now)}.isFailure) }
    @Test fun crossPairRejected() { assertTrue(runCatching{FamilyProtocol.decrypt("e".repeat(32),"a",key,sealed(),now,now)}.isFailure) }
    @Test fun alteredTimeRejected() { val row=sealed().put("sampleAt",now+1);assertTrue(runCatching{FamilyProtocol.decrypt(id,"a",key,row,now+1,now+1)}.isFailure) }
    @Test fun staleAndFutureHidden() { val row=sealed();for(t in listOf(now-1,now+FamilyProtocol.TTL))assertTrue(runCatching{FamilyProtocol.decrypt(id,"a",key,row,t,t)}.isFailure) }
    @Test fun serverCannotExtendRetention() { val row=sealed().put("expires",now+FamilyProtocol.TTL+1);assertTrue(runCatching{FamilyProtocol.decrypt(id,"a",key,row,now,now)}.isFailure) }
    @Test fun noNumericAddressesPinsOrCoordinatesInLabels() { for(s in listOf("10.1,78.2","Town 622301","12 Main Street","","x".repeat(81)))assertTrue(runCatching{FamilyProtocol.clean(s)}.isFailure);assertEquals("ஆலங்குடி",FamilyProtocol.clean("ஆலங்குடி")) }
    @Test fun fixRequiresWallMonotonicFreshnessAndAccuracy() { assertTrue(FamilyProtocol.freshFix(1000,1000,500f));assertFalse(FamilyProtocol.freshFix(1000,130000,500f));assertFalse(FamilyProtocol.freshFix(-1,1000,500f));assertFalse(FamilyProtocol.freshFix(1000,1000,Float.NaN));assertFalse(FamilyProtocol.freshFix(1000,1000,5001f)) }
    @Test fun confirmationBindsInviteAndGuestNonce() { assertEquals(FamilyProtocol.verification(id,key,"d".repeat(32)),FamilyProtocol.verification(id,key,"d".repeat(32)));assertNotEquals(FamilyProtocol.verification(id,key,"d".repeat(32)),FamilyProtocol.verification(id,key,"e".repeat(32))) }
    @Test fun aStopRevisionCannotBeOverwrittenByStartReply() { assertTrue(FamilyProtocol.canAcceptStart(0,0,false,true));assertFalse(FamilyProtocol.canAcceptStart(0,1,false,true));assertFalse(FamilyProtocol.canAcceptStart(1,2,true,true)) }
    @Test fun startingNeedsTrackingAndNoPendingStop() { assertFalse(FamilyProtocol.canAcceptStart(0,0,true,true));assertFalse(FamilyProtocol.canAcceptStart(0,0,false,false)) }
}
