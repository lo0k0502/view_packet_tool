#include <stdio.h>
#include <stdlib.h>
#include <pcap.h>
#include <net/ethernet.h>

#define SIZE_ETHERNET 14


/* Ethernet header */
    struct sniff_ethernet {
        u_char ether_dhost[ETHER_ADDR_LEN]; /* Destination host address */
        u_char ether_shost[ETHER_ADDR_LEN]; /* Source host address */
        u_short ether_type; /* IP? ARP? RARP? etc */
    };

    /* IP header */
    struct sniff_ip {
        u_char ip_vhl;      /* version << 4 | header length >> 2 */
        u_char ip_tos;      /* type of service */
        u_short ip_len;     /* total length */
        u_short ip_id;      /* identification */
        u_short ip_off;     /* fragment offset field */
    #define IP_RF 0x8000        /* reserved fragment flag */
    #define IP_DF 0x4000        /* dont fragment flag */
    #define IP_MF 0x2000        /* more fragments flag */
    #define IP_OFFMASK 0x1fff   /* mask for fragmenting bits */
        u_char ip_ttl;      /* time to live */
        u_char ip_p;        /* protocol */
        u_short ip_sum;     /* checksum */
        struct in_addr ip_src;
        struct in_addr ip_dst; /* source and dest address */
    };
    #define IP_HL(ip)       (((ip)->ip_vhl) & 0x0f)
    #define IP_V(ip)        (((ip)->ip_vhl) >> 4)

    /* TCP header */
    struct sniff_tcp {
        u_short th_sport;   /* source port */
        u_short th_dport;   /* destination port */
        u_int32_t th_seq;       /* sequence number */
        u_int32_t th_ack;       /* acknowledgement number */

        u_char th_offx2;    /* data offset, rsvd */
    #define TH_OFF(th)  (((th)->th_offx2 & 0xf0) >> 4)
        u_char th_flags;
    #define TH_FIN 0x01
    #define TH_SYN 0x02
    #define TH_RST 0x04
    #define TH_PUSH 0x08
    #define TH_ACK 0x10
    #define TH_URG 0x20
    #define TH_ECE 0x40
    #define TH_CWR 0x80
    #define TH_FLAGS (TH_FIN|TH_SYN|TH_RST|TH_ACK|TH_URG|TH_ECE|TH_CWR)
        u_short th_win;     /* window */
        u_short th_sum;     /* checksum */
        u_short th_urp;     /* urgent pointer */
};

int main(int argc, const char * argv[]) {
    char errbuf[PCAP_ERRBUF_SIZE];
    int packetCount = 0,
        isIP = 0,
        i,
        j;
    const u_char *packet;
    struct pcap_pkthdr *header;
    u_int size_ip;

    pcap_t *handle = pcap_open_offline("assets/smallFlows.pcap", errbuf);
    if (!handle) {
        fprintf(stderr, "pcap_open_offline: %s\n", errbuf);
        exit(1);
    }

    //tcp info
    const struct sniff_ethernet *ethernet; /* The ethernet header */
    const struct sniff_ip *ip; /* The IP header */
    const struct sniff_tcp *tcp; /* The TCP header */

    while (pcap_next_ex(handle, &header, &packet) >= 0) {
        // Show the packet number
        printf("Packet # %i", ++packetCount);

        // Show Epoch Time
        printf(", Timestamp: %ld seconds;%ld milliseconds", header->ts.tv_sec, header->ts.tv_usec);

        ethernet = (struct sniff_ethernet *) packet;

        printf(", src MAC address: %02x:%02x:%02x:%02x:%02x:%02x",
            (unsigned)ethernet->ether_shost[0],
            (unsigned)ethernet->ether_shost[1],
            (unsigned)ethernet->ether_shost[2],
            (unsigned)ethernet->ether_shost[3],
            (unsigned)ethernet->ether_shost[4],
            (unsigned)ethernet->ether_shost[5]);
        printf(", dest MAC address: %02x:%02x:%02x:%02x:%02x:%02x",
            (unsigned)ethernet->ether_dhost[0],
            (unsigned)ethernet->ether_dhost[1],
            (unsigned)ethernet->ether_dhost[2],
            (unsigned)ethernet->ether_dhost[3],
            (unsigned)ethernet->ether_dhost[4],
            (unsigned)ethernet->ether_dhost[5]);

        ip = (struct sniff_ip*)(packet + SIZE_ETHERNET);
        size_ip = IP_HL(ip) * 4;
        isIP = size_ip >= 20;
        printf(", 是否為IP: %c", isIP ? 'Y' : 'N');

        if (isIP) {
            printf(", src IP address: %s, dest IP address: %s",  inet_ntoa(ip->ip_src),  inet_ntoa(ip->ip_dst));
        }

        tcp = (struct sniff_tcp *)(packet + SIZE_ETHERNET + size_ip);

        printf(", src port: %d, dest port: %d ", tcp->th_sport, tcp->th_dport);

        if (!tcp->th_sport || !tcp->th_dport) {
            return 0;
        }

        // Add two lines between packets
        printf("\n\n");
    }

    //free
    pcap_close(handle);

    return 0;
}
