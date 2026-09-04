package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class AuditLogStateTest {
    private class Session {
        var profiles=ProfileFixtures.showcaseProfiles.take(2).map { it.copy(developerTools=it.developerTools.copy(isEnabled=true)) }
        var active=profiles.first().id
        var locked=false
        var signed=profiles.mapTo(mutableSetOf()) { it.id }
        val c=AuditLogController({profiles},{active},{it in signed},{locked})
        fun step() { c.work!!.let { c.advance(it.id,it.attempt) } }
        fun start(action: AuditLogAction): Long { val id=c.begin(action)!!; if(c.work!!.phase==AuditLogPhase.Consent) assertTrue(c.confirm(id)); step(); return id }
    }
    @Test fun enablingRequiresConsentAndIsSeparateFromSanitizedDiagnostics() {
        val s=Session(); val before=s.profiles.map { it.diagnostics }; val id=s.c.begin(AuditLogAction.Enable)!!
        assertEquals(AuditLogPhase.Consent,s.c.work!!.phase); assertFalse(s.c.state.enabled); assertTrue(s.c.state.files.isEmpty())
        s.c.cancel(id); assertFalse(s.c.state.enabled); s.start(AuditLogAction.Enable)
        assertTrue(s.c.state.enabled); assertEquals(2,s.c.state.files.size); assertEquals(before,s.profiles.map { it.diagnostics })
    }
    @Test fun stoppingRetainsFilesAndRecordingIsAppWide() {
        val s=Session(); s.start(AuditLogAction.Enable); val files=s.c.state.files
        s.active=s.profiles.last().id; s.c.reconcile(); assertTrue(s.c.state.enabled); assertNull(s.c.work)
        s.start(AuditLogAction.Disable); assertEquals(files,s.c.state.files); assertFalse(s.c.state.enabled)
    }
    @Test fun exportConsentCancellationDoesNotPrepareAnything() {
        val s=Session(); s.start(AuditLogAction.Enable); val id=s.c.begin(AuditLogAction.Export)!!
        assertNull(s.c.work!!.archive); s.c.cancel(id); assertNull(s.c.takeForWriting(id,0))
    }
    @Test fun exportIncludesEachSensitiveFileAndCanBeConsumedOnce() {
        val s=Session(); s.start(AuditLogAction.Enable); val files=s.c.state.files; val id=s.start(AuditLogAction.Export)
        val bytes=s.c.takeForWriting(id,0)!!; assertNull(s.c.takeForWriting(id,0))
        val actual=mutableMapOf<String,String>(); ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry=zip.nextEntry
            while(entry!=null) { actual[entry.name]=zip.readBytes().toString(Charsets.UTF_8); entry=zip.nextEntry }
        }
        assertEquals(files.associate { it.name to it.content },actual); assertTrue(actual.values.any { "profile_public_key" in it })
        assertTrue(s.c.written(id,0,true)); assertFalse(s.c.written(id,0,true)); assertNull(s.c.work!!.archive)
    }
    @Test fun writeFailureRetriesPreparationWithoutChangingRecordingOrFiles() {
        val s=Session(); s.start(AuditLogAction.Enable); val before=s.c.state
        s.c.choose(AuditLogScenario.WriteFails); val id=s.start(AuditLogAction.Export); s.c.takeForWriting(id,0); s.c.written(id,0,false)
        assertEquals(AuditLogFailure.Write,s.c.work!!.failure); assertTrue(s.c.retry(id)); s.step(); assertNotNull(s.c.takeForWriting(id,1)); assertEquals(before,s.c.state)
    }
    @Test fun partialDeleteRetriesOnlyRemainingFilesAndKeepsRecordingOn() {
        val s=Session(); s.start(AuditLogAction.Enable); val old=s.c.state.files.map { it.id }.toSet()
        s.c.choose(AuditLogScenario.PartialDelete); val id=s.start(AuditLogAction.Delete)
        assertEquals(AuditLogFailure.PartialDelete,s.c.work!!.failure); assertEquals(1,s.c.work!!.removed.size); assertTrue(s.c.state.enabled)
        assertTrue(s.c.retry(id)); s.step(); assertEquals(old,s.c.work!!.removed); assertTrue(s.c.state.enabled)
        assertTrue(s.c.state.files.none { it.id in old }); assertEquals(2,s.c.state.files.size)
    }
    @Test fun deletingWhenRecordingIsOffLeavesEmptyInventory() {
        val s=Session(); s.start(AuditLogAction.Enable); s.start(AuditLogAction.Disable); s.start(AuditLogAction.Delete)
        assertFalse(s.c.state.enabled); assertTrue(s.c.state.files.isEmpty())
    }
    @Test fun emptyExportAndDeleteHaveHonestEmptyOutcomes() {
        val s=Session(); s.start(AuditLogAction.Export); assertEquals(AuditLogFailure.Empty,s.c.work!!.failure)
        assertFalse(s.c.retry(s.c.work!!.id)); s.start(AuditLogAction.Delete); assertEquals(AuditLogFailure.Empty,s.c.work!!.failure)
    }
    @Test fun failedRecordingChangeLeavesPreviousValue() {
        val s=Session(); s.c.choose(AuditLogScenario.UpdateFails); val id=s.start(AuditLogAction.Enable)
        assertFalse(s.c.state.enabled); assertEquals(AuditLogFailure.Update,s.c.work!!.failure); assertTrue(s.c.retry(id)); s.step(); assertTrue(s.c.state.enabled)
    }
    @Test fun lockAndProfileChangesInvalidatePreparedExports() {
        val s=Session(); s.start(AuditLogAction.Enable); val first=s.start(AuditLogAction.Export)
        s.locked=true; assertNull(s.c.takeForWriting(first,0)); assertNull(s.c.work); s.locked=false
        val second=s.start(AuditLogAction.Export); s.c.takeForWriting(second,0); s.active=s.profiles.last().id
        assertFalse(s.c.written(second,0,true)); assertNull(s.c.work)
    }
    @Test fun routeAndOwnerExitCannotCommitPendingRecording() {
        val s=Session(); s.c.observeRoute("audit"); val id=s.c.begin(AuditLogAction.Enable)!!; s.c.confirm(id)
        s.c.observeRoute("elsewhere"); s.c.advance(id,0); assertFalse(s.c.state.enabled)
        val next=s.c.begin(AuditLogAction.Enable)!!; s.c.confirm(next); s.signed.clear(); s.c.advance(next,0); assertFalse(s.c.state.enabled)
    }
    @Test fun profileWipeRemovesOnlyItsFilesAndAppEraseClearsEverything() {
        val s=Session(); s.start(AuditLogAction.Enable); val removed=s.profiles.last().id; s.profiles=s.profiles.dropLast(1); s.c.reconcile()
        assertEquals(1,s.c.state.files.size); assertTrue(s.c.state.files.none { it.profileId==removed })
        s.c.erase(); assertEquals(AuditLogState(),s.c.state); assertNull(s.c.work)
    }
    @Test fun archiveRejectsPathTraversalAndUsesStableEntryTimes() {
        val file=AuditLogFile(1,"profile","audit-session-1.jsonl","hello\n")
        assertArrayEquals(AuditLogs.archive(listOf(file)),AuditLogs.archive(listOf(file)))
        assertThrows(IllegalArgumentException::class.java) { AuditLogs.archive(listOf(file.copy(name="../secret"))) }
    }
    @Test fun interruptedWriterCanRetryWithANewDestinationLease() {
        val s=Session(); s.start(AuditLogAction.Enable); val id=s.start(AuditLogAction.Export)
        s.c.takeForWriting(id,0); s.c.interruptWriting()
        assertEquals(AuditLogFailure.Write,s.c.work!!.failure); assertFalse(s.c.written(id,0,true))
        assertTrue(s.c.retry(id)); s.step(); assertNotNull(s.c.takeForWriting(id,1))
        assertFalse(s.c.written(id,0,true)); assertEquals(AuditLogPhase.Writing,s.c.work!!.phase)
        assertTrue(s.c.written(id,1,true))
    }

}
