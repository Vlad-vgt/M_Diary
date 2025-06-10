package com.md

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.MenuInflater
import android.view.Menu
import android.app.Activity
import android.os.Environment
import android.widget.EditText
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.FileInputStream
import android.content.Context
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class DiaryFragment : Fragment() {

private lateinit var editText: EditText
private var fileName: String = "default_file.txt"
private var savedYearMonth: String = ""
private var YearMonth: String = ""
private var Time: String = ""
private var originalText: String = ""
private val REQUEST_CODE_PERMISSION = 1001
private lateinit var sharedPrefs: SharedPreferences
private var activeProfileName: String = "Default"

override fun onCreateView(
inflater: LayoutInflater,
container: ViewGroup?,
savedInstanceState: Bundle?
): View? {
val view = inflater.inflate(R.layout.fragment_diary, container, false)

editText = view.findViewById(R.id.editText)
// Получаем SharedPreferences
val sharedPrefs = requireContext().getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)

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
(activity as? MainActivity)?.setActionBarTitle(activeProfileName)
//Toast.makeText(requireContext(), "Активный профиль: $activeProfileName", Toast.LENGTH_SHORT).show()
/////////!????!!??!!(()!//!/:!)

// Инициализация и загрузка данных
checkAndCreateDiaryFolder()
currentDateTime()

// Извлекаем сохраненный год/месяц
val sharedPreferences = requireContext().getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
savedYearMonth = sharedPreferences.getString("my_key_year", "").orEmpty()

// Проверяем разрешения
if (checkPermissions()) {
if (savedYearMonth == YearMonth) {
// Загружаем данные из файла
fileName = "$savedYearMonth.txt"
loadFileToEditText()
originalText = editText.text.toString()
} else {
// Устанавливаем текущую дату и время в EditText
editText.setText("$YearMonth\n$Time\n\n")
originalText = editText.text.toString()
// Сохраняем текущий год/месяц в SharedPreferences
val editor = sharedPreferences.edit()
editor.putString("my_key_year", YearMonth)
editor.apply()
// Устанавливаем имя файла
fileName = "$YearMonth.txt"
}
}
return view
}
///////

// 
///////
private fun checkPermissions(): Boolean {
val writePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
val readPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)

if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
// Запрашиваем оба разрешения
ActivityCompat.requestPermissions(
requireActivity(),
arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
REQUEST_CODE_PERMISSION
)
return false
}
return true
}

override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
super.onRequestPermissionsResult(requestCode, permissions, grantResults)
if (requestCode == REQUEST_CODE_PERMISSION) {
if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
// Разрешение предоставлено, загружаем файл
//loadFileToEditText()
} else {
// Разрешение не предоставлено, выводим сообщение
Toast.makeText(requireContext(), "Разрешение на чтение/запись файлов не предоставлено", Toast.LENGTH_SHORT).show()
}
}
}

private fun currentDateTime() {
val currentTime = Date()
val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
val formatterA = SimpleDateFormat("HH.mm", Locale.getDefault())
Time = formatterA.format(currentTime)
YearMonth = formatter.format(currentTime)
}
///////////
private fun checkAndCreateDiaryFolder() {
if (activeProfileName.isBlank()) {
Toast.makeText(requireContext(), "Имя профиля не задано", Toast.LENGTH_SHORT).show()
return
}

val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
documentsDir.mkdirs()

val myDiaryFolder = File(documentsDir, "MyDiary/$activeProfileName")

if (!myDiaryFolder.exists()) {
val isCreated = myDiaryFolder.mkdirs()
if (isCreated) {
Toast.makeText(requireContext(), "Папка создана: ${myDiaryFolder.path}", Toast.LENGTH_SHORT).show()
} else {
Toast.makeText(requireContext(), "Ошибка создания папки", Toast.LENGTH_SHORT).show()
}
}
}
///////////////
private fun loadFileToEditText() {
val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
val file = File(documentsDir, "MyDiary/$activeProfileName/$fileName")

if (file.exists()) {
try {
val inputStream = FileInputStream(file)
val size = inputStream.available()
val buffer = ByteArray(size)
inputStream.read(buffer)
inputStream.close()

val fileContent = String(buffer, Charsets.UTF_8)
editText.setText("$fileContent\n\n$Time\n")
editText.setSelection(editText.text.length)
} catch (e: IOException) {
e.printStackTrace()
Toast.makeText(requireContext(), "Ошибка при чтении файла", Toast.LENGTH_SHORT).show()
editText.setText("$YearMonth\n$Time\n")
}
} else {
Toast.makeText(requireContext(), "Новая запись сегодня", Toast.LENGTH_SHORT).show()
editText.setText("$YearMonth\n$Time\n\n")
}
}
////////////
private fun saveToFile() {
// Получаем первую видимую строку (с очисткой от недопустимых символов)
val firstLine = if (editText.layout != null && editText.layout.lineCount > 0) {
val start = editText.layout.getLineStart(0)
val end = editText.layout.getLineEnd(0)
editText.text.substring(start, end).toString()
} else {
editText.text.toString().lines().firstOrNull() ?: ""
}.replace("[^a-zA-Z0-9-_. ]".toRegex(), "") // Удаляем недопустимые символы

val text = editText.text.toString()
if (text.isEmpty()) {
Toast.makeText(requireContext(), "Текст для сохранения пуст", Toast.LENGTH_SHORT).show()
return
}

fileName = "$firstLine.txt"

try {
// Создаём все необходимые директории
val documentsDir = File(
Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
"MyDiary/$activeProfileName"
)
if (!documentsDir.exists()) {
documentsDir.mkdirs()
}

val file = File(documentsDir, fileName)

FileOutputStream(file).use { fos ->
fos.write(text.toByteArray())
}
Toast.makeText(requireContext(), "Файл сохранен: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
} catch (e: Exception) {
e.printStackTrace()
Toast.makeText(requireContext(), "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
}
}

override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
inflater.inflate(R.menu.main_menu, menu)
super.onCreateOptionsMenu(menu, inflater)
}

override fun onOptionsItemSelected(item: MenuItem): Boolean {
return when (item.itemId) {
R.id.action_save -> {
saveToFile()
true
}
else -> super.onOptionsItemSelected(item)
}
}

override fun onStop() {
super.onStop()
val currentText = editText.text.toString()
if (currentText != originalText) {
saveToFile()
}
}

}