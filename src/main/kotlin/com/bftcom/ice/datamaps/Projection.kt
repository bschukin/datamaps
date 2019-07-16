package com.bftcom.ice.datamaps

import com.bftcom.ice.datamaps.misc.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

/**
 * Created by Щукин on 07.11.2017.
 */

/**
 *
 * Worker
 * {
 *        name,
 *        gender {
 *             gender
 *        }
 * }
 *
 */

/*"Проекция" в графе объектов.
Может правильно называть "Ребро" */
//@Serializable
typealias DataProjection = DataProjectionF<*>

typealias Projection = DataProjectionF<UndefinedFieldSet>

class UndefinedProjection: DataProjectionF<UndefinedFieldSet>(UndefinedFieldSet.entity)

abstract class BaseProjection {
    //выражение  where
    var filter: exp? = null

    //альтернативный вариант where - "OQL"
    var oql: String? = null

    var limit: Int? = null
    var offset: Int? = null

    //сортировки
    var _orders: List<ExpressionField> = mutableListOf()

    //группировки
    var groupByFields = mutableListOf<String>()

    var params = mutableMapOf<String, Any?>()

    fun where(): String? {
        return this.oql
    }

    fun orders() = _orders

    open fun filter(exp: exp?): BaseProjection {
        if (exp == null)
            return this
        filter = if (filter == null) exp else (filter!! and exp)
        return this
    }

    open fun filter(aaa: (m: Unit) -> exp): BaseProjection {
        val exp = aaa(Unit)
        filter = if (filter == null) exp else (filter!! and exp)
        return this
    }

    open fun where(oql: String): BaseProjection {
        this.oql = oql
        return this
    }

    open fun param(k: String, v: Any?): BaseProjection {
        this.params[k] = v
        return this
    }

    open fun order(vararg fields: f): BaseProjection {
        (_orders as MutableList).addAll(fields)
        return this
    }

    open fun order(vararg fields: Field<*, *>): BaseProjection {
        (_orders as MutableList).addAll(fields.map { f(it) }.toList())
        return this
    }

    open fun limit(l: Int): BaseProjection {
        limit = l
        return this
    }

    open fun offset(l: Int): BaseProjection {
        offset = l
        return this
    }

    open fun groupBy(vararg fields: f): BaseProjection {
        groupByFields.addAll(fields.map { it.name })
        return this
    }

    open fun groupBy(vararg fields: Field<*, *>): BaseProjection {
        groupByFields.addAll(fields.map { it.n })
        return this
    }

    abstract fun copy(): BaseProjection
}

class ProjectionOptions(var collectionJoinType: JoinType? = null,
                        var retrieveType: RetrieveType? = null,
                        var recursiveDepth: Int? = -1,
                        var serviceMethodCall: ServiceMethodCall? = null,
                        var customParams:MutableMap<String, Any?>? = null) {
    fun withSubSelect() {
        collectionJoinType = JoinType.SELECT
    }

    fun asMapWithOnlyId() {
        retrieveType = RetrieveType.JustId
    }

    fun asSimpleField() {
        retrieveType = RetrieveType.SimpleField
    }

    fun recursive(depth: Int? = null) {
        retrieveType = RetrieveType.Recursive
        if (depth != null && depth > -1)
            TODO()
    }

    fun serviceCall(className: String, method: String) {
        serviceMethodCall = ServiceMethodCall(className, method)
    }

    fun addCustomParam(name:String, value:Any?){
        if(customParams==null)
            customParams = mutableMapOf()
        customParams!![name] = value
    }

    fun copy(): ProjectionOptions {
        return ProjectionOptions(collectionJoinType, retrieveType,
                recursiveDepth, serviceMethodCall)
    }
}

data class ServiceMethodCall(val className: String, val method: String)

open class DataProjectionF<T : FieldSet> : BaseProjection {
    //для корневой проекции  - сущность по которой надо строить запрос
    //для вложенных поекций - опционально
    var entity: String? = null
    var fieldSet: T? = null
    //алиас, использованный для данной сущности в запросе (например для использования в фильтрах)
    var queryAlias: String? = null

    //id объекта - возможно указание только для рутовых ОП
    var id: Any? = null
    //для вложенных проекций - родительское поле
    internal var parentField: String? = null
    //группы, которые включеные в проекцию
    var groups = mutableListOf<String>()
    //поля, вк люченные в проекцию - в вилей проекций
    var fields: MutableMap<String, DataProjection> = CaseInsensitiveKeyMap() //рекурсивные проекции

    //вычислимые поля (формулы)
    var formulas: MutableMap<String, String> = mutableMapOf()

    //вычислимые поля, основанные на латеральных джойнах
    var laterals = mutableListOf<Lateral>()

    //выражение  текстового фильтра
    var textFilter: exp? = null
    //выражение  текстового фильтра
    var textFilterString: String? = null

    var options: ProjectionOptions? = null


    //целевая дата для  темпоральных запросов
    var onDate: Date? = null

    var joinFilter: Expression? = null

    @Suppress("unused")
    constructor()

    constructor(entity: KClass<*>) {
        this.entity = entity.simpleName
    }

    constructor(fieldSet: T) {

        this.entity = fieldSet.entity
        this.fieldSet = fieldSet
    }

    constructor(entity: String) {
        this.entity = entity
    }

    constructor(entity: String?, id: Any?) {

        this.entity = entity
        this.id = id
    }

    constructor(entity: String?, field: String?) {
        this.parentField = field
        this.entity = entity
    }

    fun setFieldSetIfShould(fieldSet: FieldSet?) {
        if (this.fieldSet == null && fieldSet != null)
            this.fieldSet = fieldSet as T
    }

    /***
     * получить субпроекцию
     */
    operator fun get(field: String): DataProjection? {
        return fields[field]
    }

    /**
     * установить алиас на проекцию
     */
    fun alias(alias: String): DataProjectionF<T> {
        queryAlias = alias
        return this
    }

    fun group(gr: String): DataProjectionF<T> {
        groups.add(gr)
        return this
    }


    fun onlyId(): DataProjection {
        return field("id")
    }

    fun full(): DataProjectionF<T> {
        return group(FULL)
    }

    fun scalars(): DataProjectionF<T> {
        return group(DEFAULT)
    }


    fun withRefs(): DataProjectionF<T> {
        return group(REFS)
    }

    fun withBlobs(): DataProjectionF<T> {
        return group(BLOB)
    }

    fun withFormulas(): DataProjectionF<T> {
        return group(FORMULA)
    }

    fun withCollections(): DataProjectionF<T> {
        return group(LIST)
    }


    fun field(f: String): DataProjectionF<T> {
        fields[f] = slice(f)
        return this
    }

    fun field(f: KCallable<*>): DataProjection {
        fields[f.name] = slice(f.name)
        return this
    }

    fun field(f: Field<*, *>): DataProjectionF<T> {
        fields[f.n] = slice(f.n)
        return this
    }

    fun fields(vararg fields: Field<*, *>): DataProjectionF<T> {
        fields.forEach {
            field(it)
        }
        return this
    }

    fun fields(vararg fields: String): DataProjectionF<T> {
        fields.forEach {
            field(it)
        }
        return this
    }

    fun fields(vararg afields: T.() -> Field<*, *>): DataProjectionF<T> {
        afields.forEach {
            val f = it(fieldSet!!)
            field(f)
        }
        return this
    }

    fun with(slice: () -> DataProjection): DataProjection {
        val sl = slice()
        fields[sl.parentField!!] = sl
        return this
    }

    fun with(sl: DataProjection): DataProjection {
        fields[sl.parentField!!] = sl
        return this
    }

    fun with(vararg fields: String): DataProjectionF<T> {

        fields.forEach {
            var currProjection: DataProjection = this

            val names = it.split('.')
            names.forEach { n ->

                if (currProjection.fields[n] == null)
                    currProjection.fields[n] = slice(n)
                currProjection = currProjection.fields[n]!!
            }

        }
        return this
    }

    fun with(vararg fields: Field<*, *>): DataProjection {
        fields.forEach {
            var currProjection: DataProjection = this

            val names = it.f.n.split('.')
            names.forEach { n ->

                if (currProjection.fields[n] == null)
                    currProjection.fields[n] = slice(n)
                currProjection = currProjection.fields[n]!!
            }

        }
        return this
    }


    fun id(id: Any?): DataProjectionF<T> {
        makeSure(isRoot())
        this.id = id
        return this
    }

    //todo: добавить возможность укаазть тип, к которому вязаться
    fun formula(name: String, formula: String): DataProjection {
        formulas[name] = formula
        return this
    }

    fun lateral(lateralTableAlias: String, oql: String, vararg pairs: Pair<String, String>): DataProjection {
        val lmap = linkedCaseInsMapOf<String>()
        pairs.forEach { pair ->
            lmap[pair.second] = pair.first
        }
        laterals.add(Lateral(lateralTableAlias, oql, null, false, linkedCaseInsMapOf(*pairs)))
        return this
    }

    fun embeddedLateral(lateralTableAlias: String,
                        property: String,
                        oql: String,
                        vararg pairs: Pair<String, String>): DataProjection {
        val lmap = linkedCaseInsMapOf<String>()
        pairs.forEach { pair ->
            lmap[pair.second] = pair.first
        }
        laterals.add(Lateral(lateralTableAlias, oql, property, false, linkedCaseInsMapOf(*pairs)))
        return this
    }

    fun isLateral(alias: String): Boolean {
        return laterals.any { l -> l.table == alias }
    }

    fun isRoot() = parentField == null

    override fun where(oql: String): DataProjectionF<T> {
        return super.where(oql) as DataProjectionF<T>
    }

    override fun filter(exp: exp?): DataProjectionF<T> {
        return super.filter(exp) as DataProjectionF<T>
    }

    override fun filter(aaa: (m: Unit) -> exp): DataProjectionF<T> {
        return super.filter(aaa) as DataProjectionF<T>
    }

    fun textFilter(exp: exp?): DataProjectionF<T> {
        if (exp == null)
            return this
        textFilter = if (textFilter == null) exp else (textFilter!! and exp)
        return this
    }

    fun textFilter(aaa: (m: Unit) -> exp): DataProjectionF<T> {
        val exp = aaa(Unit)
        textFilter = if (textFilter == null) exp else (textFilter!! and exp)
        return this
    }

    override fun order(vararg fields: f): DataProjectionF<T> {
        return super.order(*fields) as DataProjectionF<T>
    }

    override fun order(vararg fields: Field<*, *>): DataProjectionF<T> {
        return super.order(*fields) as DataProjectionF<T>
    }

    override fun limit(l: Int): DataProjectionF<T> {
        return super.limit(l) as DataProjectionF<T>
    }

    override fun offset(l: Int): DataProjectionF<T> {
        return super.offset(l) as DataProjectionF<T>
    }

    override fun param(k: String, v: Any?): DataProjectionF<T> {
        return super.param(k, v) as DataProjectionF<T>
    }

    override fun groupBy(vararg fields: f): DataProjectionF<T> {
        return super.groupBy(*fields) as DataProjectionF<T>
    }

    override fun groupBy(vararg fields: Field<*, *>): DataProjectionF<T> {
        return super.groupBy(*fields) as DataProjectionF<T>
    }

    @Deprecated("используйте option().withSubSelect")
    fun asSelect(): DataProjectionF<T> {
        option().withSubSelect()
        return this
    }

    fun option(): ProjectionOptions {
        if (options == null)
            options = ProjectionOptions()
        return options!!
    }

    fun onDate(date: Date): DataProjectionF<T> {
        val f = fieldSet
        onDate = date
        return this
    }

    fun withJoinFilter(e: Expression): DataProjectionF<T> {
        joinFilter = e
        return this
    }

    infix fun UNION(projection: DataProjection): UnionProjection {
        return UnionProjection().apply {
            addProjection(this@DataProjectionF)
            addProjection(projection)
        }
    }

    override fun copy(): DataProjectionF<T> {

        val dp = DataProjectionF<T>(this.entity, this.id)
        dp.queryAlias = this.queryAlias
        dp.parentField = this.parentField
        dp.groups = this.groups.toMutableList()
        dp.fields = CaseInsensitiveKeyMap.create(this.fields)
        dp.formulas = this.formulas.toMutableMap()
        dp.laterals = this.laterals.toMutableList()
        dp.params = this.params.toMutableMap()
        dp.filter = this.filter//?.copy() //чтобы не нагружать js не будем делать глубокое копирование фильтра
        dp.oql = this.oql
        dp._orders = this.orders().toMutableList()//не будем  делать глубокое копирование
        dp.limit = this.limit
        dp.offset = this.offset
        dp.options = this.options?.copy()//понадобится, если будет глубокое копирование ов fields
        dp.textFilter = this.textFilter
        dp.onDate = this.onDate
        return dp
    }


}

typealias projection = Projection

class Lateral(
        val table: String,
        val sql: String,
        val embeddedProperty: String?,
        val isList: Boolean = false,
        val mappings: MutableMap<String, String> = mutableMapOf())


fun slice(f: String) = DataProjectionF<UndefinedFieldSet>(null, f)
fun slice(f: KCallable<*>) = slice(f.name)
fun slice(f: Field<*, *>) = slice(f.n)

fun <L : FieldSet> slice(f: () -> Field<*, DataMapF<L>>) = DataProjectionF<L>(null, f)

//TODO: здесб же можно сделать и INTERSECT, EXCEPT
class UnionProjection : BaseProjection() {

    var projections = mutableListOf<DataProjection>()

    fun addProjection(projection: DataProjection): UnionProjection {
        projections.add(projection)
        return this
    }

    //TODO: тест
    infix fun UNION(projection: DataProjection): UnionProjection {
        projections.add(projection)
        return this
    }

    override fun filter(exp: exp?): UnionProjection {
        return super.filter(exp) as UnionProjection
    }

    override fun filter(aaa: (m: Unit) -> exp): UnionProjection {
        return super.filter(aaa) as UnionProjection
    }

    override fun where(oql: String): UnionProjection {
        return super.where(oql) as UnionProjection
    }

    override fun param(k: String, v: Any?): UnionProjection {
        return super.param(k, v) as UnionProjection
    }

    override fun order(vararg fields: f): UnionProjection {
        return super.order(*fields) as UnionProjection
    }

    override fun order(vararg fields: Field<*, *>): UnionProjection {
        return super.order(*fields) as UnionProjection
    }

    override fun limit(l: Int): UnionProjection {
        return super.limit(l) as UnionProjection
    }

    override fun offset(l: Int): UnionProjection {
        return super.offset(l) as UnionProjection
    }

    override fun copy(): UnionProjection {
        val dp = UnionProjection()
        dp.params = this.params.toMutableMap()
        dp.filter = this.filter//?.copy() //чтобы не нагружать js не будем делать глубокое копирование фильтра
        dp.oql = this.oql
        dp._orders = this.orders().toMutableList()//не будем  делать глубокое копирование
        dp.limit = this.limit
        dp.offset = this.offset
        dp.projections = this.projections
        return dp
    }
}


sealed class Expression {

    infix fun or(exp: exp): exp {
        return OR(this, exp)
    }

    infix fun or(righta: (m: Unit) -> exp): exp {
        return OR(this, righta(Unit))
    }


    infix fun and(exp: exp): exp {
        return AND(this, exp)
    }

    infix fun and(righta: (m: Unit) -> exp): exp {
        return AND(this, righta(Unit))
    }

    infix fun gt(exp1: Any): exp {
        return bop(exp1, Operation.GT)
    }

    infix fun ge(exp1: Any): exp {
        return bop(exp1, Operation.GE)
    }

    infix fun le(exp1: Any): exp {
        return bop(exp1, Operation.LE)
    }

    infix fun lt(exp1: Any): exp {
        return bop(exp1, Operation.LT)
    }

    infix fun eq(exp1: Any): exp {
        return bop(exp1, Operation.EQ)
    }

    infix fun neq(exp1: Any): exp {
        return bop(exp1, Operation.NEQ)
    }

    infix fun like(exp1: Any): exp {
        return bop(exp1, Operation.LIKE)
    }

    infix fun ilike(exp1: Any): exp {
        return BinaryOP(Upper(this), Upper(exp1), Operation.LIKE)
    }

    infix fun IS(nul: NULL): exp {
        return bop(nul, Operation.IS_NULL)
    }

    infix fun ISNOT(nul: NULL): exp {
        return bop(nul, Operation.IS_NOT_NULL)
    }

    infix fun IN(list: List<*>): exp {
        return if (list.isNotEmpty()) bop(list, Operation.IN) else alwaysFalse()
    }

    infix fun IN(list: Array<*>): exp {
        return if (list.isNotEmpty()) bop(list, Operation.IN) else alwaysFalse()
    }

    infix fun between(pairs: Pair<Any, Any>): exp {
        return ((this IS NULL) or (this ge pairs.first)) and ((this IS NULL) or (this le pairs.second))
    }

    private fun bop(right: Any, op: Operation): exp = when {
        (this is ExpressionField && right is DataMap) -> {
            BinaryOP(idField(name), value(right.id), op)
        }
        (this is ExpressionField && right is value && right.v is DataMap) -> {
            BinaryOP(idField(name), value(right.v.id), op)
        }
        else -> BinaryOP(this, right as? exp
                ?: value(right), op)
    }

    abstract fun copy(): Expression

}
typealias exp = Expression

fun conjunction(vararg expressions: exp?): exp? {
    return expressions.reduce { exp1, exp2 -> conjunction(exp1, exp2) }
}

fun conjunction(expressions: Collection<exp?>) = conjunction(*expressions.toTypedArray())

fun conjunction(exp1: exp?, exp2: exp?): exp? {
    return if (exp1 != null && exp2 != null) {
        exp1 and exp2
    } else {
        exp1 ?: exp2
    }
}

fun disjunction(vararg expressions: exp?): exp? {
    return expressions.takeIf { it.isNotEmpty() }?.reduce { exp1, exp2 -> disjunction(exp1, exp2) }
}

fun disjunction(expressions: Collection<exp?>) = disjunction(*expressions.toTypedArray())

fun disjunction(exp1: exp?, exp2: exp?): exp? {
    return if (exp1 != null && exp2 != null) {
        exp1 or exp2
    } else {
        exp1 ?: exp2
    }
}

fun alwaysFalse() = NULL ISNOT NULL

fun idField(name: String) = f("$name.id")

private fun extractField(exp: exp): exp {
    if (exp is Field<*, *>)
        return f(exp.n)
    return exp
}

data class ExpressionField(val name: String) : exp() {

    constructor(d: Field<*, *>) : this(d.n)

    constructor(d: ExpressionField) : this(d.name) {
        this.isasc = d.isasc
    }

    var isasc = true

    fun asc_(): f {
        isasc = true
        return this
    }

    fun desc(): f {
        isasc = false
        return this
    }

    override fun copy(): Expression {
        return f(this)
    }
}

typealias f = ExpressionField


data class ExpressionValue(val v: Any?) : exp() {
    override fun copy(): Expression {
        return this
    }
}

data class ExpressionParam(val v: Any?) : exp() {
    override fun copy(): Expression {
        return this
    }
}

typealias value = ExpressionValue
typealias param = ExpressionParam

class BinaryOP(left: exp, right: exp, val op: Operation) : exp() {
    val left: exp = extractField(left)
    val right: exp = extractField(right)

    override fun copy(): Expression {
        return BinaryOP(this.left.copy(), this.right.copy(), op)
    }
}

class OR(left: exp, right: exp) : exp() {
    val left: exp = extractField(left)
    val right: exp = extractField(right)

    override fun copy(): Expression {
        return OR(this.left.copy(), this.right.copy())
    }
}

data class NOT(val right: exp) : exp() {
    override fun copy(): Expression {
        return NOT(this.right.copy())
    }
}


class AND(left: exp, right: exp) : exp() {
    val left: exp = extractField(left)
    val right: exp = extractField(right)

    override fun copy(): Expression {
        return AND(this.left.copy(), this.right.copy())
    }
}

object NULL : exp() {
    override fun copy(): Expression {
        return this
    }
}

open class ExpFunction(val name: String, args: List<Any>) : exp() {

    val arguments: List<exp>

    override fun copy(): Expression {
        return ExpFunction(name, arguments.map { it.copy() }.toList())
    }

    init {
        arguments = args.map {
            val ex = if (it is exp) it else value(it)
            extractField(ex)
        }.toList()
    }
}

class Upper(val string: Any) : ExpFunction("UPPER", listOf(string))

open class OqlExpression(oql: String, params: Map<String, Any?> = emptyMap()) : exp() {

    val oql: String = oql

    val params: Map<String, Expression> = params.mapValues { (_, value) -> value as? exp
            ?: value(value)
    }

    override fun copy(): Expression {
        return OqlExpression(oql, params.mapValues { it.value.copy() })
    }
}

infix fun (() -> exp).and(function: () -> exp): exp {
    return AND(this(), function())
}

infix fun (() -> exp).and(exp: exp): exp {
    return AND(this(), exp)
}

infix fun (exp).and(exp: exp): exp {
    return AND(this, exp)
}

infix fun (() -> exp).or(function: () -> exp): exp {
    return OR(this(), function())
}

infix fun (exp).OR(function: exp): exp {
    return OR(this, function)
}

infix fun (() -> exp).or(exp: exp): exp {
    return OR(this(), exp)
}

fun not(function: () -> exp): exp {
    return NOT(function())
}

fun not(exp: exp): exp {
    return NOT(exp)
}

fun on(name: String): DataProjection {
    return DataProjectionF<UndefinedFieldSet>(name)
}

fun on(entity: KClass<*>): DataProjection {
    return DataProjectionF<UndefinedFieldSet>(entity)
}

fun <T : FieldSet> on(t: T): DataProjectionF<T> {
    return DataProjectionF<T>(t)
}

enum class JoinType {
    JOIN,
    SELECT
}

enum class RetrieveType {
    Usual,
    JustId,
    SimpleField,
    Recursive
}

enum class Operation(val value: String) {
    EQ("="),
    NEQ("<>"),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
    LIKE("like"),
    ILIKE("ilike"),
    IS_NULL("is"),
    IS_NOT_NULL("is not"),
    IN("in"),
    BETWEEN("between");

    companion object {
        private val compareOps = listOf(GT, LT, LE, GE)

        fun fromValue(v:String): Operation
        {
            return when(v) {
                "="-> EQ
                "<>"-> NEQ
                ">"-> GT
                ">="-> GE
                "<"-> LT
                "<="-> LE
                "like"-> LIKE
                "ilike"-> ILIKE
                "is" -> IS_NULL
                "is not"-> IS_NOT_NULL
                "in"-> IN
                "between"-> BETWEEN
                else->TODO()
            }
        }
    }

    fun isCompareOp() = this in compareOps
}