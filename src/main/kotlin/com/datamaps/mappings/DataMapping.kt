package com.datamaps.mappings

import com.datamaps.general.NIS
import com.datamaps.general.NIY
import com.datamaps.general.SNF
import com.datamaps.general.checkNIS
import com.datamaps.util.linkedCaseInsMapOf
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
    var scanFieldsInDb = false

    @SerializedName("id-column")
    var idColumn: String? = ID

    var idGenerationType = IdGenerationType.NONE

    //поля
    var fields = linkedCaseInsMapOf<DataField>()

    var groups: MutableMap<String, DataGroup> = linkedCaseInsMapOf<DataGroup>()

    init {
        groups[DEFAULT] = DataGroup(DEFAULT)
        groups[FULL] = DataGroup(FULL)
        groups[REFS] = DataGroup(REFS)
        groups[LIST] = DataGroup(LIST)
    }

    val defaultGroup: DataGroup
        get() = groups[DEFAULT]!!

    val fullGroup: DataGroup
        get() = groups[FULL]!!

    val refsGroup: DataGroup
        get() = groups[REFS]!!

    val lists: DataGroup
        get() = groups[LIST]!!


    fun add(field: DataField) = fields.merge(field.name, field, { _, _ -> throw NIS() })


    operator fun get(field: String): DataField {
        return fields.computeIfAbsent(field.toLowerCase(),
                { _ -> throw SNF("parentLinkField '$field' of '$name' entity not found") })
    }

    fun getBackReferenceFieldForThisList(parent:DataMapping, listProperty:String):DataField
    {
        val oneToMany = parent[listProperty]
        checkNIS(oneToMany.is1N)

        return fields.values.filter { it.isM1 }.find {
            it.referencedOneToAnother(oneToMany)
        }!!
    }

    override fun toString(): String {
        return "DataMapping(entity='$name', table='$table')"
    }


}

enum class IdGenerationType {
    NONE, //самостоятельно, епт
    IDENTITY, //identity-колонка
    SEQUENCE //из сиквенса с именем таблицы + _SEQ
}

const val ID: String = "ID"
const val DEFAULT: String = "DEFAULT"
const val REFS: String = "REFS"
const val FULL: String = "FULL"
const val LIST: String = "LIST"


class DataField(var name: String) {

    var javaType: Class<*>? = null
    var sqlcolumn: String? = null

    @SerializedName("m-1")
    var manyToOne: ManyToOne? = null

    @SerializedName("1-m")
    var oneToMany: OneToMany? = null

    @SerializedName("m-m")
    var manyToMany: ManyToMany? = null

    val isSimple: Boolean
        get() = manyToOne == null && oneToMany == null && manyToMany == null

    val isM1: Boolean
        get() = manyToOne != null

    val is1N: Boolean
        get() = oneToMany != null

    fun referenceTo() =
            when {
                isM1 -> manyToOne!!.to
                is1N -> oneToMany!!.to
                else -> null
            }

    fun thisSideJoinColumn()=
            when {
                isM1 -> sqlcolumn
                is1N -> "ID" //todo: сделать как следовает
                else -> null
            }

    fun thatSideJoinColumn()=
            when {
                isM1 -> "ID"//todo: сделать как следовает
                is1N -> oneToMany!!.theirJoinColumn
                else -> null
            }

    fun referencedOneToAnother(anotherField:DataField):Boolean
    {
        return anotherField.thatSideJoinColumn() == this.thisSideJoinColumn()
                && anotherField.thisSideJoinColumn() == this.thatSideJoinColumn()
    }


    override fun toString(): String {
        return "DataField(name='$name', javaType='$javaType')"
    }


}


class DataGroup(var name: String) {

    var fields = mutableListOf<String>()

    fun add(field: String) {
        fields.add(field)
    }
}

class ManyToOne(var to: String,
                @SerializedName("join-column") var joinColumn: String)

class OneToMany(var to: String,
                @SerializedName("their-join-column") var theirJoinColumn: String)

class ManyToMany(var to: String,
                 @SerializedName("join-table") var joinTable: String,
                 @SerializedName("our-join-column") var thisJoinColumn: String,
                 @SerializedName("their-join-column") var theirJoinColumn: String)


//https://db.apache.org/ojb/docu/guides/jdbc-types.html
fun getJavaTypeByJDBCType(jdbcType: JDBCType): Class<*> {
    return when (jdbcType) {
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
        JDBCType.DATE -> java.sql.Date::javaClass
        JDBCType.TIME -> java.sql.Time::javaClass
        JDBCType.TIMESTAMP -> java.sql.Timestamp::javaClass
        JDBCType.CLOB, JDBCType.BLOB -> throw NIY()
        JDBCType.ARRAY, JDBCType.DISTINCT, JDBCType.DATALINK, JDBCType.STRUCT, JDBCType.REF -> throw NIY()
        else -> throw NIY()
    } as Class<*>

}