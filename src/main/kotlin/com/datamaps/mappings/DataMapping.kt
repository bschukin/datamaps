package com.datamaps.mappings

import com.datamaps.general.NIY
import com.datamaps.general.SNF
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.sql.JDBCType

/**
 * Created by Щукин on 28.10.2017.
 *
 * Маппинг
 *
 */

class DataMapping(var name: String, var table: String) {

    @SerializedName("scan-fields-in-db")
    var scanFieldsInDb = false;

    @SerializedName("id-column")
    var idColumn: String? = ID;
    var fields = linkedMapOf<String, DataField>()

    var groups: MutableMap<String, DataGroup> = linkedMapOf<String, DataGroup>()

    init {
        groups[DEFAULT] = DataGroup(DEFAULT)
    }

    val defaultGroup: DataGroup
        get() = groups[DEFAULT]!!


    fun add(field: DataField) = fields.put(field.name.toLowerCase(), field)

    operator fun get(field: String): DataField {
        return fields.computeIfAbsent(field.toLowerCase(),
                { t -> throw SNF("field '${field}' of '${name}' entity not found") })
    }

}

const val ID: String = "ID";
const val DEFAULT: String = "DEFAULT";


class DataField(var name: String) {

    var type: FieldType? = null
    var javaType: Class<Any>? = null
    lateinit var sqlcolumn: String

    @SerializedName("m-1")
    var manyToOne: ManyToOne? = null;

    @SerializedName("1-m")
    var oneToMany: OneToMany? = null;

    @SerializedName("m-m")
    var manyToMany: ManyToMany? = null;

    val isSimple: Boolean
        get() = manyToOne==null && oneToMany==null && manyToMany==null
}


class DataGroup(var name: String) {

    var fields = mutableListOf<String>()

    fun add(field:String)
    {
        fields. add(field)
    }
}

class ManyToOne(var to: String,
                @SerializedName("join-column") var joinColumn: String) {

}

class OneToMany(var to: String,
                @SerializedName("their-join-column") var theirJoinColumn: String) {

    var lazy:Boolean=true;

}

class ManyToMany(var to: String,
                 @SerializedName("join-table") var joinTable: String,
                 @SerializedName("our-join-column") var thisJoinColumn: String,
                 @SerializedName("their-join-column") var theirJoinColumn: String) {

}

enum class FieldType {
    long,
    string,
    list,
    set,
    map
}

//https://db.apache.org/ojb/docu/guides/jdbc-types.html
fun <T:Any> getJavaTypeByJDBCType(jdbcType: JDBCType):Class<T>
{
    return when(jdbcType)
    {
        JDBCType.CHAR, JDBCType.VARCHAR, JDBCType.LONGNVARCHAR -> String::class.java
        JDBCType.NUMERIC, JDBCType.DECIMAL -> BigDecimal::class.java
        JDBCType.INTEGER -> Int::class.java
        JDBCType.BIT, JDBCType.BOOLEAN -> Boolean::class.java
        JDBCType.TINYINT -> Byte::class.java
        JDBCType.SMALLINT -> Short::class.java
        JDBCType.BIGINT -> Long::class.java
        JDBCType.REAL -> Float::class.java
        JDBCType.FLOAT -> Double::class.java
        JDBCType.DOUBLE -> Double::class.java
        JDBCType.BINARY, JDBCType.VARBINARY, JDBCType.LONGVARBINARY -> throw NIY()
        JDBCType.DATE    -> java.sql.Date::javaClass
        JDBCType.TIME  -> java.sql.Time::javaClass
        JDBCType.TIMESTAMP  -> java.sql.Timestamp::javaClass
        JDBCType.CLOB, JDBCType.BLOB  -> throw NIY()
        JDBCType.ARRAY, JDBCType.DISTINCT, JDBCType.DATALINK,JDBCType.STRUCT, JDBCType.REF  -> throw NIY()
        else-> throw NIY()
    } as Class<T>

}