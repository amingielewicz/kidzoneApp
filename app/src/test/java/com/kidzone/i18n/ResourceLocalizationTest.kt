package com.kidzone.i18n

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ResourceLocalizationTest {

    private val resDir = findResDir()
    private val defaultValuesDir = File(resDir, "values")
    private val enValuesDir = File(resDir, "values-en")

    private fun findResDir(): File {
        val root = File(".")
        val appRes = File(root, "app/src/main/res")
        if (appRes.exists()) return appRes
        val srcRes = File(root, "src/main/res")
        if (srcRes.exists()) return srcRes
        error("Resource directory not found. CWD: ${root.absolutePath}")
    }

    private val formatRegex = Regex("%(\\d+\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])")

    @Test
    fun `verify all resources are localized and consistent`() {
        check(defaultValuesDir.exists()) {
            "Default values directory not found at ${defaultValuesDir.absolutePath}. CWD: ${File(".").absolutePath}"
        }
        val defaultFiles = defaultValuesDir.listFiles { _, name -> 
            name.endsWith(".xml") && !name.contains("colors") && !name.contains("themes") 
        } ?: emptyArray()

        val errors = mutableListOf<String>()

        defaultFiles.forEach { defaultFile ->
            compareFiles(defaultFile, errors)
        }

        if (errors.isNotEmpty()) {
            val message = "Found ${errors.size} localization errors:\n\n" + 
                    errors.joinToString("\n\n")
            assertTrue(false, message)
        }
    }

    private fun compareFiles(defaultFile: File, errors: MutableList<String>) {
        val fileName = defaultFile.name
        val enFile = File(enValuesDir, fileName)

        if (!enFile.exists()) {
            errors.add("File $fileName exists in 'values' but is missing in 'values-en'")
            return
        }

        val defaultResources = parseResources(defaultFile)
        val enResources = parseResources(enFile)

        checkMissingKeys(fileName, defaultResources, enResources, errors)
        checkOrphanedKeys(fileName, defaultResources, enResources, errors)
    }

    private fun checkMissingKeys(
        fileName: String,
        defaultResources: Map<String, List<String>>,
        enResources: Map<String, List<String>>,
        errors: MutableList<String>
    ) {
        defaultResources.forEach { (key, defaultArgs) ->
            if (!enResources.containsKey(key)) {
                errors.add("Key '$key' in $fileName is missing from 'values-en/$fileName'")
            } else {
                val enArgs = enResources[key] ?: emptyList()
                if (defaultArgs != enArgs) {
                    errors.add(
                        "Format arguments mismatch for key '$key' in $fileName.\n" +
                                "  Default: $defaultArgs\n" +
                                "  EN:      $enArgs"
                    )
                }
            }
        }
    }

    private fun checkOrphanedKeys(
        fileName: String,
        defaultResources: Map<String, List<String>>,
        enResources: Map<String, List<String>>,
        errors: MutableList<String>
    ) {
        enResources.keys.forEach { key ->
            if (!defaultResources.containsKey(key)) {
                errors.add(
                    "Key '$key' exists in 'values-en/$fileName' but is " +
                            "missing from default 'values/$fileName'"
                )
            }
        }
    }

    private fun parseResources(file: File): Map<String, List<String>> {
        val resources = mutableMapOf<String, List<String>>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()

        // Parse strings
        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val node = strings.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                val name = element.getAttribute("name")
                val content = element.textContent
                resources[name] = extractFormatArgs(content)
            }
        }

        // Parse plurals
        val plurals = doc.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val node = plurals.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                val name = element.getAttribute("name")
                val items = element.getElementsByTagName("item")
                val allArgs = mutableListOf<String>()
                for (j in 0 until items.length) {
                    val item = items.item(j) as Element
                    allArgs.addAll(extractFormatArgs(item.textContent))
                }
                // Sort to compare unique args presence regardless of which plural item they are in
                resources[name] = allArgs.distinct().sorted()
            }
        }

        return resources
    }

    private fun extractFormatArgs(text: String): List<String> {
        return formatRegex.findAll(text).map { it.value }.toList().sorted()
    }
}
