package com.datamaps.util

import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.stereotype.Service

/**
 * Created by b.schukin on 15.11.2017.
 */
@Service
class DataConverter {


    fun <T> convert(var1: Any?, var2: Class<T>): T?
    {
        if(var1==null)
            return null

        return DefaultConversionService.getSharedInstance().convert(var1, var2)
    }

}