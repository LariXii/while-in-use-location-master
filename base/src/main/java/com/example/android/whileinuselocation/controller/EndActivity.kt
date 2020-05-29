package com.example.android.whileinuselocation.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.android.whileinuselocation.R
import com.example.android.whileinuselocation.model.Journey
import kotlinx.android.synthetic.main.activity_end.*

class EndActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end)

        val endIntent: Intent = intent
        textView_end.text = endIntent.getParcelableExtra<Journey>("journey")?.toString()

        btn_end.setOnClickListener{
            val firstActivityIntent = Intent(this,MenuPrincipalActivity::class.java)
            firstActivityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(firstActivityIntent)
            finish()
        }
    }

}