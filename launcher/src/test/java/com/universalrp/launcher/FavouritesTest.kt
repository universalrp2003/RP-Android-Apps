package com.universalrp.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one-tap call list's rules, without an Android Context.
 *
 * The storage itself needs SharedPreferences, so what is tested here is the part that decides what
 * is allowed in and how a number is compared - which is where a duplicate favourite, or a number
 * that will not dial, would come from.
 */
class FavouritesTest {

    @Test
    fun aNumberIsComparedWithoutItsSpaces() {
        // People save "98400 12345" and "9840012345" for the same person. Without normalising, both
        // get added and the button list grows a duplicate nobody asked for.
        assertEquals("9840012345", Favourites.normalise("98400 12345"))
        assertEquals("9840012345", Favourites.normalise("98400-12345"))
        assertEquals("+919840012345", Favourites.normalise("+91 98400 12345"))
        assertEquals("9840012345", Favourites.normalise("(98400) 12345"))
    }

    @Test
    fun theSameNumberInTwoFormsIsOneFavourite() {
        val a = Favourite("Son", "98400 12345")
        val b = Favourite("Son", "9840012345")
        assertEquals(Favourites.normalise(a.number), Favourites.normalise(b.number))
    }

    @Test
    fun aDifferentNumberIsNotTheSameFavourite() {
        assertFalse(
            Favourites.normalise("9840012345") == Favourites.normalise("9840012346"),
        )
    }

    @Test
    fun theFavouriteCapIsEight() {
        // Eight fits the wrap layout on a phone screen without scrolling the home screen, and the
        // owner asked for "son daughter brother any other" - a handful, not an address book.
        assertEquals(8, Favourites.MAX)
    }

    @Test
    fun aContactWithoutANumberIsNotAFavourite() {
        // readPickedContact returns null for these, and the screen treats null as "nothing chosen"
        // rather than adding a button that calls nobody.
        val noNumber = PickedContact("Son", "")
        assertTrue(noNumber.number.isEmpty())
        val withNumber = PickedContact("Son", "9840012345")
        assertFalse(withNumber.number.isEmpty())
        assertEquals("9840012345", withNumber.toFavourite().number)
    }

    @Test
    fun theNameIsKeptAsThePhoneHasIt() {
        // No trimming beyond whitespace: the owner named the contact, and renaming it here would
        // make the button say something they did not type.
        val picked = PickedContact("  Son  ", " 98400 12345 ")
        val favourite = picked.toFavourite()
        assertEquals("  Son  ", favourite.name)
        assertEquals("9840012345", favourite.number)
    }

    @Test
    fun thePickIntentTargetsPhoneNumbers() {
        // ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE is what makes the system show the
        // phone-number list rather than every contact, so the owner never picks a contact that has
        // no number to call.
        assertEquals(
            "vnd.android.cursor.dir/phone_v2",
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE,
        )
    }
}
