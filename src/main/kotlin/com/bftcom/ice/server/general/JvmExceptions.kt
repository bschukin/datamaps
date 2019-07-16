package com.bftcom.ice.common.general

import org.apache.commons.lang3.exception.ExceptionUtils


open class DbException : RuntimeException {
    constructor() : super("A database query error")
    constructor(t: Throwable) : super("A database query error", t)
    constructor(message: String, t: Throwable? = null) : super(message, t)
}


class DbUniqueConstraintException(message:String,
                                  cause: RuntimeException):  DbException(message, cause)

internal fun throwDbUniqueConstraintException(table:String?, field:String?, value:String?, cause: RuntimeException) {
    val tablemsg = if(value.isNullOrBlank()) "" else ". Таблица: [$table]"
    val valuemsg = if(value.isNullOrBlank()) "" else ". Значение уже существует: [$value]"
    val fieldmsg = if(field.isNullOrBlank()) "" else ". Поле: [$field]"
    val msg = "Нарушено условие уникальности поля$tablemsg$fieldmsg$valuemsg"
    throw DbUniqueConstraintException(msg, cause)
}

class DbForeignConstraintException(message:String,
                                  cause: RuntimeException):  DbException(message, cause)

internal fun throwDbForeignConstraintException(tableFrom:String?, tableTo:String?,
                                              referencedToField:String?,
                                              referencedFromField:String?,
                                              key:String?, cause: RuntimeException) {

    val msg = "Операция обновления в таблице [$tableFrom] нарушает целостность. " +
            "\r\n На запись с ключом [$referencedToField]=($key) есть ссылки из таблицы [$tableTo] (поле [$referencedFromField])"

    throw DbForeignConstraintException(msg, cause)

}

open class DbRecordNotFound(val entity: String, val id: Any?) : DbException("An object was not found")


fun Throwable.toExceptionInfo(astackTrace: String? = null): ExceptionInfo {
    return ExceptionInfo(
            cause = (if (this.cause != null) cause!!.toExceptionInfo("") else null),
            clazz = this::class.qualifiedName.orEmpty(),
            message = this.message.orEmpty(),
            stackTrace = astackTrace ?: ExceptionUtils.getStackTrace(this))
}

