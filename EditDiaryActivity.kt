	package com.md

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import com.google.android.material.appbar.MaterialToolbar
import android.os.Environment
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream

class EditDiaryActivity : AppCompatActivity() {

private lateinit var editText: EditText
private lateinit var file: File
private var isChanged = false
private var originalContent = ""
private var activeProfileName: String = ""
private lateinit var sharedPrefs: SharedPreferences

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_edit_diary)

editText = findViewById(R.id.editText)

// Получаем SharedPreferences
sharedPrefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)

// Способ 1: Получаем ID активного профиля и имя
val activeProfileId = sharedPrefs.getString("activeProfileId", "default")
val profilesJson = sharedPrefs.getString("profiles", null)
val ProfileName = if (profilesJson != null) {
val profiles = Gson().fromJson<List<Profile>>(profilesJson, object : TypeToken<List<Profile>>() {}.type)
profiles.firstOrNull { it.id == activeProfileId }?.name ?: "Default"
} else {
"Default"
}
activeProfileName = ProfileName
//Toast.makeText(this, "Активный профиль: $activeProfileName", Toast.LENGTH_SHORT).show()

// Получаем путь к файлу из интента
val fileName = intent.getStringExtra("FILE_NAME") ?: run {
finish()
return
}

val myDiaryDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MyDiary/$activeProfileName")
file = File(myDiaryDir, fileName)

// Настройка Toolbar
val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
setSupportActionBar(toolbar)
supportActionBar?.setDisplayHomeAsUpEnabled(true)
//val fileNameNotSuffix : String = fileName.removeSuffix(".txt")
supportActionBar?.title = "$fileName"

// Настройка FAB для удаления
val fabDelete = findViewById<FloatingActionButton>(R.id.fabDelete)
fabDelete.setOnClickListener {
showDeleteConfirmationDialog()
}

// Загружаем содержимое файла
try {
originalContent = file.readText()
editText.setText(originalContent)
} catch (e: Exception) {
Toast.makeText(this, "Ошибка при чтении файла", Toast.LENGTH_SHORT).show()
finish()
}

// Отслеживаем изменения
editText.addTextChangedListener(object : TextWatcher {
override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
override fun afterTextChanged(s: Editable?) {
isChanged = s.toString() != originalContent
}
})
}

private fun showDeleteConfirmationDialog() {
AlertDialog.Builder(this)
.setTitle("Удаление записи")
.setMessage("Вы уверены, что хотите удалить эту запись?")
.setPositiveButton("Удалить") { _, _ ->
deleteFileAndExit()
}
.setNegativeButton("Отмена", null)
.show()
}

private fun deleteFileAndExit() {
try {
if (file.exists()) {
if (file.delete()) {
Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
finish()
} else {
Toast.makeText(this, "Не удалось удалить запись", Toast.LENGTH_SHORT).show()
}
}
} catch (e: Exception) {
Toast.makeText(this, "Ошибка при удалении файла", Toast.LENGTH_SHORT).show()
}
}

override fun onSupportNavigateUp(): Boolean {
onBackPressed()
return true
}

private fun saveIfChanged() {
if (!isChanged) return

val newContent = editText.text.toString()
try {
FileOutputStream(file).use {
it.write(newContent.toByteArray())
}
Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show()
} catch (e: Exception) {
Toast.makeText(this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show()
}
}

override fun onBackPressed() {
if (isChanged) {
AlertDialog.Builder(this)
.setTitle("Есть несохраненные изменения")
.setMessage("Сохранить изменения перед выходом?")
.setPositiveButton("Сохранить") { _, _ ->
saveIfChanged()
super.onBackPressed()
}
.setNegativeButton("Не сохранять") { _, _ ->
super.onBackPressed()
}
.setNeutralButton("Отмена", null)
.show()
} else {
super.onBackPressed()
}
}
}