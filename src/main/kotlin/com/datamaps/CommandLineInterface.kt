package com.datamaps

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.JLineShellComponent
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.plugin.BannerProvider
import org.springframework.shell.plugin.HistoryFileNameProvider
import org.springframework.shell.plugin.support.DefaultPromptProvider
import org.springframework.shell.support.util.FileUtils
import org.springframework.shell.support.util.OsUtils
import org.springframework.shell.support.util.VersionUtils
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.Resource



@Order(Ordered.HIGHEST_PRECEDENCE)
class CliService
{
    @Resource
    lateinit var jLineShellComponent: JLineShellComponent

    @PostConstruct
    fun init()
    {
        jLineShellComponent.start()
    }

}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class PromptProvider : HistoryFileNameProvider, DefaultPromptProvider()
{

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

    @CliCommand(value = "hw", help = "say hello world")
    fun insert() {
        println("hello wolrd, from datamaps")
    }
}