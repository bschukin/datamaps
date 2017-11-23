package com.datamaps.general

/**
 * Created by b.schukin on 07.11.2017.
 */

//Not Implemented Yet Exception
class NIY : RuntimeException {
    constructor() {}

    constructor(message: String) : super(message) {}

    constructor(cause: Throwable) : super(cause) {}

}

fun validateNIY(boolean: Boolean) {
    if (!boolean)
        throw NIY()
}


//Something not found
class SNF : RuntimeException {
    constructor() {}

    constructor(message: String) : super(message) {}

    constructor(cause: Throwable) : super(cause) {}

}

//Not impossible situation
class NIS : RuntimeException {
    constructor() {}

    constructor(message: String?) : super(message) {}

    constructor(cause: Throwable) : super(cause) {}

}

fun validate(condition: Boolean, message: String? = null) {
    if (!condition)
        throwNIS(message)
}

public fun throwNIS(message: String? = null): Nothing = throw NIS(message)
