package dev.ipf.whitenoise.model

import java.util.Locale

enum class EmojiCategory(
    val id: String,
    val title: String,
    val searchTerms: String,
) {
    Recent("recent", "Recently Used", "recent frequently used history"),
    SmileysAndPeople("smileys", "Smileys & People", "face smile people emotion hand body"),
    AnimalsAndNature("animals", "Animals & Nature", "animal nature plant weather pet"),
    FoodAndDrink("food", "Food & Drink", "food drink fruit meal restaurant"),
    Activities("activities", "Activities", "activity sport game music art celebration"),
    TravelAndPlaces("travel", "Travel & Places", "travel place transport building space"),
    Objects("objects", "Objects", "object tool technology clothing household"),
    Symbols("symbols", "Symbols", "symbol sign heart arrow number"),
    Flags("flags", "Flags", "flag country nation"),
}

data class EmojiSection(
    val category: EmojiCategory,
    val emoji: List<String>,
)

/**
 * Deterministic, offline emoji data for reaction picking.
 *
 * The category order and accepted product set come from the pinned White Noise iOS baseline. The
 * Android presentation is independent Compose code and uses the device's native color emoji font.
 */
object EmojiCatalog {
    private val aliases = mapOf(
        "❤" to "heart love red",
        "❤️" to "heart love red",
        "🤘" to "horns rock hand",
        "🔥" to "fire flame hot",
        "😂" to "laugh tears joy",
        "🦫" to "beaver animal",
        "🚀" to "rocket launch space",
        "👍" to "thumbs up yes like",
        "👎" to "thumbs down no dislike",
        "😭" to "cry sob tears",
        "🎉" to "party celebrate",
        "💯" to "hundred perfect",
        "🙏" to "please thanks pray",
        "👀" to "eyes look",
        "✅" to "check done yes",
        "❌" to "cross no",
        "😀" to "grin happy smile",
        "😃" to "happy smile",
        "😄" to "happy smile",
        "😁" to "grin smile",
        "🤣" to "rolling laugh tears",
        "😊" to "smile blush happy",
        "😍" to "love heart eyes",
        "🥰" to "love hearts smile",
        "🤔" to "think thinking",
        "😴" to "sleep sleeping tired",
        "🤮" to "sick vomit",
        "👏" to "clap applause",
        "👋" to "wave hello goodbye",
        "🤝" to "handshake agreement",
        "💪" to "strong muscle",
        "🐶" to "dog puppy pet",
        "🐱" to "cat kitten pet",
        "🍕" to "pizza food",
        "☕️" to "coffee hot drink",
        "⚽️" to "football soccer sport",
        "🎮" to "game controller videogame",
        "✈️" to "airplane plane flight travel",
        "💡" to "light bulb idea",
        "⚠️" to "warning alert",
    )

    val sections: List<EmojiSection> = listOf(
        section(
            EmojiCategory.Recent,
            "😂😭🎉👀🤦🤭👍💀🫂💯🏃⚫🍴🤮😢🆗🤘🤨🛏🦫🥶😌🚀💋🙅😅",
        ),
        section(
            EmojiCategory.SmileysAndPeople,
            "😀😃😄😁😆😅😂🤣🥲😊😇🙂🙃😉😌😍🥰😘😗😙😚😋😛😝😜🤪🤨🧐🤓😎🥸🤩🥳🙂‍↕️😏😒🙂‍↔️😞😔😟😕🙁☹️😣😖😫😩🥺😢😭😤😠😡🤬🤯😳🥵🥶😶‍🌫️😱😨😰😥😓🤗🤔🫣🤭🫢🫡🤫🫠🤥😶🫥😐🫤😑🫨😬🙄😯😦😧😮😲🥱😴🤤😪😮‍💨😵😵‍💫🤐🥴🤢🤮🤧😷🤒🤕🤑🤠😈👿👹👺🤡💩👻💀☠️👽👾🤖🎃😺😸😹😻😼😽🙀😿😾👋🤚🖐️✋🖖🫱🫲🫳🫴👌🤌🤏✌️🤞🫰🤟🤘🤙👈👉👆👇☝️🫵👍👎✊👊🤛🤜👏🙌🫶👐🤲🤝🙏✍️💅🤳💪🦾🦿🦵🦶👂👃🧠🫀🫁🦷🦴👀👁️👅👄🫦💋",
        ),
        section(
            EmojiCategory.AnimalsAndNature,
            "🐶🐱🐭🐹🐰🦊🐻🐼🐻‍❄️🐨🐯🦁🐮🐷🐽🐸🐵🙈🙉🙊🐒🐔🐧🐦🐤🐣🐥🦆🦅🦉🦇🐺🐗🐴🦄🐝🪱🐛🦋🐌🐞🐜🪰🪲🪳🦟🦗🕷️🕸️🦂🐢🐍🦎🦖🦕🐙🦑🪼🦐🦞🦀🐡🐠🐟🐬🐳🐋🦈🦭🐊🐅🐆🦓🫏🦍🦧🦣🐘🦛🦏🐪🐫🦒🦘🦬🐃🐂🐄🫎🐎🐖🐏🐑🦙🐐🦌🦫🦥🐕🐩🦮🐕‍🦺🐈🐈‍⬛🪽🪶🐓🦃🦤🦚🦜🦢🪿🦩🕊️🐇🦝🦨🦡🦦🦫🐁🐀🐿️🦔🐉🐲🌵🎄🌲🌳🌴🪹🪺🪵🌱🌿☘️🍀🎍🪴🎋🍃🍂🍁🍄🐚🪨🌾💐🌷🌹🥀🌺🌸🪷🌼🌻🌞🌝🌛🌜🌚🌕🌖🌗🌘🌑🌒🌓🌔🌙🌎🌍🌏🪐💫⭐️🌟✨⚡️☄️💥🔥🌪️🌈☀️🌤️⛅️🌥️☁️🌦️🌧️⛈️🌩️🌨️❄️☃️⛄️🌬️💨💧💦🫧☔️☂️🌊",
        ),
        section(
            EmojiCategory.FoodAndDrink,
            "🍏🍎🍐🍊🍋🍋‍🟩🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍆🥑🫛🥦🥬🥒🌶️🫑🌽🥕🫒🧄🧅🫚🥔🍠🫘🥐🥯🍞🥖🥨🧀🥚🍳🧈🥞🧇🥓🥩🍗🍖🦴🌭🍔🍟🍕🫓🥪🥙🧆🌮🌯🫔🥗🥘🫕🥫🍝🍜🍲🍛🍣🍱🥟🦪🍤🍙🍚🍘🍥🥠🥮🍢🍡🍧🍨🍦🥧🧁🍰🎂🍮🍭🍬🍫🍿🍩🍪🌰🥜🍯🥛🍼🫖☕️🍵🧃🥤🧋🍶🍺🍻🥂🍷🫗🥃🍸🍹🧉🍾🧊🥄🍴🍽️🥣🥡🥢🧂",
        ),
        section(
            EmojiCategory.Activities,
            "⚽️🏀🏈⚾️🥎🎾🏐🏉🥏🎱🪀🏓🏸🏒🏑🥍🏏🪃🥅⛳️🪁🏹🎣🤿🥊🥋🎽🛹🛼🛷⛸️🥌🎿⛷️🏂🪂🏋️🤼🤸⛹️🤺🤾🏌️🏇🧘🏄🏊🤽🚣🧗🚵🚴🏆🥇🥈🥉🏅🎖️🏵️🎗️🎫🎟️🎪🤹🎭🩰🎨🎬🎤🎧🎼🎹🥁🪘🎷🎺🪗🎸🪕🎻🪈🎲♟️🎯🎳🎮🎰🧩🎉🎊🎈🎁🪄🪅",
        ),
        section(
            EmojiCategory.TravelAndPlaces,
            "🚗🚕🚙🚌🚎🏎️🚓🚑🚒🚐🛻🚚🚛🚜🦯🦽🦼🛴🚲🛵🏍️🛺🚨🚔🚍🚘🚖🛞🚡🚠🚟🚃🚋🚞🚝🚄🚅🚈🚂🚆🚇🚊🚉✈️🛫🛬🛩️💺🛰️🚀🛸🚁🛶⛵️🚤🛥️🛳️⛴️🚢⚓️🛟⛽️🚧🚦🚥🗺️🗿🗽🗼🏰🏯🏟️🎡🎢🛝🎠⛲️⛱️🏖️🏝️🏜️🌋⛰️🏔️🗻🏕️⛺️🛖🏠🏡🏘️🏚️🏗️🏭🏢🏬🏣🏤🏥🏦🏨🏪🏫🏩💒🏛️⛪️🕌🕍🛕🕋⛩️🛤️🛣️🗾🎑🏞️🌅🌄🌠🎇🎆🌇🌆🏙️🌃🌌🌉🌁",
        ),
        section(
            EmojiCategory.Objects,
            "⌚️📱📲💻⌨️🖥️🖨️🖱️🖲️🕹️🗜️💽💾💿📀📼📷📸📹🎥📽️🎞️📞☎️📟📠📺📻🎙️🎚️🎛️🧭⏱️⏲️⏰🕰️⌛️⏳📡🔋🪫🔌💡🔦🕯️🪔🧯🛢️💸💵💴💶💷🪙💰💳💎⚖️🪜🧰🪛🔧🔨⚒️🛠️⛏️🪚🔩⚙️🪤🧱⛓️⛓️‍💥🧲🔫💣🧨🪓🔪🗡️⚔️🛡️🚬⚰️🪦⚱️🏺🔮📿🧿🪬💈⚗️🔭🔬🕳️🩹🩺🩻🩼💊💉🩸🧬🦠🧫🧪🌡️🧹🪠🧺🧻🚽🚰🚿🛁🛀🧼🪥🪒🧽🪣🧴🛎️🔑🗝️🚪🪑🛋️🛏️🛌🧸🪆🖼️🪞🪟🛍️🛒🎁🎈🎏🎀🪄🪩🎎🏮🎐🧧✉️📩📨📧💌📥📤📦🏷️🪧📪📫📬📭📮📯📜📃📄📑🧾📊📈📉🗒️🗓️📆📅🗑️📇🗃️🗳️🗄️📋📁📂🗂️🗞️📰📓📔📒📕📗📘📙📚📖🔖🧷🔗📎🖇️📐📏🧮📌📍✂️🖊️🖋️✒️🖌️🖍️📝✏️🔍🔎🔏🔐🔒🔓",
        ),
        section(
            EmojiCategory.Symbols,
            "❤️🧡💛💚💙🩵💜🖤🩶🤍🤎💔❤️‍🔥❤️‍🩹❣️💕💞💓💗💖💘💝💟☮️✝️☪️🕉️☸️✡️🔯🕎☯️☦️🛐⛎♈️♉️♊️♋️♌️♍️♎️♏️♐️♑️♒️♓️🆔⚛️🉑☢️☣️📴📳🈶🈚️🈸🈺🈷️✴️🆚💮🉐㊙️㊗️🈴🈵🈹🈲🅰️🅱️🆎🆑🅾️🆘❌⭕️🛑⛔️📛🚫💯💢♨️🚷🚯🚳🚱🔞📵🚭❗️❕❓❔‼️⁉️🔅🔆〽️⚠️🚸🔱⚜️🔰♻️✅🈯️💹❇️✳️❎🌐💠Ⓜ️🌀💤🏧🚾♿️🅿️🛗🈳🈂️🛂🛃🛄🛅🚹🚺🚼⚧️🚻🚮🎦📶🈁🔣ℹ️🔤🔡🔠🆖🆗🆙🆒🆕🆓0️⃣1️⃣2️⃣3️⃣4️⃣5️⃣6️⃣7️⃣8️⃣9️⃣🔟🔢#️⃣*️⃣⏏️▶️⏸️⏯️⏹️⏺️⏭️⏮️⏩⏪⏫⏬◀️🔼🔽➡️⬅️⬆️⬇️↗️↘️↙️↖️↕️↔️↪️↩️⤴️⤵️🔀🔁🔂🔄🔃🎵🎶➕➖➗✖️🟰♾️💲💱™️©️®️〰️➰➿🔚🔙🔛🔝🔜✔️☑️🔘🔴🟠🟡🟢🔵🟣⚫️⚪️🟤🔺🔻🔸🔹🔶🔷🔳🔲▪️▫️◾️◽️◼️◻️🟥🟧🟨🟩🟦🟪⬛️⬜️🟫",
        ),
        section(
            EmojiCategory.Flags,
            "🏁🚩🎌🏴🏳️🏳️‍🌈🏳️‍⚧️🏴‍☠️🇺🇳🇦🇺🇦🇹🇧🇪🇧🇷🇨🇦🇨🇳🇭🇷🇨🇾🇨🇿🇩🇰🇪🇪🇫🇮🇫🇷🇩🇪🇬🇷🇭🇺🇮🇸🇮🇳🇮🇩🇮🇪🇮🇱🇮🇹🇯🇵🇰🇷🇱🇻🇱🇹🇱🇺🇲🇹🇲🇽🇲🇪🇳🇱🇳🇿🇲🇰🇳🇴🇵🇱🇵🇹🇷🇴🇷🇸🇸🇬🇸🇰🇸🇮🇿🇦🇪🇸🇸🇪🇨🇭🇹🇷🇺🇦🇬🇧🇺🇸🇻🇦",
        ),
    )

    val all: List<String> = sections.flatMap(EmojiSection::emoji).distinct()

    fun search(query: String): List<EmojiSection> {
        val normalized = query.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) return sections
        return sections.mapNotNull { section ->
            val matchesSection = section.category.title.lowercase(Locale.ROOT).contains(normalized) ||
                section.category.searchTerms.contains(normalized)
            val matches = if (matchesSection) {
                section.emoji
            } else {
                section.emoji.filter { emoji ->
                    emoji.contains(normalized) || aliases[emoji]?.contains(normalized) == true
                }
            }
            matches.takeIf(List<String>::isNotEmpty)?.let { section.copy(emoji = it) }
        }
    }

    private fun section(category: EmojiCategory, source: String): EmojiSection =
        EmojiSection(category, splitEmojiGraphemes(source).distinct())
}

/** Splits the catalog into extended emoji clusters without requiring an API-24 ICU dependency. */
internal fun splitEmojiGraphemes(source: String): List<String> {
    val codePoints = buildList {
        var offset = 0
        while (offset < source.length) {
            val codePoint = Character.codePointAt(source, offset)
            add(codePoint)
            offset += Character.charCount(codePoint)
        }
    }
    val result = mutableListOf<String>()
    var index = 0
    while (index < codePoints.size) {
        val cluster = StringBuilder().appendCodePoint(codePoints[index])
        val startsWithRegionalIndicator = codePoints[index] in 0x1F1E6..0x1F1FF
        index += 1
        if (startsWithRegionalIndicator && index < codePoints.size && codePoints[index] in 0x1F1E6..0x1F1FF) {
            cluster.appendCodePoint(codePoints[index])
            index += 1
        }
        while (index < codePoints.size) {
            val codePoint = codePoints[index]
            when {
                codePoint == 0x200D -> {
                    cluster.appendCodePoint(codePoint)
                    index += 1
                    if (index < codePoints.size) {
                        cluster.appendCodePoint(codePoints[index])
                        index += 1
                    }
                }
                codePoint == 0xFE0E || codePoint == 0xFE0F ||
                    codePoint == 0x20E3 || codePoint in 0x1F3FB..0x1F3FF ||
                    codePoint in 0xE0020..0xE007F || codePoint.isCombiningMark() -> {
                    cluster.appendCodePoint(codePoint)
                    index += 1
                }
                else -> break
            }
        }
        result += cluster.toString()
    }
    return result
}

private fun Int.isCombiningMark(): Boolean = when (Character.getType(this)) {
    Character.COMBINING_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt(),
    Character.NON_SPACING_MARK.toInt(),
    -> true
    else -> false
}
