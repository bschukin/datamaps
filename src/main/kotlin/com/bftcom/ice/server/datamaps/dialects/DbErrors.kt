package com.bftcom.ice.server.datamaps.dialects

import com.bftcom.ice.common.general.throwDbForeignConstraintException
import com.bftcom.ice.common.general.throwDbUniqueConstraintException
import com.bftcom.ice.common.maps.FieldSet
import com.bftcom.ice.common.maps.FieldSetRepo
import com.bftcom.ice.server.datamaps.DbMetadataService
import com.bftcom.ice.server.datamaps.mappings.DataMapping
import com.bftcom.ice.server.datamaps.mappings.DataMappingsService
import com.bftcom.ice.server.services.DbDialect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/****
 * Сервис, который отвечает за перевод сообщений SQL-ошибок в сообщения,
 * хотя бы приближенные к пользователю
 */
@Service
class DbExceptionsMessageTranslator {

    @Autowired
    private lateinit var dbDialect: DbDialect

    @Autowired
    private lateinit var dbMetadataService: DbMetadataService

    @Autowired
    private lateinit var dataMappingsService: DataMappingsService


    private lateinit var translator: SqlErrorMessageTranslator

    @PostConstruct
    private fun init() {
        translator = buildExceptionTranslator()
    }

    fun makeMessageUserFriendlyOrRethrow(entity:String, exception: RuntimeException) {
        if (exception is DuplicateKeyException)
            translator.translateUniqueConstraintMessage(entity, exception)

        if (exception is DataIntegrityViolationException)
            translator.translateForeignConstraintMessage(entity, exception)

        throw exception
    }

    private fun buildExceptionTranslator(): SqlErrorMessageTranslator {
        return when (dbDialect) {
            is PostgresqlDialect -> PostgreSqlErrorMessageTranslator(dbMetadataService, dataMappingsService)
            else -> DefaultSqlErrorMessageTranslator()
        }
    }


}

private interface SqlErrorMessageTranslator {
    fun translateForeignConstraintMessage(entity:String, exception: RuntimeException)

    fun translateUniqueConstraintMessage(entity:String, exception: RuntimeException)
}

private class DefaultSqlErrorMessageTranslator : SqlErrorMessageTranslator {
    override fun translateForeignConstraintMessage(entity:String, exception: RuntimeException) {
        throw exception
    }

    override fun translateUniqueConstraintMessage(entity:String, exception: RuntimeException) {
        throw exception
    }
}


private class PostgreSqlErrorMessageTranslator(
        val dbMetadataService: DbMetadataService,
        val dataMappingsService: DataMappingsService) : SqlErrorMessageTranslator {


    //en: ERROR: duplicate key value violates unique constraint "hand_name_key"
    //  Detail: Key (name)=(xxx) already exists.
    override fun translateUniqueConstraintMessage(entity:String, exception: RuntimeException) {
        val message = exception.message ?: throw exception

        //1 ищем уникальный индекс
        val indexName = dbMetadataService.getUniqueIndexes().find { message.contains(it.name) } ?: throw exception

        //2 инофрмация о сущности
        val entityName = entityInfo(entity, indexName.table)

        //3 поля
        val fieldNames = fieldNames(entityName, indexName.columns)

        //4. значение
        val value = obtainValue(message)

        throwDbUniqueConstraintException(entityName.localizedEntityName, fieldNames.joinToString(", "), value, exception)

    }




    // ru: UPDATE или DELETE в таблице "hand" нарушает ограничение внешнего ключа "finger_hand_id_fkey" таблицы "finger"
    //  Подробности: На ключ (id)=(1) всё ещё есть ссылки в таблице "finger".
    override fun translateForeignConstraintMessage(entity:String, exception: RuntimeException) {

        val message = exception.message ?: throw exception

        //1 ищем уникальный индекс
        val foreignKey = dbMetadataService.getExportedKeys().find { message.contains(it.name) } ?: throw exception

        //2. родительская  и дочерняя таблица:
        val masterEntity = entityInfo(entity, foreignKey.pkTable)
        val slaveEntity = entityInfo(entity, foreignKey.fkTable)

        //3. поля
        val masterFieldNames = fieldNames(masterEntity, listOf(foreignKey.pkColumn))
        val slaveFieldNames = fieldNames(slaveEntity, listOf(foreignKey.fkColumn))

        //4. значение
        val value = obtainValue(message)

        throwDbForeignConstraintException(
                masterEntity.localizedEntityName, slaveEntity.localizedEntityName,
                masterFieldNames.joinToString (", "), slaveFieldNames.joinToString(", "),
                value, exception)
    }

    private fun entityInfo(aentity:String, tableName:String):EntityInfo {
        val entity = dataMappingsService.getEntityDefaultNameByTableName(tableName)
        val dm = dataMappingsService.getDataMapping(entity, false)
        val fieldSet = FieldSetRepo.fieldSetOrNull(entity)?:FieldSetRepo.fieldSetOrNull(aentity)

        val localizedEntityName = fieldSet?.caption ?: entity
        return EntityInfo(entity, dm, FieldSetRepo.fieldSetOrNull(entity), localizedEntityName)
    }

    private fun fieldNames(entityName: EntityInfo, columns: List<String>): List<String> {
        return if (entityName.fieldSet != null) columns.map {
            val eFieldName = entityName.mapping?.findByDbColumnName(it)?.name ?: it
            FieldSetRepo.getFieldOrNull(entityName.fieldSet, eFieldName)?.caption ?: it
        }
        else columns
    }

    private fun obtainValue(message: String): String {
        val index1 = message.indexOf(")=(")
        return if (index1 > -1) message.substring(index1 + 3, message.indexOf(")", index1 + 3)) else ""
    }

    private data class EntityInfo(val entity:String, val mapping:DataMapping?, val fieldSet:FieldSet?, val localizedEntityName:String)
}
