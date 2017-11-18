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


//Something not found
class SNF : RuntimeException {
    constructor() {}

    constructor(message: String) : super(message) {}

    constructor(cause: Throwable) : super(cause) {}

}

//Not impossible situation
class NIS : RuntimeException {
    constructor() {}

    constructor(message: String) : super(message) {}

    constructor(cause: Throwable) : super(cause) {}

}

public fun throwNIS(): Nothing = throw NIS()
