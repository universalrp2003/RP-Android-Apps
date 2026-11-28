package com.universalrp.tamilnadufm.ui

/** Discovery shortcuts, not a claim that a dedicated live station exists for each person. */
data class ArtistShortcut(val name: String, val query: String, val photo: String = "")

object ArtistBrowse {
    /** Explicit, third-party on-demand pages; these are not Media3 stream URLs.
     * Availability, subscription and playback are controlled by Spotify. */
    val externalCollections: Map<String, String> = mapOf(
        "vijay antony" to "https://open.spotify.com/artist/6CtYzQvENTdGq5LPPsePdV",
        "sivaji" to "https://open.spotify.com/album/2n2qfLKzVKYuiTP6xNGxkI",
        "santhanam" to "https://open.spotify.com/album/5iy7VICB9uVJue3tdSRnoD",
        "susheela" to "https://open.spotify.com/artist/15i8ZpQP4ZTWUXNo7VA2OE",
        "vidyasagar" to "https://open.spotify.com/artist/5TB837uKG65w8Jnz5D0bS5",
        "vivek" to "https://open.spotify.com/album/1sbHi5Osl4tIpv12uJ82Cw",
        "goundamani" to "https://open.spotify.com/album/2YTVteE18wKXpWIU9Eyg0I",
    )

    val artists = listOf(
        ArtistShortcut("Ilaiyaraaja", "ilayaraja", "https://upload.wikimedia.org/wikipedia/commons/2/26/Ilaiyaraaja_at_Merku_Thodarchi_Malai_Press_Meet.jpg"),
        ArtistShortcut("A. R. Rahman", "rahman", "https://upload.wikimedia.org/wikipedia/commons/f/f7/AR_Rahman_At_The_%E2%80%98Marvel_Anthem%E2%80%99_Launch.jpg"),
        ArtistShortcut("S. P. Balasubrahmanyam", "spb", "https://upload.wikimedia.org/wikipedia/commons/4/4b/S._P._Balasubrahmanyam_in_2013.JPG"),
        ArtistShortcut("K. S. Chithra", "chitra", "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a4/KS_Chithra.jpg/330px-KS_Chithra.jpg"),
        ArtistShortcut("P. Susheela", "susheela", "https://upload.wikimedia.org/wikipedia/commons/6/63/With_P_Susheela_%28cropped%29.jpg"),
        ArtistShortcut("K. J. Yesudas", "yesudas", "https://upload.wikimedia.org/wikipedia/commons/b/bb/Kj_yesudas.jpg"),
        ArtistShortcut("Mano", "mano", "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/dd/Playback_singer_Mano.JPG/500px-Playback_singer_Mano.JPG"),
        ArtistShortcut("Harris Jayaraj", "harris", "https://upload.wikimedia.org/wikipedia/commons/9/9d/Harris_Jayaraj.jpg"),
        ArtistShortcut("Anirudh", "anirudh", "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b6/Anirudh_Ravichander.jpg/500px-Anirudh_Ravichander.jpg"),
        ArtistShortcut("Yuvan Shankar Raja", "yuvan", "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b6/Yuvan_Shankar_Raja_exclusive_HQ_Photos_Silverscreen.jpg/500px-Yuvan_Shankar_Raja_exclusive_HQ_Photos_Silverscreen.jpg"),
        ArtistShortcut("D. Imman", "imman", "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c8/D_Imman_at_Panjumittai_Audio_Launch.jpg/500px-D_Imman_at_Panjumittai_Audio_Launch.jpg"),
        ArtistShortcut("Vijay Antony", "vijay antony", "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1d/VijayAntony.JPG/330px-VijayAntony.JPG"),
        ArtistShortcut("Deva", "deva", "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1d/Music_Director_Deva_at_KJ_Yesudas_50_Concert_Press_Meet.jpg/330px-Music_Director_Deva_at_KJ_Yesudas_50_Concert_Press_Meet.jpg"),
        ArtistShortcut("Vidyasagar", "vidyasagar", "https://upload.wikimedia.org/wikipedia/commons/5/5f/Vidyasagar_at_Anarkali_celebration.jpg"),
        ArtistShortcut("Shankar", "shankar"),
        ArtistShortcut("Hariharan", "hariharan", "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4d/A_Hariharan.JPG/330px-A_Hariharan.JPG"),
        ArtistShortcut("Mohan", "mohan"),
        ArtistShortcut("Malaysia Vasudevan", "vasudevan"),
        ArtistShortcut("S. A. Rajkumar", "rajkumar"),
        ArtistShortcut("Sujatha", "sujatha", "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8e/Sujatha_%28cropped%29.jpg/330px-Sujatha_%28cropped%29.jpg"),
        ArtistShortcut("S. Janaki", "janaki", "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/de/S_Janaki_in_Pune%2C_India_2007.JPG/500px-S_Janaki_in_Pune%2C_India_2007.JPG"),
        ArtistShortcut("Swarnalatha", "swarnalatha"),
        ArtistShortcut("Anuradha", "anuradha"),
        ArtistShortcut("Ajith", "ajith", "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/3f/Ajith_Kumar_at_Irungattukottai_Race_Track.jpg/500px-Ajith_Kumar_at_Irungattukottai_Race_Track.jpg"),
        ArtistShortcut("Vijay", "vijay", "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/95/Vijay_at_Puli_Audio_Launch.jpg/500px-Vijay_at_Puli_Audio_Launch.jpg"),
        ArtistShortcut("Kamal Haasan", "kamal", "https://upload.wikimedia.org/wikipedia/commons/1/12/Kamal_at_61st_FF_%28cropped%29.jpg"),
        ArtistShortcut("Rajinikanth", "rajini", "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/40/Rajinikanth_Felicitates_Writer_Kalaignanam.jpg/330px-Rajinikanth_Felicitates_Writer_Kalaignanam.jpg"),
        ArtistShortcut("Suriya", "suriya", "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7d/Soorya_sivakumar.jpg/500px-Soorya_sivakumar.jpg"),
        ArtistShortcut("Dhanush", "dhanush", "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/86/Dhanush_at_the_%E2%80%98Asuran%E2%80%99_Success_Meet.jpg/330px-Dhanush_at_the_%E2%80%98Asuran%E2%80%99_Success_Meet.jpg"),
        ArtistShortcut("Vijayakanth", "vijayakanth", "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1c/Vijaykanth_at_the_Sagaptham_Audio_Launch.jpg/500px-Vijaykanth_at_the_Sagaptham_Audio_Launch.jpg"),
        ArtistShortcut("Ramarajan", "ramarajan"),
        ArtistShortcut("T. Rajendar", "rajendar"),
        ArtistShortcut("Na. Muthukumar", "muthukumar", "https://upload.wikimedia.org/wikipedia/commons/a/a3/Na_Muthukumar_at_Touring_Talkies_Audio_Launch_%28cropped%29.jpg"),
        ArtistShortcut("Vaali", "vaali"),
        ArtistShortcut("M. G. Ramachandran", "mgr", "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0f/MGR_-OIL_PAINTING_BY_RAJASEKHARAN.jpg/500px-MGR_-OIL_PAINTING_BY_RAJASEKHARAN.jpg"),
        ArtistShortcut("Sivaji Ganesan", "sivaji", "https://upload.wikimedia.org/wikipedia/commons/f/f3/Sivaji_Ganesan_1.jpg"),
        ArtistShortcut("M. S. Viswanathan", "viswanathan", "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6a/M.s.v.JPG/500px-M.s.v.JPG"),
        ArtistShortcut("Vani Jayaram", "vani", "https://upload.wikimedia.org/wikipedia/commons/2/24/Vani_Jairam_2014_FF_%28cropped%29.jpg"),
        ArtistShortcut("Kannadasan", "kannadasan"),
        ArtistShortcut("T. M. Soundararajan", "soundararajan"),
        ArtistShortcut("A. M. Rajah", "rajah"),
        ArtistShortcut("J. P. Chandrababu", "chandrababu"),
        ArtistShortcut("P. B. Sreenivas", "sreenivas"),
        ArtistShortcut("Vadivelu", "vadivelu", "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fc/Vadivelu_1_at_Trailer_%26_HD_Songs_Launch_of_Kaththi_Sandai_%28cropped%29.jpg/500px-Vadivelu_1_at_Trailer_%26_HD_Songs_Launch_of_Kaththi_Sandai_%28cropped%29.jpg"),
        ArtistShortcut("Goundamani", "goundamani"),
        ArtistShortcut("Vivek", "vivek", "https://upload.wikimedia.org/wikipedia/commons/9/9b/Vivek_at_the_Ezhumin_Press_Meet_%28headshot%29.jpg"),
        ArtistShortcut("Santhanam", "santhanam", "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a4/Santhanam_at_the_%E2%80%98A1%E2%80%99_Press_Meet_%28cropped%29.jpg/500px-Santhanam_at_the_%E2%80%98A1%E2%80%99_Press_Meet_%28cropped%29.jpg"),
    )
}
