package com.datamaps.mappings

import com.datamaps.BaseSpringTests
import org.testng.annotations.Test
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */
class DmFileStorageTests: BaseSpringTests() {

    @Resource
    lateinit var dmFileStorage:DmFileStorage;

    @Test
    fun testInitPathes()
    {
        println(dmFileStorage.pathes)
        print(dmFileStorage.mappingFiles.keys)
    }


}