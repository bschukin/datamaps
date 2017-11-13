package com.datamaps.util

import org.apache.tomcat.util.collections.CaseInsensitiveKeyMap
import org.springframework.util.LinkedCaseInsensitiveMap


public inline fun <V> linkedCaseInsMapOf(): LinkedCaseInsensitiveMap<V> = LinkedCaseInsensitiveMap<V>()

public inline fun <V> caseInsMapOf(): CaseInsensitiveKeyMap<V> = CaseInsensitiveKeyMap<V>()

