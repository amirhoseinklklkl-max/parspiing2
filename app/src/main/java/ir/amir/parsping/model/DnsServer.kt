package ir.amir.parsping.model

data class DnsServer(
    val id: String,
    val name: String,
    val primary: String,
    val secondary: String?,
    val category: DnsCategory,
    val isCustom: Boolean = false
)
