package ir.amir.parsping.data

import android.content.Context
import ir.amir.parsping.model.DnsCategory
import ir.amir.parsping.model.DnsServer
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles persistence of user-added custom DNS servers and the last selected DNS,
 * using SharedPreferences + a small JSON array (no extra dependency needed).
 */
class DnsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("parsping_prefs", Context.MODE_PRIVATE)

    fun loadCustomDns(): List<DnsServer> {
        val raw = prefs.getString(KEY_CUSTOM_LIST, null) ?: return emptyList()
        val arr = JSONArray(raw)
        val result = mutableListOf<DnsServer>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result += DnsServer(
                id = obj.getString("id"),
                name = obj.getString("name"),
                primary = obj.getString("primary"),
                secondary = obj.optString("secondary").takeIf { it.isNotBlank() },
                category = DnsCategory.CUSTOM,
                isCustom = true
            )
        }
        return result
    }

    fun saveCustomDns(list: List<DnsServer>) {
        val arr = JSONArray()
        list.forEach { dns ->
            val obj = JSONObject()
            obj.put("id", dns.id)
            obj.put("name", dns.name)
            obj.put("primary", dns.primary)
            obj.put("secondary", dns.secondary ?: "")
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_LIST, arr.toString()).apply()
    }

    fun addCustomDns(dns: DnsServer) {
        val current = loadCustomDns().toMutableList()
        current += dns
        saveCustomDns(current)
    }

    fun removeCustomDns(id: String) {
        val current = loadCustomDns().filterNot { it.id == id }
        saveCustomDns(current)
    }

    fun saveSelectedDnsId(id: String?) {
        prefs.edit().putString(KEY_SELECTED_ID, id).apply()
    }

    fun loadSelectedDnsId(): String? = prefs.getString(KEY_SELECTED_ID, null)

    companion object {
        private const val KEY_CUSTOM_LIST = "custom_dns_list"
        private const val KEY_SELECTED_ID = "selected_dns_id"
    }
}
