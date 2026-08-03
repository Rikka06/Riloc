package com.riloc.app.ui.kernelFlash.state

import android.app.Activity
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FlashState(
    val isFlashing: Boolean = false,
    val isCompleted: Boolean = false,
    val progress: Float = 0f,
    val currentStep: String = "",
    val logs: List<String> = emptyList(),
    val error: String = ""
)

class HorizonKernelState {
    private val _state = MutableStateFlow(FlashState())
    val state: StateFlow<FlashState> = _state.asStateFlow()

    fun updateProgress(progress: Float) {
        _state.update { it.copy(progress = progress) }
    }

    fun updateStep(step: String) {
        _state.update { it.copy(currentStep = step) }
    }

    fun addLog(log: String) {
        _state.update { it.copy(logs = it.logs + log) }
    }

    fun setError(error: String) {
        _state.update { it.copy(error = error) }
    }

    fun startFlashing() {
        _state.update {
            it.copy(
                isFlashing = true,
                isCompleted = false,
                progress = 0f,
                currentStep = "Preparing UI template preview...",
                logs = emptyList(),
                error = ""
            )
        }
    }

    fun completeFlashing() {
        _state.update { it.copy(isFlashing = false, isCompleted = true, progress = 1f) }
    }
}

class HorizonKernelWorker(
    private val context: Context,
    private val state: HorizonKernelState,
    private val slot: String? = null,
    private val kpmPatchEnabled: Boolean = false,
    private val kpmUndoPatch: Boolean = false
) : Thread() {
    var uri: Uri? = null
    private var onFlashComplete: (() -> Unit)? = null

    fun setOnFlashCompleteListener(listener: () -> Unit) {
        onFlashComplete = listener
    }

    override fun run() {
        state.startFlashing()
        state.updateStep("Template preview")
        state.updateProgress(0.25f)
        state.addLog("UI template: no kernel file is modified.")
        state.addLog("Selected slot: ${slot ?: "current"}")
        state.addLog("KPM patch: ${if (kpmPatchEnabled) "shown only" else "off"}")
        state.addLog("KPM undo patch: ${if (kpmUndoPatch) "shown only" else "off"}")
        state.addLog("Input URI: ${uri?.scheme ?: "none"}")
        state.updateProgress(0.75f)
        state.updateStep("Finished template simulation")
        state.completeFlashing()

        (context as? Activity)?.runOnUiThread {
            onFlashComplete?.invoke()
        }
    }
}

