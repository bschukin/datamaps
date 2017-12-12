package com.datamaps.services

import com.datamaps.general.throwNIS
import com.datamaps.general.validate
import java.sql.Connection


/**
 * Created by b.schukin on 23.11.2017.
 */
interface DbDialect
{
    fun getDbIdentifier(name:String):String = name

    fun getCurrentScheme():String = "public"

    fun getLimitOffsetQueryInSelect(limit:Int?, offset:Int?):String = ""
    fun getLimitOffsetQueryInWhere(limit:Int?, offset:Int?):String = ""

    fun getQuotedDbIdentifier(id:String?):String =  "\"$id\""
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

    override fun getLimitOffsetQueryInSelect(limit:Int?, offset:Int?): String {
        var res=""
        limit?.let {
            res+= "LIMIT $limit"
        }
        offset?.let {
            validate(limit!=null)
            res+= " $offset"
        }
        return res+" "
    }
}

class PostgresqlDialect : DbDialect
{
    override fun getDbIdentifier(name: String): String  = name.toLowerCase()


    override fun getLimitOffsetQueryInWhere(limit:Int?, offset:Int?): String {
        var res=""
        limit?.let {
            res+= " LIMIT $limit"
        }
        offset?.let {
            res+= " OFFSET $offset"
        }
        return res+" "
    }
}