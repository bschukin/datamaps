package com.datamaps.util

import org.apache.tomcat.util.collections.CaseInsensitiveKeyMap
import org.springframework.util.LinkedCaseInsensitiveMap


public inline fun <V> linkedCaseInsMapOf(): LinkedCaseInsensitiveMap<V> = LinkedCaseInsensitiveMap<V>()
public inline fun <V> linkedCaseInsMapOf(vararg pairs: Pair<String, V>): LinkedCaseInsensitiveMap<V> = if (pairs.size > 0) pairs.toMap(LinkedCaseInsensitiveMap(pairs.size)) else linkedCaseInsMapOf()


public inline fun <V> caseInsMapOf(): CaseInsensitiveKeyMap<V> = CaseInsensitiveKeyMap<V>()

typealias lcims = LinkedCaseInsensitiveMap<String>