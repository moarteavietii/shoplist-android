package eu.pascariu.shoppinglist.domain.order

const val BASE_62_DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

private val digitIndex: Map<Char, Int> = BASE_62_DIGITS.withIndex().associate { (index, digit) -> digit to index }

private const val ZERO = '0'

private fun midpoint(a: String, b: String?): String {
    require(b == null || a < b) { "$a >= $b" }
    require(!a.endsWith(ZERO) && (b == null || !b.endsWith(ZERO))) { "trailing zero" }
    if (b != null) {
        var n = 0
        while ((a.getOrNull(n) ?: ZERO) == b[n]) {
            n++
        }
        if (n > 0) {
            return b.take(n) + midpoint(a.drop(n), b.drop(n))
        }
    }
    val digitA = if (a.isEmpty()) 0 else digitIndex.getValue(a[0])
    val digitB = b?.let { digitIndex.getValue(it[0]) } ?: BASE_62_DIGITS.length
    if (digitB - digitA > 1) {
        val midDigit = Math.round(0.5 * (digitA + digitB)).toInt()
        return BASE_62_DIGITS[midDigit].toString()
    }
    if (b != null && b.length > 1) {
        return b.take(1)
    }
    return BASE_62_DIGITS[digitA].toString() + midpoint(a.drop(1), null)
}

private fun getIntegerLength(head: Char): Int =
    when (head) {
        in 'a'..'z' -> head - 'a' + 2
        in 'A'..'Z' -> 'Z' - head + 2
        else -> throw IllegalArgumentException("invalid order key head: $head")
    }

private fun getIntegerPart(key: String): String {
    val integerPartLength = getIntegerLength(key[0])
    if (integerPartLength > key.length) {
        throw IllegalArgumentException("invalid order key: $key")
    }
    return key.take(integerPartLength)
}

private fun isSmallestInteger(key: String): Boolean = key == "A" + ZERO.toString().repeat(26)

private fun validateOrderKey(key: String) {
    if (isSmallestInteger(key)) {
        throw IllegalArgumentException("invalid order key: $key")
    }
    val integerPart = getIntegerPart(key)
    val fractionalPart = key.drop(integerPart.length)
    if (fractionalPart.endsWith(ZERO)) {
        throw IllegalArgumentException("invalid order key: $key")
    }
}

private fun validateInteger(integerPart: String) {
    if (integerPart.length != getIntegerLength(integerPart[0])) {
        throw IllegalArgumentException("invalid integer part of order key: $integerPart")
    }
}

private fun incrementInteger(integerPart: String): String? {
    validateInteger(integerPart)
    val head = integerPart[0]
    var trailing = ""
    for (i in integerPart.length - 1 downTo 1) {
        val digit = digitIndex.getValue(integerPart[i]) + 1
        if (digit == BASE_62_DIGITS.length) {
            trailing = ZERO + trailing
        } else {
            return head + integerPart.substring(1, i) + BASE_62_DIGITS[digit] + trailing
        }
    }
    if (head == 'Z') {
        return "a$ZERO"
    }
    if (head == 'z') {
        return null
    }
    val nextHead = head + 1
    return nextHead.toString() + if (nextHead > 'a') trailing + ZERO else trailing.drop(1)
}

private fun decrementInteger(integerPart: String): String? {
    validateInteger(integerPart)
    val head = integerPart[0]
    val last = BASE_62_DIGITS.last()
    var trailing = ""
    for (i in integerPart.length - 1 downTo 1) {
        val digit = digitIndex.getValue(integerPart[i]) - 1
        if (digit == -1) {
            trailing = last + trailing
        } else {
            return head + integerPart.substring(1, i) + BASE_62_DIGITS[digit] + trailing
        }
    }
    if (head == 'a') {
        return "Z$last"
    }
    if (head == 'A') {
        return null
    }
    val previousHead = head - 1
    return previousHead.toString() + if (previousHead < 'Z') trailing + last else trailing.drop(1)
}

fun generateKeyBetween(a: String?, b: String?): String {
    var lower = a
    var upper = b
    if (lower != null) {
        validateOrderKey(lower)
    }
    if (upper != null) {
        validateOrderKey(upper)
    }
    if (lower != null && upper != null && lower > upper) {
        val temp = lower
        lower = upper
        upper = temp
    }

    if (lower == null) {
        if (upper == null) {
            return "a$ZERO"
        }
        val ib = getIntegerPart(upper)
        val fb = upper.drop(ib.length)
        if (isSmallestInteger(ib)) {
            return ib + midpoint("", fb)
        }
        if (ib < upper) {
            return ib
        }
        return decrementInteger(ib) ?: throw IllegalStateException("cannot decrement any more")
    }

    if (upper == null) {
        val ia = getIntegerPart(lower)
        val fa = lower.drop(ia.length)
        val incremented = incrementInteger(ia)
        return incremented ?: (ia + midpoint(fa, null))
    }

    val ia = getIntegerPart(lower)
    val fa = lower.drop(ia.length)
    val ib = getIntegerPart(upper)
    val fb = upper.drop(ib.length)
    if (ia == ib) {
        return ia + midpoint(fa, fb)
    }
    val incremented = incrementInteger(ia) ?: throw IllegalStateException("cannot increment any more")
    if (incremented < upper) {
        return incremented
    }
    return ia + midpoint(fa, null)
}
