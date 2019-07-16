package com.bftcom.ice.server.tools

import org.apache.commons.io.FileUtils
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.Services
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.util.*
import javax.script.*

private const val SCRIPT_FUN = "__SCRIPT__FUNCTION__"

@Service
class KotlinJsCompiler {

    @Autowired
    lateinit var sources: Sources

    private val webLibs: String by lazy {
        (javaClass.classLoader as? URLClassLoader)?.urLs
                ?.map { it.file }
                ?.filter { it.contains("web/build/classes/kotlin/main")
                        || it.contains("/WEB-INF/classes")
                        || it.endsWith("-web.jar")
                        || it.endsWith("kotlin-stdlib-js.jar")
                }?.joinToString(File.pathSeparator)
                .orEmpty()
    }

    fun compileToJsScript(ktScript: String, vararg variables: String): String {
        return compileToJsScript(ktScript, emptyList(), *variables)
    }

    fun compileToJsScript(ktScript: String, imports: List<String>, vararg variables: String): String {
        return compileToJsScript(ktScript, imports, variables.toList())
    }

    fun compileToJsScript(ktScript: String, imports: List<String>, variables: List<String>): String {

        val script = prepareScript(ktScript, imports, variables)

        val ktScriptFirstLine= imports.size + 1

        val fileName = UUID.randomUUID().toString()

        val ktFile = File(sources.dotIceJsDir, "$fileName.kt")

        val jsFile = File(sources.dotIceJsDir, "$fileName.js")

        val compiler = K2JSCompiler()

        val args = arrayOf(
                "-no-stdlib",
                "-module-kind", "amd",
                "-output", jsFile.absolutePath,
                "-libraries", webLibs,
                ktFile.absolutePath
        )
        val messageCollector = KeepFirstErrorMessageCollector(System.out)

        val arguments = compiler.createArguments().also { compiler.parseArguments(args, it) }

        try {
            ktFile.writeText(script)

            compiler.exec(messageCollector, Services.EMPTY, arguments)

            if (messageCollector.hasErrors()) {
                val errorLocation = messageCollector.getErrorLocation()

                if (errorLocation != null) {
                    throw ScriptException(messageCollector.getErrorMessage(), null, errorLocation.line - ktScriptFirstLine, errorLocation.column)
                } else {
                    throw ScriptException(messageCollector.getErrorMessage())
                }
            }
            val jsScript = jsFile.readText()

            return extractScriptFunctionBody(jsScript)
        } finally {
            FileUtils.deleteQuietly(ktFile)
            FileUtils.deleteQuietly(jsFile)
        }
    }

    private fun prepareScript(ktScript: String, imports: List<String>, variables: List<String>): String {
        val ktScriptLines = ktScript.lines()
        val script = arrayListOf<String>()

        val parameters = variables.map { if (it.contains(':')) it else "$it : dynamic" }.joinToString { it }

        script.addAll(imports.map { "import $it" })
        script.addAll(ktScriptLines.filter { it.startsWith("import ") })
        script.add("fun $SCRIPT_FUN($parameters) {")
        script.addAll(ktScriptLines.filter { !it.startsWith("import ") })
        script.add("}")

        return script.joinToString("\n")
    }

    private fun extractScriptFunctionBody(script: String): String {
        val scriptLines = script.lines()
        var startVarsLine = -1
        var startFunLine = -1
        var endFunLine = -1

        scriptLines.forEachIndexed { index, line ->
            val lineTrimmed = line.trimStart()
            when {
                (lineTrimmed.startsWith("'use strict'")) -> startVarsLine = index
                (lineTrimmed.startsWith("function $SCRIPT_FUN")) -> startFunLine = index
                (lineTrimmed.startsWith("_.$SCRIPT_FUN")) -> endFunLine = index
            }
        }
        if (startVarsLine == -1 || startFunLine == -1 || endFunLine == -1) {
            throw ScriptException("Неверное объявление функции скрипта")
        }
        val varsLines = scriptLines.subList(startVarsLine, startFunLine)
        val funBodyLines = scriptLines.subList(startFunLine + 1, endFunLine - 1)

        return (varsLines + funBodyLines).joinToString("\n")
    }
}

data class ScriptVar(val name:String, val variable: Any?,
                     val type:String = buildTypeName(variable))

typealias Var  = ScriptVar

private fun buildTypeName(value:Any?): String {
    if(value==null) return "Any?"
    val f = value::class
    val n = value::class.simpleName
    return if (f.typeParameters.isNotEmpty())
        return "$n<*>"
    else n!!
}

@Service
//в отличие от KotlinJvmScriptTool
//данный сервис импортирует в скрипты айса и прикладноого приложения по умолчанию
class ServerScriptService {

    @Autowired
    private
    lateinit var kotlinJvmScriptTool: KotlinJvmScriptTool

    @Value("\${ice.script.jvmPackages:" +
            "com.bftcom.ice.common.maps.*,\n" +
            "com.bftcom.ice.server.util.*,\n" +
            "com.bftcom.ice.common.general.*}")
    private var jvmPackages: List<String> = listOf()

    fun execute(ktScript: String, vararg variable: Var): Any? {
        return kotlinJvmScriptTool.prepareAndExecute(ktScript,
                jvmPackages,
                *variable)
    }

    fun executePreparedScript(ktScript: String, vararg vars: Var): Any? {
        return kotlinJvmScriptTool.executePreparedScript(ktScript, vars.associate { it.name to it.variable })
    }
}

@Service
class KotlinJvmScriptTool {
    init {
        //устраняет ворнинг в консоли (отсюда: https://github.com/arturbosch/detekt/issues/630)
        System.setProperty("idea.use.native.fs.for.win", "false")
    }

    fun prepareAndExecute(ktScript: String, vararg variable: Var): Any? {
        val imports = emptyList<String>()
        return prepareAndExecute(ktScript, imports, *variable)
    }

    fun prepareAndExecute(ktScript: String, imports: List<String>, vararg vars: Var): Any? {

        val variables = vars.map {
            it.name + ":" + it.type
        }.toTypedArray()


        val s = prepareServerScript(ktScript, imports, *variables)

        return executePreparedScript(s, vars.map { it.name to it.variable }.toMap().toMutableMap())
    }

    fun executePreparedScript(ktScript: String, vararg variable: Pair<String, Any?>): Any? {
        return executePreparedScript(ktScript, mapOf(*variable))
    }

    fun executePreparedScript(ktScript: String, bindings: Map<String, Any?>): Any? {

        //todo: надо вспомнить  как корретно инициализировать скрипт-машину
        //можно ли ее использовать одну на много пользователей   одновременно (мудрым применением скопов)
        val engine = getScriptEngine()

        return executePreparedScript(engine, ktScript, bindings)
    }

    fun executePreparedScript(engine:ScriptEngine, ktScript: String, bindings: Map<String, Any?>): Any? {

        //todo: надо вспомнить  как корретно инициализировать скрипт-машину
        //можно ли ее использовать одну на много пользователей   одновременно (мудрым применением скопов)

        with(engine) {
            engine.setBindings(SimpleBindings(bindings), ScriptContext.ENGINE_SCOPE)
            val res =  engine.eval(ktScript)
            return res
        }
    }

    fun executePreparedScripts(ktScript: List<String>, bindings: Map<String, Any?>): List<Any?> {

        //todo: надо вспомнить  как корретно инициализировать скрипт-машину
        //можно ли ее использовать одну на много пользователей   одновременно (мудрым применением скопов)
        val engine = getScriptEngine()

        with(engine) {
            engine.setBindings(SimpleBindings(bindings), ScriptContext.ENGINE_SCOPE)
            return ktScript.map { engine.eval(it) }
        }
    }




    fun prepareServerScript(ktScript: String, vararg variables: String): String {
        return prepareServerScript(ktScript, emptyList(), *variables)
    }

    fun prepareServerScript(ktScript: String, imports: List<String>, vararg variables: String): String {
        return prepareServerScript(ktScript, imports, variables.toList())
    }

    fun prepareServerScript(ktScript: String, imports: List<String>, variables: List<String>): String {

        var nKtScript = ktScript
        var imports0 = ""

        if (ktScript.contains("import ")) {
            val start = ktScript.indexOf("import")
            val last = ktScript.lastIndexOf("import")
            val last1 = ktScript.indexOf("\n", last)+1

            imports0 = ktScript.substring(start, last1)
            nKtScript = ktScript.substring(0, start) + ktScript.substring(last1)
        }

        val imports1 =  imports0 + imports.map { "import $it" }.joinToString(separator = "\n")
        val vars0 = variables.map { v ->
            val name = if (v.contains(':')) v.substringBefore(':') else v
            val type = if (v.contains(':')) v.substringAfter(':') else "Any?"
            //todo сделать системно
            if(type.trim()=="List<Any?>"){
                """val $name = (if(bindings["$name"]==null) emptyList<Any?>() else bindings["$name"]) as $type"""
            }
            else
            """val $name = bindings["$name"] as $type"""
        }.joinToString("\n")

        return "$imports1\n$vars0\n$nKtScript"
    }

    fun compileToJvmScript(ktScript: String, imports: List<String>, variables: List<String>): String {
        try {
            val prepareServerScript = prepareServerScript(ktScript, imports, variables)
            (getScriptEngine() as Compilable).compile(prepareServerScript)
            return prepareServerScript
        } catch (e: ScriptException) {
            throw ScriptException(e.message, null, e.lineNumber - (imports.size + variables.size), e.columnNumber)
        }
    }
}

fun getScriptEngine() = ScriptEngineManager().getEngineByExtension("kts")

internal class KeepFirstErrorMessageCollector(compilerMessagesStream: PrintStream) : MessageCollector {
    private val innerCollector = PrintingMessageCollector(compilerMessagesStream, MessageRenderer.WITHOUT_PATHS, false)
    private var firstErrorMessage: String? = null
    private var firstErrorLocation: CompilerMessageLocation? = null

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        if (firstErrorMessage == null && severity.isError) {
            firstErrorMessage = message
            firstErrorLocation = location
        }
        innerCollector.report(severity, message, location)
    }

    fun getErrorMessage() = firstErrorMessage

    fun getErrorLocation() = firstErrorLocation

    override fun hasErrors(): Boolean = innerCollector.hasErrors()

    override fun clear() {
        innerCollector.clear()
    }
}