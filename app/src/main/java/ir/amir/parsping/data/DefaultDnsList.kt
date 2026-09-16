package ir.amir.parsping.data

import ir.amir.parsping.model.DnsCategory
import ir.amir.parsping.model.DnsServer

object DefaultDnsList {

    val general = listOf(
        DnsServer("cloudflare", "Cloudflare", "1.1.1.1", "1.0.0.1", DnsCategory.GENERAL),
        DnsServer("google", "Google", "8.8.8.8", "8.8.4.4", DnsCategory.GENERAL),
        DnsServer("quad9", "Quad9", "9.9.9.9", "149.112.112.112", DnsCategory.GENERAL),
        DnsServer("opendns", "OpenDNS", "208.67.222.222", "208.67.220.220", DnsCategory.GENERAL),
    )

    val gaming = listOf(
        DnsServer("radar", "رادار گیم", "10.202.10.10", "10.202.10.11", DnsCategory.GAMING),
        DnsServer("shecan-gaming", "شکن", "178.22.122.100", "185.51.200.2", DnsCategory.GAMING),
        DnsServer("cloudflare-gaming", "Cloudflare", "1.1.1.1", "1.0.0.1", DnsCategory.GAMING),
        DnsServer("google-gaming", "Google", "8.8.8.8", "8.8.4.4", DnsCategory.GAMING),
        DnsServer("quad9-gaming", "Quad9", "9.9.9.9", "149.112.112.112", DnsCategory.GAMING),
        DnsServer("opendns-gaming", "OpenDNS", "208.67.222.222", "208.67.220.220", DnsCategory.GAMING),
    )

    val antiCensorship = listOf(
        DnsServer("shecan", "شکن", "178.22.122.100", "185.51.200.2", DnsCategory.ANTI_CENSORSHIP),
    )

    val all: List<DnsServer> get() = general + gaming + antiCensorship
}
