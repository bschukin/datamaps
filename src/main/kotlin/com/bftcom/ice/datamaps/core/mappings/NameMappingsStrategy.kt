package com.bftcom.ice.datamaps.core.mappings

import com.google.common.base.CaseFormat
import org.apache.commons.text.StrLookup
import org.apache.commons.text.StrSubstitutor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by b.schukin on 16.11.2017.
 */

interface NameMappingsStrategy {


    var tableNamePrefix: String?

    fun getDefaultDbTableName(javaName: String): String
    fun getDbColumnName(javaName: String): String = getDefaultDbTableName(javaName)
    fun getJavaEntityName(dbName: String): String
    fun getJavaPropertyName(dbName: String): String

    fun getJavaPropertyEscapedIdName(col: String, escapeId: Boolean = true): String {
        val n = if(escapeId) escapeId(col) else col
        return getJavaPropertyName(n)
    }

    fun getDefaultCollectionName(fkTable: String, counter: Int = 1): String {

        val column = getJavaPropertyName(escapeId(fkTable))

        val res = when (column.last()) {
            's' -> column + "es"
            else -> column + "s"
        }
        return if (counter > 1) res + counter else res
    }

    fun getDefaultCollectionColumnName(javaCollectionName: String): String {

        val column = escapeS(javaCollectionName)

        return getDbColumnName(column)
    }


    companion object {
        fun escapeId(name: String): String {
            return when {
                name.equals("id", true) -> name
                name.endsWith("_id", true) -> name.substring(0, name.length - 3)
                name.endsWith("id", true) -> name.substring(0, name.length - 2)
                else -> name
            }

        }

        fun escapeS(name: String): String {
            return when {
                name.equals("s", true) -> name
                name.endsWith("s", true) -> name.substring(0, name.length - 1)
                else -> name
            }

        }

    }
}


@Service
class TableNameResolver {
    @Resource
    private lateinit var nameMappingsStrategy: NameMappingsStrategy

    private val resolver by lazy {
        TableNameDialectResolver(nameMappingsStrategy)
    }

    fun resolveTableNamesForActiveNameStrategy(sql: String): String {

        val s = StrSubstitutor(resolver, "{{", "}}", '/')
        return s.replace(sql)
    }

    private class TableNameDialectResolver(val nameMappingsStrategy: NameMappingsStrategy) :
            StrLookup<String>() {
        override fun lookup(key: String?): String {
            return nameMappingsStrategy.getDefaultDbTableName(key!!)
        }
    }

}

open class AsIsNameMappingsStrategy : NameMappingsStrategy {

    @Value("\${dm.tablePrefix:}")
    override var tableNamePrefix: String? = null

    override fun getJavaPropertyName(dbName: String): String {
        return dbName.removePrefix(tableNamePrefix.orEmpty(), true)
    }

    override fun getJavaEntityName(dbName: String): String {
        return dbName.removePrefix(tableNamePrefix.orEmpty(), true)
    }

    override fun getDefaultDbTableName(javaName: String): String {
        return tableNamePrefix.orEmpty() + javaName
    }
}


class CamelUnderscoreNameMappingsStrategy : AsIsNameMappingsStrategy() {

    override fun getJavaPropertyName(dbName: String): String = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
            super.getJavaEntityName(dbName))

    override fun getJavaEntityName(dbName: String): String {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, super.getJavaEntityName(dbName))
    }

    override fun getDefaultDbTableName(javaName: String): String {
        val tableName = if (javaName.toUpperCase() == javaName) javaName else {
            CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, javaName)
        }
        return super.getDefaultDbTableName(tableName)
    }
}

private fun String.removePrefix(prefix: CharSequence, ignoreCase: Boolean): String {
    if (startsWith(prefix, ignoreCase)) {
        return substring(prefix.length)
    }
    return this
}
