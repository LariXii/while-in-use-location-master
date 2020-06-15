package com.example.android.whileinuselocation.controller

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.android.whileinuselocation.R
import com.example.android.whileinuselocation.SharedPreferenceUtil
import com.example.android.whileinuselocation.model.MyFileUtils
import com.example.android.whileinuselocation.model.User
import kotlinx.android.synthetic.main.activity_menu_principal.*
import kotlinx.android.synthetic.main.dialog_number_picker.*
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime

private const val TAG = "TruckTracker_Form"

class MenuPrincipalActivity: AppCompatActivity(), TextWatcher, SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var sharedPreferences: SharedPreferences

    private val listTextForm: MutableList<EditText> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        /*val file = File(applicationContext.filesDir,"mapm.csv")
        val fileStream = applicationContext.openFileOutput(file.name, Context.MODE_PRIVATE)
        fileStream.write("Franz jagt im komplett verwahrlosten Taxi quer durch Bayern".toByteArray())
        fileStream.close()
        Log.d(TAG,"Contenu : ${file.readText()}")
        val nom = MyFileUtils.getMD5EncryptedString(file.readBytes())
        Log.d(TAG,"MD5 CheckSum : $nom \nComparaison avec le code : ${nom == "a3cca2b2aa1e3b5b3b5aad99a8529074"} ${nom == "d41d8cd98f00b204e9800998ecf8427e"}")*/

        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        listTextForm.addAll(listOf(act_menu_txt_nom,act_menu_txt_roue,act_menu_txt_ess_tracteur,act_menu_txt_ess_remorque))

        act_menu_btn_valider.isEnabled = false
        act_menu_btn_save?.isVisible = false
        act_menu_btn_del.isVisible = sharedPreferences.contains(SharedPreferenceUtil.KEY_USER_NAME)
                || sharedPreferences.contains(SharedPreferenceUtil.KEY_TYRE_TYPE)
                || sharedPreferences.contains(SharedPreferenceUtil.KEY_TRACTOR_AXLES)
                || sharedPreferences.contains(SharedPreferenceUtil.KEY_TRAILER_AXLES)

        //Ajout d'un écouteur de changement de texte sur les champs du formulaire
        act_menu_txt_nom.addTextChangedListener(this)

        act_menu_txt_ess_tracteur.addTextChangedListener(this)
        act_menu_txt_ess_tracteur.showSoftInputOnFocus = false
        act_menu_txt_ess_tracteur.setOnClickListener {
            numberPickerCustom(INFORMATIONTRUCK.TRACTOR_AXLES)
        }
        act_menu_txt_ess_remorque.addTextChangedListener(this)
        act_menu_txt_ess_remorque.showSoftInputOnFocus = false
        act_menu_txt_ess_remorque.setOnClickListener {
            numberPickerCustom(INFORMATIONTRUCK.TRAILER_AXLES)
        }

        act_menu_txt_roue.addTextChangedListener(this)
        act_menu_txt_roue.showSoftInputOnFocus = false
        act_menu_txt_roue.setOnClickListener {
            numberPickerCustom(INFORMATIONTRUCK.TYRE_TYPE)
        }

        //Ajout d'un tag pour chaque champs du formulaire comme étant leur clé pour SharedPreference
        act_menu_txt_nom.tag =
            SharedPreferenceUtil.KEY_USER_NAME
        act_menu_txt_roue.tag =
            SharedPreferenceUtil.KEY_TYRE_TYPE
        act_menu_txt_ess_tracteur.tag =
            SharedPreferenceUtil.KEY_TRACTOR_AXLES
        act_menu_txt_ess_remorque.tag =
            SharedPreferenceUtil.KEY_TRAILER_AXLES

        //Ajout d'un écouteur de click sur le bouton de validation des informations
        act_menu_btn_valider.setOnClickListener{
            val user = User(act_menu_txt_nom.text.toString(),
                (act_menu_txt_roue.text.toString().toInt()),
                (act_menu_txt_ess_remorque.text.toString().toInt()),
                (act_menu_txt_ess_tracteur.text.toString().toInt()))
            Log.d(TAG,"User : $user")
            val locationActivity = Intent(this, JourneyActivity::class.java)
            locationActivity.putExtra("com.example.android.whileinuselocation.extra.USER", user)
            startActivity(locationActivity)
        }

        //Ajout d'un écouteur de click sur le bouton de sauvegarde des informations de l'utilisateur
        act_menu_btn_save.setOnClickListener{
            for(txt in listTextForm){
                if(txt.text.isNotEmpty()){
                    SharedPreferenceUtil.saveMenuInfo(
                        this,
                        txt.tag.toString(),
                        txt.text.toString()
                    )
                }
            }
            Toast.makeText(this, "Vos informations ont bien été sauvegardé !\n${sharedPreferences.all}" , Toast.LENGTH_SHORT).show()
        }

        //Ajout d'un écouteur de click sur le bouton de suppression des informations de l'utilisateur
        act_menu_btn_del.setOnClickListener{
            // TODO Supprimer les informations de l'utilisateur
            SharedPreferenceUtil.eraseMenuInfo(
                this,
                SharedPreferenceUtil.KEY_USER_NAME
            )
            SharedPreferenceUtil.eraseMenuInfo(
                this,
                SharedPreferenceUtil.KEY_TYRE_TYPE
            )
            SharedPreferenceUtil.eraseMenuInfo(
                this,
                SharedPreferenceUtil.KEY_TRACTOR_AXLES
            )
            SharedPreferenceUtil.eraseMenuInfo(
                this,
                SharedPreferenceUtil.KEY_TRAILER_AXLES
            )
            Toast.makeText(this, "Vos informations ont bien été supprimé !\n${sharedPreferences.all}" , Toast.LENGTH_SHORT).show()
            //Suppression des informations contenues dans les champs de texte
            for(txt in listTextForm){
                txt.text = null
            }
        }

        for(txt in listTextForm){
            txt.setText(sharedPreferences.getString(txt.tag.toString(), null))
        }

        if(sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED,false)){
            val locationActivity = Intent(this, JourneyActivity::class.java)
            val user = User(act_menu_txt_nom.text.toString(),
                (act_menu_txt_roue.text.toString().toInt()),
                (act_menu_txt_ess_remorque.text.toString().toInt()),
                (act_menu_txt_ess_tracteur.text.toString().toInt()))
            locationActivity.putExtra("com.example.android.whileinuselocation.extra.USER", user)
            startActivity(locationActivity)
            finish()
        }

    }

    private fun numberPickerCustom(info: INFORMATIONTRUCK) {
        val d = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_number_picker, null)
        d.setTitle(info.title)
        d.setView(dialogView)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)
        numberPicker.maxValue = info.maxValue
        numberPicker.minValue = info.minValue
        numberPicker.wrapSelectorWheel = false
        numberPicker.setOnValueChangedListener { _, _, _ -> println("onValueChange: ") }
        d.setPositiveButton("Valider") { _, _ ->
            for(txt in listTextForm){
                if(txt.tag == info.tag){
                    txt.setText(numberPicker.value.toString())
                }
            }
        }
        val alertDialog = d.create()
        alertDialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_items, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.item_menu_darktheme){
            return true
        }
        return super.onOptionsItemSelected(item)
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

enum class INFORMATIONTRUCK(val title: String, val minValue: Int, val maxValue: Int, val tag: String){
    TYRE_TYPE("Type de roue : ",1,3, SharedPreferenceUtil.KEY_TYRE_TYPE),
    TRAILER_AXLES("Nombre d'essieu de la remorque : ",1,7, SharedPreferenceUtil.KEY_TRAILER_AXLES),
    TRACTOR_AXLES("Nombre d'essieu du tracteur : ",1,7, SharedPreferenceUtil.KEY_TRACTOR_AXLES)
}

