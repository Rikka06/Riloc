package com.riloc.app.data.repository

import com.riloc.app.data.model.TemplateInfo
import com.riloc.app.ui.util.getAppProfileTemplate
import com.riloc.app.ui.util.listAppProfileTemplates
import com.riloc.app.ui.util.setAppProfileTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TemplateRepositoryImpl : TemplateRepository {
    override suspend fun getTemplates(sync: Boolean): Result<List<TemplateInfo>> = withContext(Dispatchers.IO) {
        Result.success(listAppProfileTemplates().mapNotNull { getTemplateInfoById(it) })
    }

    override suspend fun importTemplates(jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val array = runCatching { JSONArray(jsonString) }
                .getOrElse { JSONArray().apply { put(JSONObject(jsonString)) } }
            (0 until array.length()).forEach { i ->
                val template = array.getJSONObject(i)
                val id = template.getString("id")
                template.put("local", true)
                setAppProfileTemplate(id, template.toString())
            }
        }
    }

    override suspend fun exportTemplates(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            JSONArray(
                listAppProfileTemplates()
                    .mapNotNull { getTemplateInfoById(it) }
                    .filter { it.local }
                    .map { it.toJSON() }
            ).toString()
        }
    }

    override suspend fun getTemplate(id: String): Result<TemplateInfo> = withContext(Dispatchers.IO) {
        runCatching { getTemplateInfoById(id) ?: error("Template not found: $id") }
    }

    private fun getTemplateInfoById(id: String): TemplateInfo? =
        runCatching { TemplateInfo.fromJSON(JSONObject(getAppProfileTemplate(id))) }.getOrNull()
}

