package com.idenc.snapceit

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Clear cache if greater than 50 MB
        if (getCacheSize() > 5e+7) {
            applicationContext.cacheDir.deleteRecursively()
        }
    }

    private fun getCacheSize(): Long {
        var size: Long = 0
        cacheDir?.also {
            it.listFiles()?.also { files ->
                for (f in files) {
                    size += f.length()
                }
            }
        }
        return size
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.itemId == R.id.action_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, 0)
        }
        return true
    }

}