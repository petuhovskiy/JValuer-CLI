package com.petukhovsky.jvaluer.cli

import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import java.io.StringReader
import java.io.StringWriter

val freemarkerConfig = Configuration(Configuration.VERSION_2_3_23).apply {
    defaultEncoding = "UTF-8"
    templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
    logTemplateExceptions = false
}

fun processTemplate(template: String, model: Map<String, Any>): String {
    val t = Template("template", StringReader(template), freemarkerConfig)

    val writer = StringWriter()
    t.process(model, writer)

    return writer.toString()
}