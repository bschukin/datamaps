package com.datamaps.services

import com.datamaps.general.throwNIS
import java.sql.Connection


/**
 * Created by b.schukin on 23.11.2017.
 */
interface DbDialect
{
    fun getDbIdentifier(name:String):String = name

    fun getCurrentScheme():String = "public"
}


fun getDbDialectByConnection(c: Connection):DbDialect
{
    return when(c.metaData.databaseProductName.toLowerCase())
    {
        "PostgreSQL".toLowerCase()->PostgresqlDialect()
        "HSQL Database Engine".toLowerCase()->HsqldbDialect()
        else -> throwNIS("unknown dialect: ${c.metaData.databaseProductName}")
    }
}


class HsqldbDialect : DbDialect
{
    override fun getDbIdentifier(name: String): String  = name.toUpperCase()

    override fun getCurrentScheme():String = "public".toUpperCase()
}

class PostgresqlDialect : DbDialect
{
    override fun getDbIdentifier(name: String): String  = name.toLowerCase()
}