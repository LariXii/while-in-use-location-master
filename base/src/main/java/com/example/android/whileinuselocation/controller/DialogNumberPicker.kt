package com.example.android.whileinuselocation.controller

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.android.whileinuselocation.R
import kotlinx.android.synthetic.main.dialog_number_picker.*

class DialogNumberPicker(private val _context: Context, private val _minValue: Int, private val _maxValue: Int): DialogFragment() {
    private lateinit var listener: DialogNPListener
    var value: Int = 0

    interface DialogNPListener {
        fun onDialogPositiveClick(dialog: DialogNumberPicker)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try{
            listener = context as DialogNPListener
        }
        catch(e: ClassCastException){
            throw ClassCastException(("$context must implement DialogNPListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            builder.setView(inflater.inflate(R.layout.dialog_number_picker,null))
                .setMessage(R.string.dialog_type_roue)
                .setPositiveButton(R.string.ok
                ) { _, _ ->
                    listener.onDialogPositiveClick(this)
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        number_picker.maxValue = _maxValue
        number_picker.minValue = _minValue
        number_picker.setOnValueChangedListener { _, _, newVal ->
            value = newVal
        }
    }

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_number_picker)

        number_picker.maxValue = _maxValue
        number_picker.minValue = _minValue

        setTitle("Nombre de roue")

        // set the custom dialog components - text, image and button
        textView_dialog.text = "Android custom dialog example!"

        // if button is clicked, close the custom dialog
        btn_dialog_ok.setOnClickListener{
            dismiss()
        }

        btn_dialog_ok.setOnClickListener {
            Toast.makeText(_context, "Selected number ${number_picker.value}", Toast.LENGTH_SHORT).show()
        }
    }*/
}