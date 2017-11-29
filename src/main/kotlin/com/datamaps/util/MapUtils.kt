package com.datamaps.util

import org.apache.tomcat.util.collections.CaseInsensitiveKeyMap
import org.springframework.util.LinkedCaseInsensitiveMap


fun <V> linkedCaseInsMapOf(): LinkedCaseInsensitiveMap<V> = LinkedCaseInsensitiveMap()
fun <V> linkedCaseInsMapOf(vararg pairs: Pair<String, V>): LinkedCaseInsensitiveMap<V> = if (pairs.isNotEmpty()) pairs.toMap(LinkedCaseInsensitiveMap(pairs.size)) else linkedCaseInsMapOf()


fun <V> caseInsMapOf(): CaseInsensitiveKeyMap<V> = CaseInsensitiveKeyMap()

typealias lcims = LinkedCaseInsensitiveMap<String>