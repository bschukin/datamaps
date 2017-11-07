package com.datamaps.maps;

import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.nio.file.Path;
import java.sql.JDBCType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Щукин on 03.11.2017.
 */
public class SpecialClass {

    @Value("${xxxx}")
    String zxxx;

    void test ()
    {
        //JDBCType.valueOf (crs.getInt("DATA_TYPE"))
        Path path;
    }

}
