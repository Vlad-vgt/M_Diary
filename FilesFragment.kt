package com.md

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.SharedPreferences
import android.content.Intent
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class DiaryEntry(
val fileName: String,
val date: String,
val previewText: String
)

class FilesFragment : Fragment() {
	
	private lateinit var recyclerView: RecyclerView
	private val entries = mutableListOf<DiaryEntry>()
	private val REQUEST_CODE_PERMISSION = 1001
	private var activeProfileName: String = "Default"
	private lateinit var sharedPrefs: SharedPreferences
	private lateinit var adapter: DiaryAdapter
	
	override fun onCreateView(
	inflater: LayoutInflater,
	container: ViewGroup?,
	savedInstanceState: Bundle?
	): View? {
		val view = inflater.inflate(R.layout.fragment_files, container, false)
		recyclerView = view.findViewById(R.id.recyclerView)
		setupRecyclerView()
		return view
	}
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
	super.onViewCreated(view, savedInstanceState)
	loadProfileName()
	loadDiaryFiles()
	}
	
	override fun onResume() {
	super.onResume()
	// Обновляем список файлов при каждом возвращении на фрагмент
	loadDiaryFiles()
	}
	
	private fun loadProfileName() {
	sharedPrefs = requireContext().getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
	val activeProfileId = sharedPrefs.getString("activeProfileId", "default")
	val profilesJson = sharedPrefs.getString("profiles", null)
	
	activeProfileName = if (profilesJson != null) {
	val profiles = Gson().fromJson<List<Profile>>(profilesJson, object : TypeToken<List<Profile>>() {}.type)
	profiles.firstOrNull { it.id == activeProfileId }?.name ?: "Default"
	} else {
	"Default"
	}
	
	(activity as? MainActivity)?.setActionBarTitle(activeProfileName)
	//Toast.makeText(requireContext(), "Активный профиль: $activeProfileName", Toast.LENGTH_SHORT).show()
	}
	
	private fun setupRecyclerView() {
	recyclerView.layoutManager = LinearLayoutManager(requireContext())
	adapter = DiaryAdapter(entries) { fileName ->
	val intent = Intent(requireContext(), EditDiaryActivity::class.java).apply {
	putExtra("FILE_NAME", fileName)
	}
	startActivity(intent)
	}
	recyclerView.adapter = adapter
	recyclerView.addItemDecoration(
	DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
	}
	
	private fun loadDiaryFiles() {
	if (checkPermissions()) {
	val myDiaryDir = File(
	Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
	"MyDiary/$activeProfileName"
	)
	
	if (!myDiaryDir.exists()) {
	myDiaryDir.mkdirs()
	Toast.makeText(requireContext(), "Папка не найдена, создана новая", Toast.LENGTH_SHORT).show()
	return
	}
	
	val newEntries = mutableListOf<DiaryEntry>()
	val files = myDiaryDir.listFiles()
	
	files?.forEach { file ->
	if (file.isFile && file.name.endsWith(".txt")) {
	val previewText = try {
	file.readText().take(200)
	} catch (e: Exception) {
	"Не удалось прочитать файл"
	}
	newEntries.add(DiaryEntry(
	file.name,
	formatDateFromFileName(file.name),
	previewText
	))
	}
	}
	
	// Сортируем по убыванию даты (новые сверху)
	newEntries.sortByDescending { it.fileName }
	
	// Обновляем список только если есть изменения
	if (newEntries != entries) {
	entries.clear()
	entries.addAll(newEntries)
	adapter.notifyDataSetChanged()
	}
	}
	}
	
	private fun formatDateFromFileName(fileName: String): String {
	return try {
	val dateStr = fileName.removeSuffix(".txt")
	val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
	val date = sdf.parse(dateStr)
	SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date)
	} catch (e: Exception) {
	fileName
	}
	}
	
	class DiaryAdapter(
	private val entries: List<DiaryEntry>,
	private val onItemClick: (fileName: String) -> Unit
	) : RecyclerView.Adapter<DiaryAdapter.ViewHolder>() {
	
	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
	val dateTextView: TextView = view.findViewById(R.id.dateTextView)
	val previewTextView: TextView = view.findViewById(R.id.previewTextView)
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
	val view = LayoutInflater.from(parent.context)
	.inflate(R.layout.item_diary_entry, parent, false)
	return ViewHolder(view)
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
	val entry = entries[position]
	holder.dateTextView.text = entry.date
	holder.previewTextView.text = entry.previewText
	holder.itemView.setOnClickListener { onItemClick(entry.fileName) }
	}
	
	override fun getItemCount() = entries.size
	}
	
	private fun checkPermissions(): Boolean {
	if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
	return true
	}
	
	val writePermission = ContextCompat.checkSelfPermission(
	requireContext(),
	Manifest.permission.WRITE_EXTERNAL_STORAGE
	)
	
	if (writePermission != PackageManager.PERMISSION_GRANTED) {
	ActivityCompat.requestPermissions(
	requireActivity(),
	arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
	REQUEST_CODE_PERMISSION
	)
	return false
	}
	return true
	}
}