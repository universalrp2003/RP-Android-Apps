package com.universalrp.launcher

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

/**
 * The one-tap call list.
 *
 * The owner asked for it directly: "need the important persons one click call button like son
 * daughter brother any other". On a phone whose home screen is eight large buttons, the most useful
 * thing those buttons can do is call the people who matter, and hunting through a contact list to
 * find a son's number is exactly the friction this screen exists to remove.
 *
 * The contacts are read from the phone's own contact provider, so there is no second copy of the
 * owner's address book anywhere, and nothing leaves the device.
 */
data class Favourite(
    val name: String,
    val number: String,
)

object Favourites {

    const val MAX = 8

    /** Reads every contact that has a phone number, sorted by name. */
    fun all(context: Context): List<Favourite> {
        val found = mutableListOf<Favourite>()
        val resolver: ContentResolver = context.contentResolver
        val cursor: Cursor = try {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC",
            ) ?: return emptyList()
        } catch (error: Exception) {
            // A phone with no contacts provider, or one that refused the query, is an empty list
            // rather than a crash. A launcher that crashes on open is worse than no launcher.
            return emptyList()
        }
        cursor.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (nameIndex < 0 || numberIndex < 0) return emptyList()
            val seen = mutableSetOf<String>()
            while (it.moveToNext()) {
                val name = it.getString(nameIndex)?.trim().orEmpty()
                val number = it.getString(numberIndex)?.trim().orEmpty()
                if (name.isEmpty() || number.isEmpty()) continue
                // A contact with three numbers would otherwise appear three times.
                val key = name + "\u0000" + number
                if (!seen.add(key)) continue
                found.add(Favourite(name = name, number = normalise(number)))
            }
        }
        return found
    }

    /**
     * Strips the spaces, dashes and brackets people put in numbers, so "98400 12345" and
     * "9840012345" are the same contact and a duplicate cannot be added.
     */
    fun normalise(raw: String): String = raw.filter { it.isDigit() || it == '+' }

    /** The stored favourites, in the order they were added. */
    fun load(context: Context): List<Favourite> {
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val raw = preferences.getString(KEY, null) ?: return emptyList()
        return raw.split(RECORD).mapNotNull { record ->
            if (record.isBlank()) return@mapNotNull null
            val name = record.substringBefore(SEP)
            val number = record.substringAfter(SEP, "")
            if (number.isEmpty()) null else Favourite(name = name, number = number)
        }
    }

    fun save(context: Context, list: List<Favourite>) {
        val encoded = list.joinToString(RECORD) { it.name + SEP + it.number }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, encoded)
            .apply()
    }

    /** Adds one, keeping the cap and refusing a number that is already there. */
    fun add(context: Context, favourite: Favourite): Boolean {
        val current = load(context)
        if (current.size >= MAX) return false
        if (current.any { normalise(it.number) == normalise(favourite.number) }) return false
        save(context, current + favourite)
        return true
    }

    fun remove(context: Context, number: String) {
        save(context, load(context).filterNot { normalise(it.number) == normalise(number) })
    }

    /** Places the call directly. Falls back to the dialler if the permission is missing. */
    fun call(context: Context, favourite: Favourite) =
        LauncherActions.placeDirectCall(context, favourite.number)

    private const val FILE = "launcher_favourites"
    private const val KEY = "list"
    private const val SEP = "\u001f"
    private const val RECORD = "\u001e"
}

/** A contact chosen in the picker, before it becomes a favourite. */
data class PickedContact(val name: String, val number: String) {
    fun toFavourite() = Favourite(name = name, number = Favourites.normalise(number))
}

/** The contact picker's result, read from the URI the system returns. */
fun readPickedContact(context: Context, uri: Uri): PickedContact? {
    val resolver = context.contentResolver
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
    )
    return try {
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val name = if (nameIndex >= 0) cursor.getString(nameIndex)?.trim().orEmpty() else ""
            val number = if (numberIndex >= 0) cursor.getString(numberIndex)?.trim().orEmpty() else ""
            if (number.isEmpty()) null else PickedContact(name = name, number = number)
        }
    } catch (error: Exception) {
        null
    }
}
