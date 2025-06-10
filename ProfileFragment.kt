package com.md

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

class ProfileFragment : Fragment() {
	
	private lateinit var sharedPrefs: SharedPreferences
	private lateinit var adapter: ProfileAdapter
	private val MAX_PROFILES = 10
	private val APP_DIRECTORY = "MyDiary"
	
	override fun onCreateView(
	inflater: LayoutInflater,
	container: ViewGroup?,
	savedInstanceState: Bundle?
	): View? {
		val view = inflater.inflate(R.layout.fragment_profile, container, false)
		
		val recyclerView = view.findViewById<RecyclerView>(R.id.profileRecyclerView)
		val addButton = view.findViewById<FloatingActionButton>(R.id.addProfileButton)
		
		sharedPrefs = requireActivity().getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
		
		// Загрузка профилей
		val profiles = loadProfiles()
		
		// Инициализация адаптера
		adapter = ProfileAdapter(
		profiles = profiles,
		onProfileSelected = { profile ->
		switchProfile(profile)
		},
		onProfileDeleted = { profile ->
		showDeleteDialog(profile)
		}
		)
		
		// Настройка RecyclerView
		recyclerView.layoutManager = LinearLayoutManager(requireContext())
		recyclerView.adapter = adapter
		recyclerView.addItemDecoration(
		DividerItemDecoration(
		requireContext(),
		DividerItemDecoration.VERTICAL
		)
		)
		
		// Обработчик кнопки добавления
		addButton.setOnClickListener {
		if (adapter.profiles.size >= MAX_PROFILES) {
		showToast("Достигнуто максимальное количество профилей")
		} else {
		showAddProfileDialog()
		}
		}
		
		adapter.updateProfiles(profiles)
		adapter.notifyDataSetChanged()
		
		// После загрузки профилей обновляем ActionBar
		val activeProfile = profiles.firstOrNull { it.isActive }
		activeProfile?.let {
		(activity as? MainActivity)?.setActionBarTitle(it.name)
		}
		
		return view
		}
		
		private fun switchProfile(profile: Profile) {
		val updatedProfiles = adapter.profiles.map {
		it.copy(isActive = it.id == profile.id)
		}
		adapter.updateProfiles(updatedProfiles)
		adapter.notifyDataSetChanged()
		saveActiveProfile(profile.id)
		
		// Меняем заголовок ActionBar
		val prName: String = profile.name
		(activity as? MainActivity)?.setActionBarTitle(prName)
		
		//showToast("Активирован профиль: ${profile.name}")
		}
		
		private fun showDeleteDialog(profile: Profile) {
		if (profile.isDefault) return
		
		AlertDialog.Builder(requireContext())
		.setTitle("Удалить профиль '${profile.name}'?")
		.setMessage("Все данные этого профиля будут удалены.")
		.setPositiveButton("Удалить") { _, _ ->
		deleteProfile(profile)
		}
		.setNegativeButton("Отмена", null)
		.show()
		}
		
		private fun showAddProfileDialog() {
		val input = EditText(requireContext()).apply {
		hint = "Введите название профиля"
		maxLines = 1
		}
		
		AlertDialog.Builder(requireContext())
		.setTitle("Новый профиль")
		.setView(input)
		.setPositiveButton("Создать") { _, _ ->
		val name = input.text.toString().trim()
		when {
		name.isEmpty() -> showToast("Название не может быть пустым")
		adapter.profiles.any { it.name.equals(name, ignoreCase = true) } ->
		showToast("Профиль с таким именем уже существует")
		else -> createProfile(name)
		}
		}
		.setNegativeButton("Отмена", null)
		.show()
		}
		
		private fun createProfile(name: String) {
		// Создаем папку профиля
		if (!createProfileDirectory(name)) {
		showToast("Ошибка при создании папки профиля")
		return
		}
		
		val newProfile = Profile(
		id = UUID.randomUUID().toString(),
		name = name,
		isActive = false
		)
		val updatedProfiles = adapter.profiles + newProfile
		adapter.updateProfiles(updatedProfiles)
		saveProfiles(updatedProfiles)
		showToast("Профиль '$name' создан")
		}
		
		private fun deleteProfile(profile: Profile) {
		// Удаляем папку профиля
		if (!deleteProfileDirectory(profile.name)) {
		showToast("Ошибка при удалении папки профиля")
		return
		}
		
		val updatedProfiles = adapter.profiles.filter { it.id != profile.id }
		
		// Если удаляем активный профиль, активируем профиль по умолчанию
		if (profile.isActive && updatedProfiles.isNotEmpty()) {
		val defaultProfile = updatedProfiles.firstOrNull { it.isDefault } ?: updatedProfiles.first()
		switchProfile(defaultProfile)
		}
		
		adapter.updateProfiles(updatedProfiles)
		saveProfiles(updatedProfiles)
		showToast("Профиль '${profile.name}' удален")
		}
		
		private fun createProfileDirectory(profileName: String): Boolean {
		return try {
		val appDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), APP_DIRECTORY)
		if (!appDir.exists()) {
		appDir.mkdirs()
		}
		
		val profileDir = File(appDir, profileName)
		if (!profileDir.exists()) {
		profileDir.mkdirs()
		}
		true
		} catch (e: Exception) {
		e.printStackTrace()
		false
		}
		}
		
		private fun deleteProfileDirectory(profileName: String): Boolean {
		return try {
		val profileDir = File(
		Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
		"$APP_DIRECTORY/$profileName"
		)
		if (profileDir.exists()) {
		deleteRecursive(profileDir)
		}
		true
		} catch (e: Exception) {
		e.printStackTrace()
		false
		}
		}
		
		private fun deleteRecursive(fileOrDirectory: File) {
		if (fileOrDirectory.isDirectory) {
		fileOrDirectory.listFiles()?.forEach { child ->
		deleteRecursive(child)
		}
		}
		fileOrDirectory.delete()
		}
		
		private fun loadProfiles(): List<Profile> {
		val json = sharedPrefs.getString("profiles", null)
		val activeProfileId = sharedPrefs.getString("activeProfileId", "default")
		
		val loadedProfiles = if (json != null) {
		val type = object : TypeToken<List<Profile>>() {}.type
		Gson().fromJson<List<Profile>>(json, type) ?: getDefaultProfiles()
		} else {
		getDefaultProfiles()
		}
		
		// Восстанавливаем активный профиль
		return loadedProfiles.map { profile ->
		profile.copy(isActive = profile.id == activeProfileId)
		}
		}
		
		private fun getDefaultProfiles(): List<Profile> {
		return listOf(
		Profile(
		id = "default",
		name = "Мой дневник",
		isActive = true,
		isDefault = true
		)
		)
		}
		
		private fun saveProfiles(profiles: List<Profile>) {
		sharedPrefs.edit()
		.putString("profiles", Gson().toJson(profiles))
		.apply()
		}
		
		private fun saveActiveProfile(profileId: String) {
		sharedPrefs.edit()
		.putString("activeProfileId", profileId)
		.apply()
		}
		
		private fun showToast(message: String) {
		Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
		}
		
		inner class ProfileAdapter(
		var profiles: List<Profile>,
		private val onProfileSelected: (Profile) -> Unit,
		private val onProfileDeleted: (Profile) -> Unit
		) : RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {
		
		inner class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val name: TextView = itemView.findViewById(R.id.profileName)
		val deleteBtn: ImageButton = itemView.findViewById(R.id.deleteProfileButton)
		}
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
		val view = LayoutInflater.from(parent.context)
		.inflate(R.layout.item_profile, parent, false)
		return ProfileViewHolder(view)
		}
		
		override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
		val profile = profiles[position]
		
		holder.name.text = profile.name
		holder.itemView.isSelected = profile.isActive
		holder.deleteBtn.visibility = if (profile.isDefault) View.GONE else View.VISIBLE
		
		// Принудительно обновляем фон
		holder.itemView.background = when {
		profile.isActive -> ContextCompat.getDrawable(
		holder.itemView.context,
		R.drawable.bg_profile_item_selected
		)
		else -> ContextCompat.getDrawable(
		holder.itemView.context,
		R.drawable.bg_profile_item_normal
		)
		}
		
		holder.itemView.setOnClickListener { onProfileSelected(profile) }
		holder.deleteBtn.setOnClickListener { onProfileDeleted(profile) }
		}
		
		override fun getItemCount(): Int = profiles.size
		
		fun updateProfiles(newProfiles: List<Profile>) {
		profiles = newProfiles
		notifyDataSetChanged()
		}
		}
		}
		
		data class Profile(
		val id: String,
		val name: String,
		val isActive: Boolean,
		val isDefault: Boolean = false
		)