package com.example.android.whileinuselocation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import kotlinx.android.synthetic.main.activity_menu_principal.*

class MenuPrincipalActivity: AppCompatActivity(), TextWatcher {

    private lateinit var sharedPreferences: SharedPreferences

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
            SharedPreferenceUtil.saveMenuPref(this,SharedPreferenceUtil.KEY_USER_NAME,act_menu_txt_nom.text.toString())
            SharedPreferenceUtil.saveMenuPref(this,SharedPreferenceUtil.KEY_TYRE_TYPE,act_menu_txt_roue.text.toString())
            SharedPreferenceUtil.saveMenuPref(this,SharedPreferenceUtil.KEY_TRACTOR_AXLES,act_menu_txt_ess_tracteur.text.toString())
            SharedPreferenceUtil.saveMenuPref(this,SharedPreferenceUtil.KEY_TRAILER_AXLES,act_menu_txt_ess_remorque.text.toString())

            val locationActivity = Intent(this, MainActivity::class.java)
            startActivity(locationActivity)
        }

        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        if(sharedPreferences.getString(SharedPreferenceUtil.KEY_TRACTOR_AXLES,null) != null){
            act_menu_txt_ess_tracteur.setText(sharedPreferences.getString(SharedPreferenceUtil.KEY_TRACTOR_AXLES, null))
        }
        if(sharedPreferences.getString(SharedPreferenceUtil.KEY_TRAILER_AXLES, null) != null){
            act_menu_txt_ess_remorque.setText(sharedPreferences.getString(SharedPreferenceUtil.KEY_TRAILER_AXLES, null))
        }
        if(sharedPreferences.getString(SharedPreferenceUtil.KEY_TYRE_TYPE, null) != null){
            act_menu_txt_roue.setText(sharedPreferences.getString(SharedPreferenceUtil.KEY_TYRE_TYPE, null))
        }
        act_menu_txt_nom.setText(sharedPreferences.getString(SharedPreferenceUtil.KEY_USER_NAME, null))
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
