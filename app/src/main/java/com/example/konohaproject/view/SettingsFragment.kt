package com.example.konohaproject.view

import com.example.konohaproject.controller.TimeConfig
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.example.konohaproject.R
import com.example.konohaproject.databinding.FragmentSettingsListDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class SettingsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSettingsListDialogBinding? = null
    private val binding get() = _binding!!

    private val focusValues = mutableListOf<Int>()
    private val shortBreakValues = mutableListOf<Int>()
    private val longBreakValues = mutableListOf<Int>()
    private val roundsValues = mutableListOf<Int>()

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
        setupFocusValues()
        setupShortBreakValues()
        setupLongBreakValues()
        setupRoundsValues()
        setupSeekBars()
        loadSavedPreferences()
        setupSaveButton()
    }


    private fun setupFocusValues() {

        var current = 10
        while (current <= 60) {
            focusValues.add(current)
            current += 5
        }
        current = 75
        while (current <= 90) {
            focusValues.add(current)
            current += 15
        }
    }

    private fun setupShortBreakValues() {

        var current = 2
        while (current <= 5) {
            shortBreakValues.add(current)
            current += 1
        }
        current = 10
        while (current <= 15) {
            shortBreakValues.add(current)
            current += 5
        }
    }

    private fun setupLongBreakValues() {

        var current = 15
        while (current <= 40) {
            longBreakValues.add(current)
            current += 5
        }
    }

    private fun setupRoundsValues() {

        var current = 2
        while (current <= 8) {
            roundsValues.add(current)
            current += 1
        }
    }

    private fun setupSeekBars() {
        binding.seekBarFocusTime.apply {
            max = focusValues.size - 1
            setOnSeekBarChangeListener(createSeekBarListener(::updateFocusTime))
        }

        binding.seekBarShortBreak.apply {
            max = shortBreakValues.size - 1
            setOnSeekBarChangeListener(createSeekBarListener(::updateShortBreak))
        }

        binding.seekBarLongBreak.apply {
            max = longBreakValues.size - 1
            setOnSeekBarChangeListener(createSeekBarListener(::updateLongBreak))
        }

        binding.seekBarRounds.apply {
            max = roundsValues.size - 1
            setOnSeekBarChangeListener(createSeekBarListener(::updateRounds ))
        }
    }

    private fun createSeekBarListener(updateFunction: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateFunction(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
    }

    private fun updateFocusTime(progress: Int) {
        val value = focusValues[progress]
        binding.txtFocusTime.text = getString(R.string.minutes_format, value)
    }

    private fun updateShortBreak(progress: Int) {
        val value = shortBreakValues[progress]
        binding.txtShortBreak.text = getString(R.string.minutes_format, value)
    }

    private fun updateLongBreak(progress: Int) {
        val value = longBreakValues[progress]
        binding.txtLongBreak.text = getString(R.string.minutes_format, value)
    }

    private fun updateRounds(progress: Int) {
        val value = roundsValues[progress]
        binding.txtRounds.text = String.format(Locale.US, "%d", value)
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            savePreferences()
            notifySettingsChanged()
            dismiss()
        }
    }

    private fun savePreferences() {
        TimeConfig.updateSettings(
            requireContext(),
            focus = focusValues[binding.seekBarFocusTime.progress].toLong(),
            shortBreak = shortBreakValues[binding.seekBarShortBreak.progress].toLong(),
            longBreak = longBreakValues[binding.seekBarLongBreak.progress].toLong(),
            rounds = roundsValues[binding.seekBarRounds.progress],
            autoRestart = true
        )
    }

    private fun loadSavedPreferences() {
        val context = requireContext()
        binding.seekBarFocusTime.progress = focusValues.indexOf(TimeConfig.getFocusMinutes(context).toInt())
        binding.seekBarShortBreak.progress = shortBreakValues.indexOf(TimeConfig.getShortBreakMinutes(context).toInt())
        binding.seekBarLongBreak.progress = longBreakValues.indexOf(TimeConfig.getLongBreakMinutes(context).toInt())
        binding.seekBarRounds.progress = roundsValues.indexOf(TimeConfig.getTotalRounds(context))

        updateFocusTime(binding.seekBarFocusTime.progress)
        updateShortBreak(binding.seekBarShortBreak.progress)
        updateLongBreak(binding.seekBarLongBreak.progress)
        updateRounds(binding.seekBarRounds.progress)
    }

    private fun notifySettingsChanged() {
        val listener = parentFragment as? SettingsListener ?: activity as? SettingsListener
        listener?.onSettingsChanged(
            focusValues[binding.seekBarFocusTime.progress],
            shortBreakValues[binding.seekBarShortBreak.progress],
            longBreakValues[binding.seekBarLongBreak.progress],
            roundsValues[binding.seekBarRounds.progress]
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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