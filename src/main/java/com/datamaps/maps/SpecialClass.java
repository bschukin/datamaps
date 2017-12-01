package com.datamaps.maps;

import java.io.Serializable;

/**
 * Created by Щукин on 03.11.2017.
 */
public class SpecialClass {

    ThreadLocal<SpecialClass> specialClassThreadLocal = new ThreadLocal<>();

    void test ()
    {
        int sss = 0;
    }

    class St<T extends Serializable>
    {

    }

}
