package io.github.leogallego.ansiblejane.presentation.settings

import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.data.TokenSavingMode
import io.github.leogallego.ansiblejane.data.backup.BackupManager
import io.github.leogallego.ansiblejane.fakes.FakeAssistantRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupViewModelTest {

    private lateinit var backupManager: BackupManager
    private lateinit var fakeTokenManager: FakeTokenManager
    private lateinit var fakeAssistantRepo: FakeAssistantRepository
    private lateinit var viewModel: BackupViewModel

    private val instance1 = AapInstance(
        id = "inst-1",
        baseUrl = "https://aap1.example.com",
        token = "token-1",
        alias = "Production",
        apiVersion = "CONTROLLER_V2"
    )

    private val instance2 = AapInstance(
        id = "inst-2",
        baseUrl = "https://aap2.example.com",
        token = "token-2",
        alias = "Staging",
        apiVersion = "CONTROLLER_V2"
    )

    private val testLlmConfig = LlmProviderConfig.OpenAiCompatible(
        url = "https://openrouter.ai/api/v1",
        model = "anthropic/claude-sonnet-4-6",
        apiKey = "sk-test-key",
        tokenSavingMode = TokenSavingMode.STANDARD
    )

    @BeforeTest
    fun setup() {
        setupMainDispatcher()
        backupManager = BackupManager()
        fakeTokenManager = FakeTokenManager()
        fakeAssistantRepo = FakeAssistantRepository()
        viewModel = BackupViewModel(backupManager, fakeTokenManager, fakeAssistantRepo)
    }

    @AfterTest
    fun cleanup() {
        tearDownMainDispatcher()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(BackupUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `exportBackup with instances produces ExportReady`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))

        viewModel.exportBackup(password = "test123", includeAssistantConfig = false)

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.ExportReady)
        assertTrue((state as BackupUiState.ExportReady).data.isNotEmpty())
    }

    @Test
    fun `exportBackup with no instances emits Error`() = runTest {
        viewModel.exportBackup(password = "test123", includeAssistantConfig = false)

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.Error)
        assertEquals("No instances to export", (state as BackupUiState.Error).message)
    }

    @Test
    fun `exportBackup includes LLM config when requested`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        fakeAssistantRepo.savedConfig = testLlmConfig

        viewModel.exportBackup(password = "test123", includeAssistantConfig = true)

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.ExportReady)

        // Verify by importing back that LLM config is included
        val envelope = backupManager.importBackup(
            (state as BackupUiState.ExportReady).data, "test123"
        )
        assertNotNull(envelope.llmConfig)
    }

    @Test
    fun `exportBackup without LLM config omits it`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        fakeAssistantRepo.savedConfig = testLlmConfig

        viewModel.exportBackup(password = "test123", includeAssistantConfig = false)

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.ExportReady)

        val envelope = backupManager.importBackup(
            (state as BackupUiState.ExportReady).data, "test123"
        )
        assertNull(envelope.llmConfig)
    }

    @Test
    fun `startImport with correct password shows ImportPreview`() = runTest {
        val exportedData = backupManager.exportBackup("secret", listOf(instance1))

        // No existing instances, so all are new
        viewModel.startImport(exportedData, "secret")

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.ImportPreview)
        val preview = state as BackupUiState.ImportPreview
        assertEquals(1, preview.instances.size)
        assertEquals(1, preview.newCount)
        assertEquals(0, preview.duplicateCount)
        assertEquals(false, preview.hasExistingInstances)
    }

    @Test
    fun `startImport with wrong password shows Error`() = runTest {
        val exportedData = backupManager.exportBackup("correct", listOf(instance1))

        viewModel.startImport(exportedData, "wrong")

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.Error)
        assertEquals("Incorrect password or corrupted backup", (state as BackupUiState.Error).message)
    }

    @Test
    fun `startImport detects duplicates when instances already exist`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        val exportedData = backupManager.exportBackup("pass", listOf(instance1, instance2))

        viewModel.startImport(exportedData, "pass")

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.ImportPreview)
        val preview = state as BackupUiState.ImportPreview
        assertEquals(1, preview.duplicateCount)
        assertEquals(1, preview.newCount)
        assertEquals(true, preview.hasExistingInstances)
    }

    @Test
    fun `confirmImport MERGE skips duplicates`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        val exportedData = backupManager.exportBackup("pass", listOf(instance1, instance2))

        viewModel.startImport(exportedData, "pass")
        assertTrue(viewModel.uiState.value is BackupUiState.ImportPreview)

        viewModel.confirmImport(ImportMode.MERGE)

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.Success)
        assertEquals("Imported 1 instance(s)", (state as BackupUiState.Success).message)

        // instance1 was already there, instance2 was added
        assertEquals(2, fakeTokenManager.instances.value.size)
    }

    @Test
    fun `confirmImport REPLACE clears and re-imports all`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        val exportedData = backupManager.exportBackup("pass", listOf(instance2))

        viewModel.startImport(exportedData, "pass")
        assertTrue(viewModel.uiState.value is BackupUiState.ImportPreview)

        viewModel.confirmImport(ImportMode.REPLACE)

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.Success)
        assertEquals("Imported 1 instance(s)", (state as BackupUiState.Success).message)

        // instance1 was cleared, only instance2 remains
        val remaining = fakeTokenManager.instances.value
        assertEquals(1, remaining.size)
        assertEquals("https://aap2.example.com", remaining[0].baseUrl)
    }

    @Test
    fun `confirmImport with LLM config saves it and includes note`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        val exportedData = backupManager.exportBackup("pass", listOf(instance2), testLlmConfig)

        viewModel.startImport(exportedData, "pass")
        assertTrue(viewModel.uiState.value is BackupUiState.ImportPreview)

        viewModel.confirmImport(ImportMode.MERGE)

        val state = viewModel.uiState.value
        assertTrue(state is BackupUiState.Success)
        assertTrue((state as BackupUiState.Success).message.contains("LLM config"))

        // Verify LLM config was saved to repository
        assertNotNull(fakeAssistantRepo.savedConfig)
        val saved = fakeAssistantRepo.savedConfig as LlmProviderConfig.OpenAiCompatible
        assertEquals("anthropic/claude-sonnet-4-6", saved.model)
    }

    @Test
    fun `dismiss resets to Idle and clears pending envelope`() = runTest {
        val exportedData = backupManager.exportBackup("pass", listOf(instance1))

        viewModel.startImport(exportedData, "pass")
        assertTrue(viewModel.uiState.value is BackupUiState.ImportPreview)

        viewModel.dismiss()
        assertEquals(BackupUiState.Idle, viewModel.uiState.value)

        // confirmImport should be a no-op after dismiss
        viewModel.confirmImport(ImportMode.MERGE)
        assertEquals(BackupUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `confirmImport without pending envelope is a no-op`() = runTest {
        viewModel.confirmImport(ImportMode.MERGE)
        assertEquals(BackupUiState.Idle, viewModel.uiState.value)
    }
}
