package com.example.konohaproject.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import com.example.konohaproject.R
import com.example.konohaproject.databinding.FragmentSettingsListDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSettingsListDialogBinding? = null
    private val binding get() = _binding!!

    private val focusValues = mutableListOf<Int>()
    private val shortBreakValues = mutableListOf<Int>()
    private val longBreakValues = mutableListOf<Int>()

    interface SettingsListener {
        fun onSettingsChanged(focusTime: Int, shortBreak: Int, longBreak: Int)
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

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            savePreferences()
            notifySettingsChanged()
            dismiss()
        }
    }

    private fun savePreferences() {
        val prefs = requireContext().getSharedPreferences("timer_settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("focus_time", focusValues[binding.seekBarFocusTime.progress])
            putInt("short_break", shortBreakValues[binding.seekBarShortBreak.progress])
            putInt("long_break", longBreakValues[binding.seekBarLongBreak.progress])
            apply()
        }
    }

    private fun loadSavedPreferences() {
        val prefs = requireContext().getSharedPreferences("timer_settings", Context.MODE_PRIVATE)
        binding.seekBarFocusTime.progress = focusValues.indexOf(prefs.getInt("focus_time", 25)).coerceAtLeast(0)
        binding.seekBarShortBreak.progress = shortBreakValues.indexOf(prefs.getInt("short_break", 5)).coerceAtLeast(0)
        binding.seekBarLongBreak.progress = longBreakValues.indexOf(prefs.getInt("long_break", 15)).coerceAtLeast(0)

        updateFocusTime(binding.seekBarFocusTime.progress)
        updateShortBreak(binding.seekBarShortBreak.progress)
        updateLongBreak(binding.seekBarLongBreak.progress)
    }

    private fun notifySettingsChanged() {
        val listener = parentFragment as? SettingsListener ?: activity as? SettingsListener
        listener?.onSettingsChanged(
            focusValues[binding.seekBarFocusTime.progress],
            shortBreakValues[binding.seekBarShortBreak.progress],
            longBreakValues[binding.seekBarLongBreak.progress]
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}