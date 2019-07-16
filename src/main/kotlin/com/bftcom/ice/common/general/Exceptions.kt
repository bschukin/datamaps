package com.bftcom.ice.common.general



/****
 * Ошибка, выкидываюмая на клиенте при всех ошибках взаимододействия с сервером.
 * Содержит полную информацию (включая сообщения, цепочку cause, стектрейс)
 * об ошибке возникшей на сервере
 */
open class ServerException(val causeInfo: ExceptionInfo)
    : RuntimeException(causeInfo.message)
{
    override fun toString(): String {
        return causeInfo.toString()
    }
}

data class ExceptionInfo(val cause: ExceptionInfo?,
                         val clazz: String = "",
                         val message: String = "",
                         val stackTrace: String = "") {

    override fun toString(): String {
        val c = if (cause == null) "" else ", \n\t\tcause={ $cause }"
        return "Exception(clazz='$clazz', message=$message $c )"
    }
}

/****
 * Базовая ошибка для ошибок уровня прикладного приложения
 */
open class AppException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


/****
 * Ошибка валидации
 */

class ValidationException(vararg val errors: ValidationError) : AppException("Ошибка валидации") {

    constructor(errors: Collection<ValidationError>) : this(*errors.toTypedArray())

}

data class ValidationError(
        val fieldName: String? = null,
        val fieldCaption: String? = null,
        val message: String
) {
    override fun toString(): String {
        return if (fieldName == null) message else """${fieldCaption ?: fieldName}: $message"""
    }
}


/*******************************************************/
/*************EXCEPTIONS FOR TYPICAL CASES ************/
/******************************************************/

class NotImplementedYetException : RuntimeException {
    constructor() : super("Not implemented yet")
    constructor(message: String) : super(message)
}
typealias NIY = NotImplementedYetException
fun throwNotImplementedYet(message: String? = null): Nothing = if (message == null) throw NIY() else throw  NIY(message)


class SomethingNotFound : RuntimeException {

    constructor() : super("Something expected in the context was not found")
    constructor(message: String) : super(message)
}
typealias SNF = SomethingNotFound

fun throwNotFound(message: String? = null): Nothing = if (message == null) throw SNF() else throw  SNF(message)


class ImpossibleSituation : RuntimeException {
    constructor() : super("A situation that was not ever expected just happens")
    constructor(message: String?) : super(message) {}
}

typealias IS = ImpossibleSituation

fun makeSure(condition: Boolean, message: String? = null) {
    if (!condition)
        throw ImpossibleSituation(message)
}

fun <T> ensureNotNull(obj: T?, message: String? = null):T {
    if(obj==null)
        throw NullPointerException(message)
    return obj
}

fun throwImpossible(message: String? = null): Nothing = if (message == null) throw IS() else throw  IS(message)

