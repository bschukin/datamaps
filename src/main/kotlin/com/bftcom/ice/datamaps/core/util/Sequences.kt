package com.bftcom.ice.datamaps.core.util

import com.bftcom.ice.datamaps.core.mappings.GenericDbMetadataService
import com.bftcom.ice.datamaps.misc.NIY
import com.bftcom.ice.datamaps.core.mappings.DataMappingsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.support.incrementer.*
import org.springframework.stereotype.Service
import javax.sql.DataSource


interface SequenceIncrementor {
    fun canGenerateIdFromSequence(table: String): Boolean
    fun getNextId(entity: String): Long
}

interface SequenceIncrementorFactory {
    fun getIncrementor(table: String): DataFieldMaxValueIncrementer?
}

@Service
class SequenceIncrementorImpl : SequenceIncrementor {
    @Autowired private
    lateinit var dataMappingsService: DataMappingsService

    @Autowired private
    lateinit var sequenceIncrementorFactory: SequenceIncrementorFactory

    val map = mutableMapOf<String, DataFieldMaxValueIncrementer?>()


    override fun canGenerateIdFromSequence(table: String): Boolean {
        val inc = getIncrementor(table)
        return inc!=null
    }


    override fun getNextId(entity: String): Long {
        val table = dataMappingsService.getTableNameByEntity(entity)

        return getIncrementor(table)!!.nextLongValue()
    }

    private fun getIncrementor(table: String): DataFieldMaxValueIncrementer? {


        if (!map.containsKey(table)) {
            map[table] = sequenceIncrementorFactory.getIncrementor(table)
        }
        return map[table]
    }
}

@Service
class SequenceIncrementorFactoryImpl : SequenceIncrementorFactory {

    @Autowired private
    lateinit var genericDbMetadataService: GenericDbMetadataService

    @Autowired private
    lateinit var dataSource: DataSource

    override fun getIncrementor(table: String): DataFieldMaxValueIncrementer? {
        when {

            genericDbMetadataService.isHsqlDb() ->
            {
                val inc = HsqlSequenceMaxValueIncrementer(dataSource, "${table}_SEQ")
                return try {
                    inc.nextLongValue()
                    inc
                } catch (e: Exception) {
                    null//TODO: Борис Вячеславыч, что это?
                }
            }
            genericDbMetadataService.isPostgreSql() -> {
                val t = genericDbMetadataService.getTableInfo(table)
                if (t.identitySequnceName != null)
                    return PostgreSQLSequenceMaxValueIncrementer(dataSource, t.identitySequnceName)
                return null
            }
            genericDbMetadataService.isOracle() -> {
                val t = genericDbMetadataService.getTableInfo(table)
                if (t.identitySequnceName != null)
                    return OracleSequenceMaxValueIncrementer(dataSource, t.identitySequnceName)
                return null
            }
            genericDbMetadataService.isFirebird() -> {
                val t = genericDbMetadataService.getTableInfo(table)
                if (t.identitySequnceName != null)
                    return FirebirdSequenceMaxValueIncrementer(dataSource, t.identitySequnceName)
                return null
            }
        }
       throw NIY()
    }

    class FirebirdSequenceMaxValueIncrementer(dataSource:DataSource, incrementerName:String?)
        : AbstractSequenceMaxValueIncrementer(dataSource, incrementerName)
    {
        override fun getSequenceQuery(): String {
            return "select gen_id($incrementerName, 1) from RDB\$DATABASE";
        }

    }

}