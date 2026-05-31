package com.example.arpegio.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Convertidores de tipo para Room.
 *
 * Room solo soporta tipos primitivos, String y ByteArray de forma nativa.
 * Esta clase proporciona serialización/deserialización mediante Gson para los
 * tipos complejos usados en el modelo Song:
 *
 * - Map<String, String> → JSON (campo "tabs")
 * - List<String>` → JSON (campo tags")
 * - Date → Long (campo "createdAt")
 *
 * Registrado globalmente en AppDataBase
 */
class Converters {
    private val gson = Gson()

    // Map<String, String> (tabs: instrumento → ruta del archivo)

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    // List<String> (tags: etiquetas de clasificación)

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // Date (createdAt: timestamp de creación)

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}