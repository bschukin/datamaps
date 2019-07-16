package com.bftcom.ice.datamaps.core.util


import com.bftcom.ice.datamaps.FieldSet
import com.bftcom.ice.datamaps.core.dialects.DbDialect
import com.bftcom.ice.datamaps.core.dialects.PostgresqlDialect
import com.bftcom.ice.datamaps.misc.FieldSetRepo
import com.bftcom.ice.datamaps.core.mappings.DbMetadataService
import com.bftcom.ice.datamaps.core.mappings.DataMapping
import com.bftcom.ice.datamaps.core.mappings.DataMappingsService
import com.bftcom.ice.datamaps.misc.DbForeignConstraintException
import com.bftcom.ice.datamaps.misc.DbUniqueConstraintException
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
                masterFieldNames.joinToString(", "), slaveFieldNames.joinToString(", "),
                value, exception)
    }

    private fun entityInfo(aentity:String, tableName:String): EntityInfo {
        val entity = dataMappingsService.getEntityDefaultNameByTableName(tableName)
        val dm = dataMappingsService.getDataMapping(entity, false)
        val fieldSet = FieldSetRepo.fieldSetOrNull(entity)?: FieldSetRepo.fieldSetOrNull(aentity)

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

    private data class EntityInfo(val entity:String, val mapping:DataMapping?, val fieldSet: FieldSet?, val localizedEntityName:String)
}

internal fun throwDbForeignConstraintException(tableFrom:String?, tableTo:String?,
                                               referencedToField:String?,
                                               referencedFromField:String?,
                                               key:String?, cause: RuntimeException) {

    val msg = "Операция обновления в таблице [$tableFrom] нарушает целостность. " +
            "\r\n На запись с ключом [$referencedToField]=($key) есть ссылки из таблицы [$tableTo] (поле [$referencedFromField])"

    throw DbForeignConstraintException(msg, cause)

}

internal fun throwDbUniqueConstraintException(table:String?, field:String?, value:String?, cause: RuntimeException) {
    val tablemsg = if(value.isNullOrBlank()) "" else ". Таблица: [$table]"
    val valuemsg = if(value.isNullOrBlank()) "" else ". Значение уже существует: [$value]"
    val fieldmsg = if(field.isNullOrBlank()) "" else ". Поле: [$field]"
    val msg = "Нарушено условие уникальности поля$tablemsg$fieldmsg$valuemsg"
    throw DbUniqueConstraintException(msg, cause)
}