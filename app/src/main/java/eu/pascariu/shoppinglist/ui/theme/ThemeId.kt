package eu.pascariu.shoppinglist.ui.theme

enum class ThemeId {
    DEFAULT,
    PASTEL,
    MINT,
    LAVENDER,
    DARK_SLATE,
    OCEAN,
    SUNSET,
    HIGH_CONTRAST,
    CHRISTMAS;

    val displayName: String
        get() = when (this) {
            DEFAULT -> "Default"
            PASTEL -> "Pastel"
            MINT -> "Mint"
            LAVENDER -> "Lavender"
            DARK_SLATE -> "Dark Slate"
            OCEAN -> "Ocean"
            SUNSET -> "Sunset"
            HIGH_CONTRAST -> "High Contrast"
            CHRISTMAS -> "Christmas"
        }

    companion object {
        fun fromName(name: String): ThemeId? = entries.firstOrNull { it.name == name }
    }
}
