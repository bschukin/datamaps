package com.datamaps.util

import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.lang.Nullable


///для null - аргументов не кидается ошибкой для притимивных типов (что нужно для котлина)
class DefaultNullPassByConversionService : DefaultConversionService()
{

    override fun <T> convert(@Nullable source: Any?, targetType: Class<T>): T? {
        if(source==null)
            return source;
        return super.convert(source, targetType)
    }

}

class NullPassByConversionServiceFactoryBean: ConversionServiceFactoryBean()
{
    override fun createConversionService(): GenericConversionService {
        return DefaultNullPassByConversionService()
    }
}