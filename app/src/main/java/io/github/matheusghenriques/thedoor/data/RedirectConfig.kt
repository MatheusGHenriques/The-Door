package io.github.matheusghenriques.thedoor.data

enum class RedirectType { NONE, APP, THE_DOOR }

data class RedirectConfig(
    val type: RedirectType = RedirectType.THE_DOOR,
    val appPackage: String = "",
    val theDoorPhrase: String = "",
    val toastEnabled: Boolean = false,
    val toastMessage: String = ""
)
