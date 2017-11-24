package com.datamaps.maps;

import com.datamaps.mappings.DataProjection;
import com.datamaps.services.GenericDbMetadataService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by Щукин on 03.11.2017.
 */
public class SpecialClass {

    GenericDbMetadataService zxxx;

    void test ()
    {
      test2(s -> new DataProjection(s));

    }

    void test2(Function<String, DataProjection> function)
    {

    }





}
