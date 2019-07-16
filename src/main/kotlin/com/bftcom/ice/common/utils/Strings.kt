fun String.removePrefix(prefix: CharSequence, ignoreCase: Boolean): String {
    if (startsWith(prefix, ignoreCase)) {
        return substring(prefix.length)
    }
    return this
}


fun String.removeSuffix(suffix: CharSequence, ignoreCase: Boolean): String {
    if (endsWith(suffix, ignoreCase)) {
        return substring(0, length - suffix.length)
    }
    return this
}