package com.bftcom.ice.datamaps.core.dbsync

import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.assertBodyEquals
import com.bftcom.ice.server.assertEqIgnoreCase
import com.bftcom.ice.IfSpringProfileActive
import com.bftcom.ice.datamaps.getAllFields
import org.junit.Assert
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Created by Щукин on 03.11.2017.
 */
open class DbDiffsTests : BaseSpringTests() {


    @Autowired
    lateinit var dbDiffService: DbDiffService

    @Autowired
    lateinit var dbScriptMaker: DbScriptMaker


    @Test
    fun test01GetDiff() {

        println(TBD01.getAllFields())

        val dbDiff = dbDiffService.makeDiffForOneEntity(TBD01.entity)
        assertEqIgnoreCase(dbDiff.entity, TBD01.entity)

        //проверяем сколько полей не досчитались в FieldSet'e по сравнению с базой
        Assert.assertTrue(dbDiff.fieldsOnlyInDb.size==3)
        Assert.assertTrue(dbDiff.fieldsOnlyInDb[0].name == "name2")
        Assert.assertTrue(dbDiff.fieldsOnlyInDb[1].name == "someiny")
        Assert.assertTrue(dbDiff.fieldsOnlyInDb[2].name == "tbd01Childs2")


        //проверяем сколько полей в FieldSet'e которыъ нет в базе
        Assert.assertTrue(dbDiff.fieldsOnlyInDataMap.size==3)
        Assert.assertTrue(dbDiff.fieldsOnlyInDataMap[0].fieldName == "newField")
        Assert.assertTrue(dbDiff.fieldsOnlyInDataMap[1].fieldName == "myRef")
        Assert.assertTrue(dbDiff.fieldsOnlyInDataMap[2].fieldName == "tbd0Babes")


        //проверяем измененные поля
        when (isOracle()) {
            true -> Assert.assertTrue(dbDiff.changedFields.size==3)
            false -> Assert.assertTrue(dbDiff.changedFields.size==2)
        }

        Assert.assertTrue(dbDiff.changedFields[0].typeChange!=null)

        var result = false

        dbDiff.changedFields.forEach {
            it.nameChange?.let { nameChange ->
                Assert.assertTrue(nameChange.inDb.equals("oldName", true))
                Assert.assertTrue(nameChange.inMapping.equals("newName", true))
                result = true
            }
        }

        Assert.assertTrue(result)

    }


    @Test
    @IfSpringProfileActive("postgresql")
    fun test02DiffScript() {

        val dbDiff = dbDiffService.makeDiffForOneEntity(TBD01.entity)

        val script = dbScriptMaker.makeScript(dbDiff)

        println(script)

        assertBodyEquals(script,"""
            ALTER TABLE tbd01 ADD COLUMN IF NOT EXISTS "NEW_FIELD" text;

            ALTER TABLE tbd01 ADD COLUMN IF NOT EXISTS "MY_REF_ID" integer;
            ALTER TABLE tbd01 DROP CONSTRAINT  IF EXISTS   "tbd01_MY_REF_ID_fk";
            ALTER TABLE tbd01 ADD CONSTRAINT "tbd01_MY_REF_ID_fk" FOREIGN KEY ("my_ref_id") REFERENCES tbd0(id);

            ALTER TABLE tbd01_babe ADD COLUMN IF NOT EXISTS "TBD01_ID" integer;
            ALTER TABLE tbd01_babe DROP CONSTRAINT  IF EXISTS   "tbd01_babe_TBD01_ID_FK";
            ALTER TABLE tbd01_babe ADD CONSTRAINT "tbd01_babe_TBD01_ID_FK" FOREIGN KEY ("TBD01_ID") REFERENCES tbd01(id)   ON DELETE CASCADE;

            ALTER TABLE tbd01 DROP COLUMN IF EXISTS "name2" CASCADE;
            ALTER TABLE tbd01 DROP COLUMN IF EXISTS "someiny" CASCADE;
            """)

    }
}

object TBD0 : MFS<TBD0>("Tbd0")

object TBD01 : MFS<TBD01>("Tbd01") {
    val id = Field.id()
    val name = Field.string("name")
    val tbd01 = Field.reference("tbd01", TBD0)
    val tbd01Childs = Field.list("tbd01Childs")
    val newField2 = Field.string("newField")
    val newName = Field.string("newName"){prevFieldName = "oldName"}
    val booleanField  = Field.int("booleanField")
    val myRef = Field.reference("myRef", TBD0)
    val tbd01Babes = Field.list("tbd0Babes", Tbd01Babe)
}

object Tbd01Babe : MFS<Tbd01Babe>("Tbd01Babe")