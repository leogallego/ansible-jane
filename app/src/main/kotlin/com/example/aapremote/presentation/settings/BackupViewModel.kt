package com.example.aapremote.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.assistant.data.AssistantRepository
import com.example.aapremote.data.TokenManager
import com.example.aapremote.data.backup.BackupDecryptionException
import com.example.aapremote.data.backup.BackupEnvelope
import com.example.aapremote.data.backup.BackupManager
import com.example.aapremote.network.ApiVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ImportMode { MERGE, REPLACE }

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Exporting : BackupUiState
    data class ExportReady(val data: ByteArray) : BackupUiState
    data class ImportPreview(
        val envelope: BackupEnvelope,
        val rawData: ByteArray,
        val password: String,
        val duplicateCount: Int,
        val newCount: Int
    ) : BackupUiState
    data object Importing : BackupUiState
    data class Success(val message: String) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

class BackupViewModel(
    private val backupManager: BackupManager,
    private val tokenManager: TokenManager,
    private val assistantRepository: AssistantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportBackup(password: String, includeAssistantConfig: Boolean) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Exporting
            try {
                val instances = tokenManager.instances.value
                if (instances.isEmpty()) {
                    _uiState.value = BackupUiState.Error("No instances to export")
                    return@launch
                }
                val llmConfig = if (includeAssistantConfig) assistantRepository.loadLlmConfig() else null
                val data = backupManager.exportBackup(password, instances, llmConfig)
                _uiState.value = BackupUiState.ExportReady(data)
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Export failed: ${e.message}")
            }
        }
    }

    fun startImport(data: ByteArray, password: String) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Importing
            try {
                val envelope = backupManager.importBackup(data, password)
                val existingUrls = tokenManager.instances.value
                    .map { it.baseUrl.trimEnd('/').lowercase() }
                    .toSet()

                var duplicates = 0
                var newOnes = 0
                for (inst in envelope.instances) {
                    if (inst.baseUrl.trimEnd('/').lowercase() in existingUrls) duplicates++ else newOnes++
                }

                _uiState.value = BackupUiState.ImportPreview(
                    envelope = envelope,
                    rawData = data,
                    password = password,
                    duplicateCount = duplicates,
                    newCount = newOnes
                )
            } catch (e: BackupDecryptionException) {
                _uiState.value = BackupUiState.Error(e.message ?: "Decryption failed")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun confirmImport(mode: ImportMode) {
        val preview = _uiState.value as? BackupUiState.ImportPreview ?: return
        viewModelScope.launch {
            _uiState.value = BackupUiState.Importing
            try {
                if (mode == ImportMode.REPLACE) {
                    tokenManager.clearCredentials()
                }

                val existingUrls = tokenManager.instances.value
                    .map { it.baseUrl.trimEnd('/').lowercase() }
                    .toSet()

                var imported = 0
                for (inst in preview.envelope.instances) {
                    val normalizedUrl = inst.baseUrl.trimEnd('/').lowercase()
                    if (mode == ImportMode.MERGE && normalizedUrl in existingUrls) continue

                    val apiVersion = try {
                        ApiVersion.valueOf(inst.apiVersion)
                    } catch (_: Exception) {
                        ApiVersion.CONTROLLER_V2
                    }

                    tokenManager.saveInstance(
                        baseUrl = inst.baseUrl,
                        token = inst.token,
                        alias = inst.alias,
                        apiVersion = apiVersion,
                        trustSelfSigned = inst.trustSelfSigned,
                        certFingerprint = inst.certFingerprint
                    )

                    if (inst.mcpEnabled || !inst.mcpServerUrls.isNullOrEmpty()) {
                        val savedInstances = tokenManager.instances.value
                        val savedId = savedInstances.find {
                            it.baseUrl.trimEnd('/').lowercase() == normalizedUrl
                        }?.id
                        if (savedId != null) {
                            tokenManager.updateMcpConfig(savedId, inst.mcpEnabled, inst.mcpServerUrls)
                        }
                    }
                    imported++
                }

                preview.envelope.llmConfig?.let { config ->
                    assistantRepository.saveLlmConfig(config)
                }

                val llmNote = if (preview.envelope.llmConfig != null) " + LLM config" else ""
                _uiState.value = BackupUiState.Success("Imported $imported instance(s)$llmNote")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun dismiss() {
        _uiState.value = BackupUiState.Idle
    }
}
