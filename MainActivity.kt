package com.md

import com.google.android.material.bottomnavigation.BottomNavigationView
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.commit
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.md.DiaryFragment

class MainActivity : AppCompatActivity() {
private lateinit var bottomAppBar: BottomAppBar

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_main)

// Инициализация BottomAppBar
//bottomAppBar = findViewById(R.id.bottomAppBar)

// Инициализация начального фрагмента
//if (savedInstanceState == null) {
//supportFragmentManager.commit {
//replace(R.id.fragment_container, DiaryFragment())
//}
//}

val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

bottomNavigationView.setOnNavigationItemSelectedListener { item ->
when (item.itemId) {
R.id.home -> {
// Переключение на фрагмент Home
supportFragmentManager.beginTransaction()
.replace(R.id.fragment_container, DiaryFragment())
.commit()
true
}
R.id.files -> {
// Переключение на фрагмент Folder
supportFragmentManager.beginTransaction()
.replace(R.id.fragment_container, FilesFragment())
.commit()
true
}
R.id.profile -> {
// Переключение на фрагмент Profile
supportFragmentManager.beginTransaction()
.replace(R.id.fragment_container, ProfileFragment())
.commit()
true
}
else -> false
}
}

// Установите начальный выбранный элемент (например, Home)
bottomNavigationView.selectedItemId = R.id.home
}

fun setActionBarTitle(title: String) {
supportActionBar?.title = title
}

override fun onCreateOptionsMenu(menu: Menu): Boolean {
menuInflater.inflate(R.menu.main_menu, menu)
return true
}

override fun onOptionsItemSelected(item: MenuItem): Boolean {
return when (item.itemId) {
R.id.action_save -> {
Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show()
true
}
R.id.action_about -> {
Toast.makeText(this, "About Clicked", Toast.LENGTH_SHORT).show()
true
}
else -> super.onOptionsItemSelected(item)
}
}
}