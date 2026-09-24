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
        DnsServer("cloudflare-gaming", "Cloudflare", "1.1.1.1", "1.0.0.1", DnsCategory.GAMING),
        DnsServer("google-gaming", "Google", "8.8.8.8", "8.8.4.4", DnsCategory.GAMING),
        DnsServer("quad9-gaming", "Quad9", "9.9.9.9", "149.112.112.112", DnsCategory.GAMING),
        DnsServer("opendns-gaming", "OpenDNS", "208.67.222.222", "208.67.220.220", DnsCategory.GAMING),
        DnsServer("adguard-gaming", "AdGuard DNS", "94.140.14.14", "94.140.15.15", DnsCategory.GAMING),
        DnsServer("alternate-gaming", "Alternate DNS", "76.76.19.19", "76.223.122.150", DnsCategory.GAMING),
        DnsServer("level3-gaming", "Level3", "4.2.2.1", "4.2.2.2", DnsCategory.GAMING),
        DnsServer("dnswatch-gaming", "DNS.WATCH", "84.200.69.80", "84.200.70.40", DnsCategory.GAMING),
    )

    val antiCensorship = listOf(
        DnsServer("cloudflare-anticensor", "Cloudflare", "1.1.1.1", "1.0.0.1", DnsCategory.ANTI_CENSORSHIP),
        DnsServer("google-anticensor", "Google", "8.8.8.8", "8.8.4.4", DnsCategory.ANTI_CENSORSHIP),
        DnsServer("opendns-anticensor", "OpenDNS", "208.67.222.222", "208.67.220.220", DnsCategory.ANTI_CENSORSHIP),
        DnsServer("adguard-anticensor", "AdGuard DNS", "94.140.14.14", "94.140.15.15", DnsCategory.ANTI_CENSORSHIP),
        DnsServer("quad9-anticensor", "Quad9", "9.9.9.9", "149.112.112.112", DnsCategory.ANTI_CENSORSHIP),
        DnsServer("dnswatch-anticensor", "DNS.WATCH", "84.200.69.80", "84.200.70.40", DnsCategory.ANTI_CENSORSHIP),
        DnsServer("comodo-anticensor", "Comodo Secure DNS", "8.26.56.26", "8.20.247.20", DnsCategory.ANTI_CENSORSHIP),
        DnsServer("cleanbrowsing-anticensor", "CleanBrowsing", "185.228.168.9", "185.228.169.9", DnsCategory.ANTI_CENSORSHIP),
        DnsServer("freenom-anticensor", "Freenom World", "80.80.80.80", "80.80.81.81", DnsCategory.ANTI_CENSORSHIP),
    )

    val all: List<DnsServer> get() = general + gaming + antiCensorship
}
