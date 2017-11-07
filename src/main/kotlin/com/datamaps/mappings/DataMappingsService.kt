package com.datamaps.mappings

import org.springframework.stereotype.Service

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
public class DataMappingsService {

    var mappings : Map<String, DataMapping> = hashMapOf()

    fun getDataMapping (name:String): DataMapping?
    {
        var dm =  mappings.get(name)
        if(dm==null)
        {
            dm = readOriginalMapping(name);
        }
        return dm;
    }

    private fun readOriginalMapping(name: String): DataMapping? {
        TODO("not implemented")
    }

}