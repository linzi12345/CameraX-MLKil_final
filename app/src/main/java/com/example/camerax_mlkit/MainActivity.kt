package com.example.camerax_mlkit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private var mLive: Button? = null
    private var mStill: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mLive = findViewById(R.id.live_preview_button)
        mStill = findViewById(R.id.still_image_button)

        findViewById<Button>(R.id.live_preview_button).setOnClickListener(View.OnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    MaskMainActivity::class.java
                )
            )
        })


        findViewById<Button>(R.id.still_image_button).setOnClickListener(View.OnClickListener {
                startActivity(
                    Intent(
                        this@MainActivity,
                        SmileMainActivity::class.java
                    )
                )
            })

        findViewById<Button>(R.id.button).setOnClickListener(View.OnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    eyeMainActivity::class.java
                )
            )
        })
        }
}
