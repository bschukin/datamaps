package com.datamaps

import com.datamaps.services.DmUtilService
import com.datamaps.tools.BBAgent
import com.datamaps.tools.FieldSetGenerator
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.JLineShellComponent
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.shell.plugin.BannerProvider
import org.springframework.shell.plugin.HistoryFileNameProvider
import org.springframework.shell.plugin.support.DefaultPromptProvider
import org.springframework.shell.support.util.FileUtils
import org.springframework.shell.support.util.OsUtils
import org.springframework.shell.support.util.VersionUtils
import org.springframework.stereotype.Component
import javax.annotation.PreDestroy
import javax.annotation.Resource


@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
class CliService  {

    @Resource
    lateinit var jLineShellComponent: JLineShellComponent

    fun startCommandLine()
    {
        jLineShellComponent.start()
    }

    @PreDestroy
    fun stop() {
        jLineShellComponent.stop()
    }
}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class PromptProvider : HistoryFileNameProvider, DefaultPromptProvider() {

    override fun getHistoryFileName(): String {
        return "datamaps-cli.log"
    }

    override fun getPrompt(): String {
        return "datamaps2"
    }

    override fun getProviderName(): String {
        return "datamaps"
    }

}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DMBannerProvider : BannerProvider {

    override fun getBanner(): String {
        val sb = StringBuilder()
        sb.append(FileUtils.readBanner(DMBannerProvider::class.java, "banner.txt"))
        sb.append(OsUtils.LINE_SEPARATOR)
        sb.append(":: datamaps ::\t(0.1)").append(OsUtils.LINE_SEPARATOR)
        return sb.toString()
    }

    override fun getVersion(): String {
        return VersionUtils.versionInfo()
    }

    override fun getWelcomeMessage(): String {
        return "Welcome to Datamaps"
    }

    override fun getProviderName(): String {
        return "datamaps"
    }


}

@Component
class DataMapsCommands : CommandMarker {

    @Resource
    lateinit var fieldSetGenerator: FieldSetGenerator

    @Resource
    lateinit var dmUtilService: DmUtilService

    /*** Команда "Сгенерировать FieldSet по имени таблицы"
     */
    @CliCommand(value = ["generate", "gene", "g"])
    fun generateFieldSet(
            @CliOption(key = ["table", "t"])
            table: String
    ) {
        println(fieldSetGenerator.generateFieldSet(table))
        println("gene --t $table")//чтобы руками не вдалбливать повторно
    }

    /***
     * Команда "рефреш фиелдсет" используется перегрузки класса фиелдсета и обновления состояния
     * objectInstance фиелдсета. В качестве имени можно использовать неполное имя класса (любого кейса,
     * поиск производится по endsWithIgnoreCase)
     */
    @CliCommand(value = ["refresh", "refr", "r"])
    fun refreshDM(
            @CliOption(key = ["class", "c"])
            c: String
    ) {
        BBAgent.refreshDataMapping(c)
        println("refr --c $c")
    }

    /**
     * Команда "reload" используется для загрузки / перегрузки класса
     */
    @CliCommand(value = ["reload", "rl"])
    fun refreshClass(
            @CliOption(key = ["class", "c"])
            c: String
    ) {
        BBAgent.refreshClass(c)
        println("rl --c $c")
    }

    /***
     * Команда "очистить кеши" - сбрасывает все наши кеши (маппинги, определения таблиц и тому подобное)
     */
    @CliCommand(value = ["clearCache", "cc"])
    fun clearCaches() {
        dmUtilService.clearCaches()
        println("caches are clear!")
    }
}