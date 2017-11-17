package com.datamaps.mappings

import com.google.common.base.CaseFormat

/**
 * Created by b.schukin on 16.11.2017.
 */

interface NameMappingsStrategy {
    fun getDbTableName(javaName: String): String
    fun getJavaEntityName(dbName: String): String
    fun getJavaPropertyName(dbName: String): String
}

class AsIsNameMappingsStrategy : NameMappingsStrategy {
    override fun getJavaPropertyName(dbName: String): String = dbName

    override fun getJavaEntityName(dbName: String): String = dbName

    override fun getDbTableName(javaName: String): String = javaName
}


class CamelUnderscoreNameMappingsStrategy : NameMappingsStrategy {

    override fun getJavaPropertyName(dbName: String): String = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, dbName)

    override fun getJavaEntityName(dbName: String): String = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, dbName)

    override fun getDbTableName(javaName: String): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, javaName)
}