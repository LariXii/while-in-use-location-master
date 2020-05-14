package com.example.android.whileinuselocation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_menu_principal.*

class MenuPrincipalActivity: AppCompatActivity(), TextWatcher, SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var sharedPreferences: SharedPreferences

    private val listTextForm: MutableList<EditText> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        Log.d("Menu","Toutes les préférences à la création de l'activité : ${sharedPreferences.all}")

        listTextForm.addAll(listOf(act_menu_txt_nom,act_menu_txt_roue,act_menu_txt_ess_tracteur,act_menu_txt_ess_remorque))

        act_menu_btn_valider.isEnabled = false
        act_menu_btn_save?.isVisible = false
        act_menu_btn_del.isVisible = sharedPreferences.contains(SharedPreferenceUtil.KEY_USER_NAME)
                || sharedPreferences.contains(SharedPreferenceUtil.KEY_TYRE_TYPE)
                || sharedPreferences.contains(SharedPreferenceUtil.KEY_TRACTOR_AXLES)
                || sharedPreferences.contains(SharedPreferenceUtil.KEY_TRAILER_AXLES)

        //Ajout d'un écouteur de changement de texte sur les champs du formulaire
        act_menu_txt_ess_tracteur.addTextChangedListener(this)
        act_menu_txt_ess_remorque.addTextChangedListener(this)
        act_menu_txt_nom.addTextChangedListener(this)
        act_menu_txt_roue.addTextChangedListener(this)

        //Ajout d'un tag pour chaque champs du formulaire comme étant leur clé pour SharedPreference
        act_menu_txt_nom.tag = SharedPreferenceUtil.KEY_USER_NAME
        act_menu_txt_roue.tag = SharedPreferenceUtil.KEY_TYRE_TYPE
        act_menu_txt_ess_tracteur.tag = SharedPreferenceUtil.KEY_TRACTOR_AXLES
        act_menu_txt_ess_remorque.tag = SharedPreferenceUtil.KEY_TRAILER_AXLES

        //Ajout d'un écouteur de click sur le bouton de validation des informations
        act_menu_btn_valider.setOnClickListener{
            val locationActivity = Intent(this, MainActivity::class.java)
            startActivity(locationActivity)
        }

        //Ajout d'un écouteur de click sur le bouton de sauvegarde des informations de l'utilisateur
        act_menu_btn_save.setOnClickListener{
            // TODO Sauvegarder les informations de l'utilisateur
            for(txt in listTextForm){
                if(txt.text.isNotEmpty()){
                    SharedPreferenceUtil.saveMenuInfo(this, txt.tag.toString(), txt.text.toString())
                }
            }
            Toast.makeText(this, "Vos informations ont bien été sauvegardé !\n${sharedPreferences.all}" , Toast.LENGTH_SHORT).show()
        }

        //Ajout d'un écouteur de click sur le bouton de suppression des informations de l'utilisateur
        act_menu_btn_del.setOnClickListener{
            // TODO Supprimer les informations de l'utilisateur
            SharedPreferenceUtil.eraseMenuInfo(this, SharedPreferenceUtil.KEY_USER_NAME)
            SharedPreferenceUtil.eraseMenuInfo(this, SharedPreferenceUtil.KEY_TYRE_TYPE)
            SharedPreferenceUtil.eraseMenuInfo(this, SharedPreferenceUtil.KEY_TRACTOR_AXLES)
            SharedPreferenceUtil.eraseMenuInfo(this, SharedPreferenceUtil.KEY_TRAILER_AXLES)
            Toast.makeText(this, "Vos informations ont bien été supprimé !\n${sharedPreferences.all}" , Toast.LENGTH_SHORT).show()
            //Suppression des informations contenues dans les champs de texte
            for(txt in listTextForm){
                txt.text = null
            }
        }

        for(txt in listTextForm){
            txt.setText(sharedPreferences.getString(txt.tag.toString(), null))
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
        //Le bouton de sauvegarde des informations de l'utilisateur est activé que lorsque ces champs contiennent une information
        act_menu_btn_save?.isVisible = act_menu_txt_ess_tracteur.text.isNotEmpty()
                || act_menu_txt_nom.text.isNotEmpty()
                || act_menu_txt_roue.text.isNotEmpty()
                || act_menu_txt_ess_remorque.text.isNotEmpty()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        act_menu_btn_del.isVisible = sharedPreferences.contains(SharedPreferenceUtil.KEY_USER_NAME)
                || sharedPreferences.contains(SharedPreferenceUtil.KEY_TYRE_TYPE)
                || sharedPreferences.contains(SharedPreferenceUtil.KEY_TRACTOR_AXLES)
                || sharedPreferences.contains(SharedPreferenceUtil.KEY_TRAILER_AXLES)
    }
}
