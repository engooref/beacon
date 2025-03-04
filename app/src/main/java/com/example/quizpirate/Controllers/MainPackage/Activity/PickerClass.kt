package com.example.quizpirate.Controllers.MainPackage.Activity

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import com.example.quizpirate.R

/**
 * Time Picker that shows a Dialog to select
 * hours, minutes and seconds. You can choose
 * to use only minutes and seconds by setting
 * to false the property "includeHours".
 */
class PickerClass() : DialogFragment() {

    private lateinit var timePickerLayout: View
    private lateinit var minPicker: NumberPicker
    private lateinit var secPicker: NumberPicker

    private var onTimeSetOption:
                (minute: Int, second: Int) -> Unit = { _, _ -> }
    private var timeSetText: String = "Ok"

    private var onCancelOption: () -> Unit = {}
    private var cancelText: String = "Cancel"


    /**
     * Which value will appear a the start of
     * the Dialog for the Minute picker.
     * Default value is 0.
     */
    var initialMinute: Int = 0
    /**
     * Which value will appear a the start of
     * the Dialog for the Second picker.
     * Default value is 0.
     */
    var initialSeconds: Int = 0


    /**
     * Max value for the Minute picker.
     * Default value is 59.
     */
    var maxValueMinute: Int = 59
    /**
     * Max value for the Second picker.
     * Default value is 59.
     */
    var maxValueSeconds: Int = 59

    /**
     * Min value for the Minute picker.
     * Default value is 0.
     */
    var minValueMinute: Int = 0
    /**
     * Min value for the Second picker.
     * Default value is 0.
     */
    var minValueSecond: Int = 0

    private var title: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            timePickerLayout = requireActivity()
                .layoutInflater.inflate(R.layout.picker_time, null)

            setupTimePickerLayout()

            builder.setView(timePickerLayout)

            title?.let { title ->
                builder.setTitle(title)
            }
            builder.setPositiveButton(timeSetText) { _, _ ->
                onTimeSetOption(minPicker.value, secPicker.value)
            }
                .setNegativeButton(cancelText) { _, _ ->
                    onCancelOption
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    /**
     * Set the title displayed in the Dialog
     */
    fun setTitle(title: String) {
        this.title = title
    }

    /**
     * Set a listener to be invoked when the Set Time button of the dialog is pressed.
     * If have set includeHours to false, the hour parameter here will be always 0.
     * @param text text to display in the Set Time button.
     */
    fun setOnTimeSetOption (text: String, onTimeSet: (minute: Int, second: Int) -> Unit) {
        onTimeSetOption = onTimeSet
        timeSetText = text
    }

    /**
     * Set a listener to be invoked when the Cancel button of the dialog is pressed.
     * @param text text to display in the Cancel button.
     */
    fun setOnCancelOption (text: String, onCancelOption: () -> Unit) {
        this.onCancelOption = onCancelOption
        cancelText = text
    }

    private fun setupTimePickerLayout() {
        bindViews()
        setupMaxValues()
        setupMinValues()
        setupInitialValues()
    }

    private fun bindViews() {
        minPicker = timePickerLayout.findViewById<NumberPicker>(R.id.minutes)
        secPicker = timePickerLayout.findViewById<NumberPicker>(R.id.seconds)
    }

    private fun setupMaxValues () {
        minPicker.maxValue = maxValueMinute
        secPicker.maxValue = maxValueSeconds
    }

    private fun setupMinValues () {
        minPicker.minValue = minValueMinute
        secPicker.minValue = minValueSecond
    }

    private fun setupInitialValues () {
        minPicker.value = initialMinute
        secPicker.value = initialSeconds
    }


}