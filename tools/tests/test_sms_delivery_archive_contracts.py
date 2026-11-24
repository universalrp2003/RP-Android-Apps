"""SMS Guard 1.1.18: delivery reports and per-message archive.

The owner asked for an SMS delivery report and an archive option. Both are places where a wrong
implementation would lie to the user: a message shown as "Delivered" because the provider default
looked like a delivery report, or an "archive" that deleted the message. These contracts pin the
wiring so the gates catch a regression before it reaches a phone:

* every sent SMS part carries both a sent and a delivery PendingIntent, and nothing marks a row
  sent or delivered at send time;
* the callback receiver is reachable only through those explicit PendingIntents;
* the provider row's STATUS/ERROR_CODE columns are what the inbox reads, and a tracked row is never
  reported as delivered without a delivery report (the policy rule is covered by JVM tests);
* archive is a set of message ids in app-private storage; the toggle never touches the provider.
"""
from pathlib import Path
import re
import unittest
ROOT = Path(__file__).resolve().parents[2]
SMS = ROOT / "smsguard/src/main/java/com/universalrp/smsguard"


def read(rel: str) -> str:
    return (SMS / rel).read_text(encoding="utf-8")


class SmsDeliveryContracts(unittest.TestCase):
    def test_every_sent_part_carries_sent_and_delivery_callbacks(self):
        repo = read("data/SmsRepository.kt")
        send = repo.split("fun sendSmsMessage(", 1)[1].split("private fun insertOutgoingRow(", 1)[0]
        self.assertNotIn("sendTextMessage(cleanDest, null, cleanBody, null, null)", send)
        self.assertNotIn("sendMultipartTextMessage(cleanDest, null, parts, null, null)", send)
        self.assertIn("SmsDeliveryPendingIntents.ACTION_SENT", send)
        self.assertIn("SmsDeliveryPendingIntents.ACTION_DELIVERED", send)
        self.assertIn("smsManager.sendMultipartTextMessage(cleanDest, null, parts, sentIntents, deliveryIntents)", send)
        self.assertIn("smsManager.sendTextMessage(cleanDest, null, cleanBody, sentIntents[0], deliveryIntents[0])", send)
        # The row is an outbox entry first; only the radio's answer moves it on.
        self.assertIn("insertOutgoingRow(context, cleanDest, cleanBody, sentNowMs, subscriptionId)", send)
        self.assertIn("SmsDeliveryTracker.start(context, messageId, partCount, sentNowMs)", send)
        self.assertNotIn("Telephony.Sms.Sent.CONTENT_URI, values", send)

    def test_a_refused_send_never_leaves_the_row_sending(self):
        repo = read("data/SmsRepository.kt")
        send = repo.split("fun sendSmsMessage(", 1)[1].split("private fun insertOutgoingRow(", 1)[0]
        catch = send.split("} catch (failure: Throwable) {", 1)[1]
        self.assertIn("SmsDeliveryStatusPolicy.TYPE_FAILED", catch)
        self.assertIn("SmsDeliveryStatusPolicy.STATUS_FAILED", catch)
        self.assertIn("SmsDeliveryTracker.clear(context, messageId)", catch)

    def test_outbox_row_starts_pending_and_callbacks_are_the_only_writer_of_sent_or_delivered(self):
        repo = read("data/SmsRepository.kt")
        insert = repo.split("private fun insertOutgoingRow(", 1)[1].split("private fun updateDeliveryRow(", 1)[0]
        self.assertIn("put(Telephony.Sms.TYPE, SmsDeliveryStatusPolicy.TYPE_OUTBOX)", insert)
        self.assertIn("put(Telephony.Sms.STATUS, SmsDeliveryStatusPolicy.STATUS_PENDING)", insert)
        self.assertIn("put(Telephony.Sms.ERROR_CODE, 0)", insert)
        self.assertIn("Telephony.Sms.Outbox.CONTENT_URI", insert)
        callback = repo.split("fun onDeliveryBroadcast(", 1)[1].split("fun markThreadRead(", 1)[0]
        self.assertIn("SmsDeliveryTracker.apply(context, messageId, partIndex, event, System.currentTimeMillis())", callback)
        self.assertIn("SmsDeliveryStatusPolicy.STATUS_COMPLETE", callback)
        self.assertIn("SmsDeliveryStatusPolicy.TYPE_FAILED, SmsDeliveryStatusPolicy.STATUS_FAILED, reason", callback)
        # Delivered only on the DELIVERED outcome, which the tracker yields once every part agreed.
        self.assertIn("SmsPartOutcome.DELIVERED -> updateDeliveryRow(context, messageId, SmsDeliveryStatusPolicy.TYPE_SENT, SmsDeliveryStatusPolicy.STATUS_COMPLETE, 0)", callback)

    def test_pending_intents_target_this_app_explicitly_and_are_mutable_only_where_android_requires(self):
        source = read("data/SmsDeliveryPendingIntents.kt")
        self.assertIn("Intent(context, SmsStatusReceiver::class.java)", source)
        self.assertIn("if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0", source)
        self.assertIn("SmsDeliveryTracker.nextRequestCode(context)", source)
        self.assertIn("PendingIntent.FLAG_UPDATE_CURRENT or mutable", source)
        self.assertIn("if (action != ACTION_SENT && action != ACTION_DELIVERED) return null", source)

    def test_status_receiver_is_private_and_has_no_intent_filter(self):
        manifest = (ROOT / "smsguard/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        block = re.search(r'<receiver\s+android:name="\.service\.SmsStatusReceiver"[^>]*/>', manifest)
        self.assertIsNotNone(block, "SmsStatusReceiver must be registered")
        self.assertIn('android:exported="false"', block.group(0))
        receiver = read("service/SmsStatusReceiver.kt")
        self.assertIn("SmsDeliveryPendingIntents.read(intent) ?: return", receiver)
        self.assertIn("val ok = resultCode == Activity.RESULT_OK", receiver)
        self.assertIn("SmsRepository.onDeliveryBroadcast(context, ref.messageId, ref.partIndex, event, code)", receiver)
        self.assertIn("SmsEvents.notifyChanged()", receiver)
        self.assertNotIn("<intent-filter>", block.group(0))

    def test_inbox_reads_the_delivery_columns_and_keeps_legacy_rows_plain(self):
        repo = read("data/SmsRepository.kt")
        projection = repo.split("val projection = arrayOf(", 2)[2].split(")", 1)[0]
        self.assertIn("Telephony.Sms.STATUS", projection)
        self.assertIn("Telephony.Sms.ERROR_CODE", projection)
        self.assertIn("status = row.status,", repo)
        self.assertIn("errorCode = row.errorCode,", repo)
        self.assertIn("smsType = row.type,", repo)
        settings = read("data/GuardSettingsStore.kt")
        self.assertIn("fun markDeliveryTrackingSince(context: Context, nowMs: Long)", settings)
        self.assertIn("val deliveryTrackingSinceMs: Long = Long.MAX_VALUE", settings)
        bubble = read("ui/MessagesTab.kt")
        self.assertIn("trackingSinceMs = trackingSinceMs,", bubble)
        self.assertIn("SmsDeliveryStatusPolicy.resolve(", bubble)


class MessageArchiveContracts(unittest.TestCase):
    def test_archive_is_app_private_and_never_deletes_the_provider_row(self):
        settings = read("data/GuardSettingsStore.kt")
        toggle = settings.split("fun toggleArchivedMessage(", 1)[1].split("fun removeArchivedMessage(", 1)[0]
        self.assertIn('putStringSet(KEY_ARCHIVED_MESSAGES', toggle)
        self.assertNotIn("contentResolver", toggle)
        self.assertNotIn(".delete(", toggle)
        self.assertIn("const val KEY_ARCHIVED_MESSAGES = \"archived_message_ids_v1\"", settings)
        repo = read("data/SmsRepository.kt")
        self.assertNotIn("archivedMessageIds", repo.split("fun deleteMessageById(", 1)[1].split("fun markThreadRead(", 1)[0])

    def test_lists_are_partitioned_and_the_archived_chip_reads_its_own_list(self):
        vm = read("ui/MainViewModel.kt")
        self.assertIn("ARCHIVED }", vm.split("enum class InboxFilter", 1)[1].split("\n", 1)[0])
        publish = vm.split("private suspend fun publishThreads(", 1)[1].split("fun toggleArchive(", 1)[0]
        self.assertIn("val visible = messages.filterNot { it.id in archivedIds }", publish)
        self.assertIn("val archived = messages.filter { it.id in archivedIds }", publish)
        self.assertIn("SmsRepository.groupThreads(archived)", publish)
        self.assertIn("withContext(Dispatchers.Default)", publish)
        self.assertIn("GuardSettingsStore.toggleArchivedMessage(ctx, message.id)", vm)
        self.assertIn("GuardSettingsStore.removeArchivedMessage(ctx, message.id)", vm)
        tab = read("ui/MessagesTab.kt")
        self.assertIn("if (filter == InboxFilter.ARCHIVED) archivedThreads else threads", tab)
        self.assertIn("InboxFilter.ARCHIVED to ui.filterArchived,", tab)
        self.assertIn("onToggleArchive = { vm.toggleArchive(msg) }", tab)
        self.assertIn("initialShowArchived = filter == InboxFilter.ARCHIVED", (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/MainActivity.kt").read_text())

    def test_every_new_string_exists_in_both_languages(self):
        catalog = read("ui/BilingualCatalog.kt")
        for name in ("statusSending", "statusSent", "statusDelivered", "statusNotDelivered", "statusNotSent",
                     "reasonRadioOff", "reasonNoService", "reasonLimitExceeded", "reasonFdnBlocked",
                     "reasonEncoding", "reasonGeneric", "archiveMsgBtn", "unarchiveMsgBtn", "filterArchived",
                     "archivedMessagesLabel", "showArchivedBtn", "hideArchivedBtn", "archivedEmptyTitle",
                     "archivedEmptyBody", "allArchivedTitle"):
            # One declaration in the data class, one assignment in the Tamil block, one in English.
            self.assertEqual(1, len(re.findall(rf"^    val {name}: String,$", catalog, re.M)), name)
            self.assertEqual(2, len(re.findall(rf"^        {name} = \"[^\"]+\",$", catalog, re.M)), name)


if __name__ == "__main__":
    unittest.main()
