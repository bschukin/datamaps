package com.bftcom.ice.datamaps.impl.mappings

import com.bftcom.ice.datamaps.misc.*
import java.sql.JDBCType
import kotlin.reflect.KClass

/**
 * Created by Щукин on 28.10.2017.
 *
 * Маппинг
 *
 */
data class DataMapping(val name: String,
                       val table: String,
                       val idColumn: String? = ID,
                       val idGenerationType: IdGenerationType = IdGenerationType.NONE,
                       val fields: MutableMap<String, DataField> = linkedCaseInsMapOf(),
                       val groups: MutableMap<String, DataGroup> = linkedCaseInsMapOf()) {

    init {
        if (groups.isEmpty()) {
            groups[DEFAULT] = DataGroup(DEFAULT)
            groups[FULL] = DataGroup(FULL)
            groups[REFS] = DataGroup(REFS)
            groups[LIST] = DataGroup(LIST)
        }
    }

    val defaultGroup: DataGroup
        get() = groups[DEFAULT]!!

    val fullGroup: DataGroup
        get() = groups[FULL]!!

    val refsGroup: DataGroup
        get() = groups[REFS]!!

    val listsGroup: DataGroup
        get() = groups[LIST]!!

    val blobsGroup: DataGroup
        get() {
            return if (groups.containsKey(BLOB)) groups[BLOB]!!
            else {
                groups[BLOB] = DataGroup(BLOB)
                return groups[BLOB]!!
            }
        }

    val formulasGroup: DataGroup
        get() {
            return if (groups.containsKey(FORMULA)) groups[FORMULA]!!
            else {
                groups[FORMULA] = DataGroup(FORMULA)
                return groups[FORMULA]!!
            }
        }


    fun add(field: DataField) = fields.put(field.name, field)

    fun remove(field: String) {
        fields.remove(field)
        groups.forEach {
            it.value.remove(field)
        }
    }

    fun idField(): DataField? = fields.values.first { it.sqlcolumn == idColumn }

    operator fun get(field: String): DataField {
        return fields[field.toLowerCase()]
                ?: throw SNF("field '$field' not found in entity '$name'")
    }

    fun findByName(name: String) = fields.values.firstOrNull { name.equals(it.name, true) }
    fun findByDbColumnName(name: String) = fields.values.filter { name.equals(it.sqlcolumn, true) }.firstOrNull()

    fun findByReference(ref: Reference) = fields.values.filter { ref == it.reference }.firstOrNull()

    fun getBackReferenceFieldForThisList(parent: DataMapping, listProperty: String): DataField {
        val oneToMany = parent[listProperty]
        makeSure(oneToMany.is1N())

        //ищем по явно-объявленному референсу
        val res = fields.values.filter { it.isM1() }.find {
            it.referencedOneToAnother(oneToMany)
        }
        if (res != null)
            return res

        //ищем по совпадению SQL-column
        return findByDbColumnName(oneToMany.thatSideJoinColumn()!!)!!
    }

    fun getListFieldForThisBackRef(childMapping: DataMapping, refProperty: String): DataField? {
        val manyToOne = childMapping[refProperty]
        makeSure(manyToOne.isM1())

        //ищем по явно-объявленному референсу
        val res = fields.values.filter { it.is1N() }.find {
            it.referencedOneToAnother(manyToOne)
        }
        return res
    }


    fun scalars() = fields.filter { u -> u.value.isSimple() }

    fun refs() = fields.filter { u -> u.value.isM1() }

    fun collections() = fields.filter { u -> u.value.is1N() }

    override fun toString(): String {
        return "DataMapping(entity='$name', table='$table')"
    }

    fun print() {
        println(toString())
        fields.values.forEach { println(it) }
    }
}

enum class IdGenerationType {
    NONE, //самостоятельно, епт
    IDENTITY, //identity-колонка
    SEQUENCE //из сиквенса с именем таблицы + _SEQ
}

data class DataGroup(val name: String) {
    val fields = mutableListOf<String>()

    fun add(field: String) {
        fields.add(field)
    }

    fun remove(field: String) {
        fields.remove(field)
    }
}


data class DataField internal constructor(val name: String,
                                          val sqlcolumn: String?,
                                          val description: String,
                                          val reference: Reference?,
                                          val kotlinType: KClass<*>,
                                          val jdbcType: JDBCType,
                                          val isBackReference: Boolean = false,
                                          val oqlFormula:OqlFormula? = null) {

    fun isSimple() = reference == null

    fun isReference() = reference != null

    fun oneToMany() = reference as OneToMany

    fun reference() = reference as Reference

    fun isM1() = reference is ManyToOne

    fun is1N() = reference is OneToMany

    fun isJson() = jdbcType == JDBCType.STRUCT

    fun referenceTo() = reference!!.to

    fun isScalarOqlFormula() = oqlFormula!=null && oqlFormula.lateralTable==null

    fun isReferenceOqlFormula():Boolean = oqlFormula?.lateralTable != null

    private fun thisSideJoinColumn() =
            when {
                isReference() -> reference().thisSideJoinColumn
                else -> null
            }

    fun thatSideJoinColumn() =
            when {
                isReference() -> reference().thatSideJoinColumn
                else -> null
            }

    fun referencedOneToAnother(anotherField: DataField): Boolean {
        return anotherField.thatSideJoinColumn() == this.thisSideJoinColumn()
                && anotherField.thisSideJoinColumn() == this.thatSideJoinColumn()
    }


    override fun toString(): String {
        return "DataField(name='$name', kotlinType='$kotlinType' desc='$description')"
    }
}

internal data class OqlFormula(val oql: String,
                               val lateralTable: String? = null,
                               val isList: Boolean = false)

sealed class Reference(val to: String,
                       val thisSideJoinColumn: String,
                       val thatSideJoinColumn: String) {

}

class ManyToOne(to: String,
                thisSideJoinColumn: String,
                thatSideJoinColumn: String) : Reference(to, thisSideJoinColumn, thatSideJoinColumn)

class OneToMany(to: String,
                thisSideJoinColumn: String,
                thatSideJoinColumn: String) : Reference(to, thisSideJoinColumn, thatSideJoinColumn)

//TODO
/*data class ManyToMany(val to: String,
                      val joinTable: String,
                      val thisJoinColumn: String,
                      val theirJoinColumn: String)*/

