package com.diegowh.konohaproject.ui.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.viewModels
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentSettingsListDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class SettingsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSettingsListDialogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    interface SettingsListener {
        fun onSettingsChanged(focusTime: Int, shortBreak: Int, longBreak: Int, rounds: Int)
        fun onDismiss();
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsListDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSavedPreferences()
        setupSeekBars()
        setupAutorunSwitch()
        setupMuteSwitch()
        setupSaveButton()
        setupResetButton()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSeekBars() {
        with(binding) {
            seekBarFocusTime.apply {
                max = viewModel.focusValues.size - 1
                progress = viewModel.focusProgress
                setOnSeekBarChangeListener(createSeekBarListener(::updateFocusTime))
            }

            seekBarShortBreak.apply {
                max = viewModel.shortBreakValues.size - 1
                progress = viewModel.shortBreakProgress
                setOnSeekBarChangeListener(createSeekBarListener(::updateShortBreak))
            }

            seekBarLongBreak.apply {
                max = viewModel.longBreakValues.size - 1
                progress = viewModel.longBreakProgress
                setOnSeekBarChangeListener(createSeekBarListener(::updateLongBreak))
            }

            seekBarRounds.apply {
                max = viewModel.roundsValues.size - 1
                progress = viewModel.roundsProgress
                setOnSeekBarChangeListener(createSeekBarListener(::updateRounds))
            }
        }
    }

    private fun setupAutorunSwitch() {
        binding.btnAutorun.apply {
            isChecked = viewModel.autorun
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.autorun = isChecked
            }
        }
    }

    private fun setupMuteSwitch() {
        binding.btnMute.apply {
            isChecked = viewModel.mute
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.mute = isChecked
            }
        }
    }

    private fun createSeekBarListener(updateFunction: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                when (seekBar) {
                    binding.seekBarFocusTime -> viewModel.focusProgress = progress
                    binding.seekBarShortBreak -> viewModel.shortBreakProgress = progress
                    binding.seekBarLongBreak -> viewModel.longBreakProgress = progress
                    binding.seekBarRounds -> viewModel.roundsProgress = progress
                }
                updateFunction(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
    }

    private fun updateFocusTime(progress: Int) {
        val value = viewModel.focusValues[progress]
        binding.seekBarFocusTime.progress = progress
        binding.txtFocusTime.text = getString(R.string.minutes_format, value)
    }

    private fun updateShortBreak(progress: Int) {
        val value = viewModel.shortBreakValues[progress]
        binding.seekBarShortBreak.progress = progress
        binding.txtShortBreak.text = getString(R.string.minutes_format, value)
    }

    private fun updateLongBreak(progress: Int) {
        val value = viewModel.longBreakValues[progress]
        binding.seekBarLongBreak.progress = progress
        binding.txtLongBreak.text = getString(R.string.minutes_format, value)
    }

    private fun updateRounds(progress: Int) {
        val value = viewModel.roundsValues[progress]
        binding.seekBarRounds.progress = progress
        binding.txtRounds.text = String.format(Locale.US, "%d", value)
    }

    private fun updateAutorun(state: Boolean) {
        binding.btnAutorun.isChecked = state
        viewModel.autorun = state
    }

    private fun updateMute(state: Boolean) {
        binding.btnMute.isChecked = state
        viewModel.mute = state
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            viewModel.savePreferences(requireContext())
            notifySettingsChanged()
            dismiss()
        }
    }

    private fun setupResetButton() {
        binding.btnReset.setOnClickListener {
            val defaults = viewModel.getDefaultIndices()
            updateFocusTime(defaults.focusIdx)
            updateShortBreak(defaults.shortBreakIdx)
            updateLongBreak(defaults.longBreakIdx)
            updateRounds(defaults.roundsIdx)
            updateAutorun(defaults.autorun)
            updateMute(defaults.mute)

        }
    }

    private fun loadSavedPreferences() {
        viewModel.loadSavedPreferences(requireContext())
        updateFocusTime(viewModel.focusProgress)
        updateShortBreak(viewModel.shortBreakProgress)
        updateLongBreak(viewModel.longBreakProgress)
        updateRounds(viewModel.roundsProgress)
    }

    private fun notifySettingsChanged() {
        val listener = parentFragment as? SettingsListener ?: activity as? SettingsListener
        listener?.onSettingsChanged(
            viewModel.focusValues[viewModel.focusProgress],
            viewModel.shortBreakValues[viewModel.shortBreakProgress],
            viewModel.longBreakValues[viewModel.longBreakProgress],
            viewModel.roundsValues[viewModel.roundsProgress]
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val listener = parentFragment as? SettingsListener ?: activity as? SettingsListener
        listener?.onDismiss()
    }

    companion object {
        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}