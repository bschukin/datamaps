package com.bftcom.ice.common.utils

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

open class Date : Comparable<Date> {

    protected val calendar: Calendar

    constructor() {
        calendar = Calendar.getInstance()
    }

    constructor(date: java.util.Date) {
        calendar = Calendar.getInstance().apply {
            time = date
        }
    }

    constructor(year: Int, month: Int, day: Int) {
        calendar = GregorianCalendar(year, month-1, day)
    }

    constructor(milliseconds: Number) : this(java.util.Date(milliseconds.toLong()))

    val date: java.util.Date
        get() = calendar.time

    @JsonIgnore
     fun getDate() = calendar[Calendar.DAY_OF_MONTH]

    @JsonIgnore
     fun getMonth() = calendar[Calendar.MONTH]

    @JsonIgnore
     fun getFullYear() = calendar[Calendar.YEAR]

     fun minusDays(days:Int):Date{
        val cal: Calendar = calendar.clone() as Calendar
        cal.add(Calendar.DATE, -days)
        return Date(cal.getTime())
    }
     fun plusDays(days:Int):Date{
        val cal: Calendar = calendar.clone() as Calendar
        cal.add(Calendar.DATE, days)
        return Date(cal.getTime())
    }

     fun plusSeconds(seconds:Int):Date{
        val cal: Calendar = calendar.clone() as Calendar
        cal.add(Calendar.SECOND, seconds)
        return Date(cal.time)
    }


    @JsonProperty(COMMON_DATE_FIELD)
     open fun getTime(): Number = calendar.timeInMillis

     open fun toCommonDateString() = apiDateFormat.format(date)!!

     open fun toReadableString() = toReadableDateString()

    override fun equals(other: Any?): Boolean = other is Date && other.calendar.time == calendar.time

    override fun toString(): String {
        return "'${apiDateFormat.format(date)}'"
    }

    override fun compareTo(other: Date): Int {
        return this.getDate().compareTo(other.getDate())
    }
}

open class Timestamp : Date {

    constructor() : super()

    constructor(date: java.util.Date) : super(date)

    constructor(milliseconds: Number) : super(milliseconds)

    @JsonIgnore
     fun getHours() = calendar[Calendar.HOUR_OF_DAY]

    @JsonIgnore
     fun getMinutes() = calendar[Calendar.MINUTE]

    @JsonIgnore
     fun getSeconds() = calendar[Calendar.SECOND]

    @JsonProperty(COMMON_TIMESTAMP_FIELD)
     override fun getTime(): Number = calendar.timeInMillis

     override fun toCommonDateString() = apiDateFormat.format(date)!!

     override fun toReadableString() = toReadableTimeStampString()

}




const val COMMON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

const val ISO_DATE_FORMAT = "yyyy-MM-dd"



const val COMMON_DATE_FIELD = "__dateMillis"

const val COMMON_TIMESTAMP_FIELD = "__timestampMillis"

 operator fun Date.compareTo(otherDate: Date): Int = date.compareTo(otherDate.date)

 fun parseDate(dateString: String): Date = Date(apiDateFormat.parse(dateString))

fun parseTimeStamp(dateString: String): Timestamp = Timestamp(apiDateFormat.parse(dateString))

fun String.toDate() = parseDate(this)

fun String.toTimeStamp() = parseTimeStamp(this)

fun Timestamp.toReadableTimeStampString(): String {
    return when {
        this == null -> ""
        this !is Date -> toString()
        this.getTime() == infinity__.getTime() -> "∞"
        this.getTime() == __infinity.getTime() -> "-∞"
        else -> "${toReadableDateString()} ${toReadableTimeString()}"
    }
}

fun Date.toReadableDateString(): String {
    return when {
        this == null -> ""
        this !is Date -> toString()
        this.getTime() == infinity__.getTime() -> "∞"
        this.getTime() == __infinity.getTime() -> "-∞"
        else -> "${zeroPadding(this.getDate())}.${zeroPadding(this.getMonth() + 1)}.${zeroPadding(this.getFullYear(),4)}"
    }
}

fun Timestamp.toReadableTimeString(): String {
    return "${zeroPadding(this.getHours())}:${zeroPadding(this.getMinutes())}:${zeroPadding(this.getSeconds())}"
}

inline fun zeroPadding(int: Number, length: Int = 2) = int.toString().padStart(length, '0')


val __infinity= Date(-9223372036832400000) //значение постгресса 9.5
//плюс бесконечность
val infinity__= Date(9223372036825200000) //значение постгресса 9.5