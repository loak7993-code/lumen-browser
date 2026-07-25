package com.lwbrowser

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.lwbrowser.databinding.DialogConfirmBinding

object Dialogs {

    fun confirm(
        context: Context,
        iconRes: Int = R.drawable.ic_delete,
        title: CharSequence,
        message: CharSequence,
        positiveText: CharSequence,
        negativeText: CharSequence,
        onPositive: () -> Unit
    ) {
        val binding = DialogConfirmBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg_rounded)
        }
        binding.dialogIcon.setImageResource(iconRes)
        binding.dialogTitle.text = title
        binding.dialogMessage.text = message
        binding.btnPositive.text = positiveText
        binding.btnNegative.text = negativeText
        binding.btnPositive.setOnClickListener { dialog.dismiss(); onPositive() }
        binding.btnNegative.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
