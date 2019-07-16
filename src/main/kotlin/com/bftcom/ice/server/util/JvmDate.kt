package com.bftcom.ice.common.utils

import java.text.SimpleDateFormat
import java.util.*





val apiDateFormat = SimpleDateFormat(COMMON_DATE_FORMAT, Locale.US)
val izoDateFormat = SimpleDateFormat(ISO_DATE_FORMAT, Locale.US)

fun Date.toIsoDateString() = izoDateFormat.format(this.date)!!


