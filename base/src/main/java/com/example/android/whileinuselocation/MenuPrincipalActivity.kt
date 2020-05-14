package com.example.android.whileinuselocation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import kotlinx.android.synthetic.main.activity_menu_principal.*

class MenuPrincipalActivity: AppCompatActivity(), TextWatcher {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        act_menu_btn_valider.isEnabled = false

        act_menu_txt_ess_tracteur.addTextChangedListener(this)
        act_menu_txt_ess_remorque.addTextChangedListener(this)
        act_menu_txt_nom.addTextChangedListener(this)
        act_menu_txt_roue.addTextChangedListener(this)

        act_menu_btn_valider.setOnClickListener{
            // TODO Sauvegarder les préférences de l'utilisateur

            val locationActivity = Intent(this, MainActivity::class.java)
            startActivity(locationActivity)
        }
    }

    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // Le bouton est activé que lorsque les quatres champs contiennent une valeur
        act_menu_btn_valider?.isEnabled = act_menu_txt_ess_tracteur.text.isNotEmpty()
                && act_menu_txt_nom.text.isNotEmpty()
                && act_menu_txt_roue.text.isNotEmpty()
                && act_menu_txt_ess_remorque.text.isNotEmpty()
    }
}