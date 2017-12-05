package com.datamaps.services

import com.datamaps.general.checkNIS
import com.datamaps.mappings.DataMappingsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer
import org.springframework.jdbc.support.incrementer.HsqlSequenceMaxValueIncrementer
import org.springframework.jdbc.support.incrementer.PostgreSQLSequenceMaxValueIncrementer
import org.springframework.stereotype.Service
import javax.sql.DataSource


interface SequenceIncrementor {
    fun canGenerateId(entity: String): Boolean
    fun getNextId(entity: String): Long
}

interface SequenceIncrementorFactory {
    fun getIncrementor(table: String): DataFieldMaxValueIncrementer?
}

@Service
class SequenceIncrementorImpl : SequenceIncrementor {
    @Autowired
    lateinit var dataMappingsService: DataMappingsService

    @Autowired
    lateinit var sequenceIncrementorFactory: SequenceIncrementorFactory

    val map = mutableMapOf<String, DataFieldMaxValueIncrementer?>()


    override fun canGenerateId(entity: String): Boolean {
        val inc = getIncrementor(entity)
        return inc!=null
    }


    override fun getNextId(entity: String): Long {

        checkNIS(canGenerateId(entity))

        return getIncrementor(entity)!!.nextLongValue()
    }

    fun getIncrementor(entity: String): DataFieldMaxValueIncrementer? {

        val table = dataMappingsService.getTableNameByEntity(entity)

        if (!map.containsKey(entity)) {
            map[entity] = sequenceIncrementorFactory.getIncrementor(table)
        }
        return map[entity]
    }
}

@Service
class SequenceIncrementorFactoryImpl : SequenceIncrementorFactory {

    @Autowired
    lateinit var genericDbMetadataService: GenericDbMetadataService

    @Autowired
    lateinit var dataSource: DataSource

    override fun getIncrementor(table: String): DataFieldMaxValueIncrementer? {
        when {

            genericDbMetadataService.isHsqlDb() ->
            {
                val inc = HsqlSequenceMaxValueIncrementer(dataSource, "${table}_SEQ")
                try {
                    inc.nextLongValue()
                    return inc
                } catch (e: Exception) {
                    return null
                }
            }
            genericDbMetadataService.isProstgress() -> {
                val t = genericDbMetadataService.getTableInfo(table)
                if (t.identitySequnceName != null)
                    return PostgreSQLSequenceMaxValueIncrementer(dataSource, t.identitySequnceName)
                return null
            }
        }
        TODO()
    }
}