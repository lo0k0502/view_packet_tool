#include <stdio.h>
#include <stdlib.h>
#include <pcap.h>
#include <net/ethernet.h>
#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>

#define SIZE_ETHERNET 14

int main(int argc, const char * argv[]) {
    char errbuf[PCAP_ERRBUF_SIZE];
    int packetCount = 0,
        isIP = 0,
        isTCP = 0;
    const u_char *packet;
    struct pcap_pkthdr *header;

    struct sockaddr_in src, dest;
    const struct ethhdr *eth;
    const struct iphdr *iph;
    const struct tcphdr *tcph;
    const struct udphdr *udph;

    pcap_t *handle = pcap_open_offline("assets/bigFlows.pcap", errbuf);
    if (!handle) {
        fprintf(stderr, "pcap_open_offline: %s\n", errbuf);
        exit(1);
    }

    while (pcap_next_ex(handle, &header, &packet) >= 0) {
        // Show the packet number
        printf("Packet # %i", ++packetCount);

        // Show Epoch Time
        printf(", Timestamp: %ld seconds;%ld milliseconds", header->ts.tv_sec, header->ts.tv_usec);

        // Convert packet to ethernet and print src address, dest address and ethernet type
        eth = (struct ethhdr *) packet;
        printf(", src MAC address: %.2x:%.2x:%.2x:%.2x:%.2x:%.2x",
            eth->h_source[0],
            eth->h_source[1],
            eth->h_source[2],
            eth->h_source[3],
            eth->h_source[4],
            eth->h_source[5]);
        printf(", dest MAC address: %.2x:%.2x:%.2x:%.2x:%.2x:%.2x",
            eth->h_dest[0],
            eth->h_dest[1],
            eth->h_dest[2],
            eth->h_dest[3],
            eth->h_dest[4],
            eth->h_dest[5]);
        printf(", Ethernet Type: %u", eth->h_proto);

        // If ethernet type = 8, then it is using IP protocol
        isIP = eth->h_proto == 8;

        // Convert packet to ip and determine what protocol is the packet using
        iph = (struct iphdr*)(packet + SIZE_ETHERNET);
        switch (iph->protocol) {
            case 6: {
                isTCP = 1;
                break;
            }
            case 17: {
                isTCP = 0;
                break;
            }
            default: {
                break;
            }
        }

        // Convert ip address to sockaddr_in to manipulate address
        memset(&src, 0, sizeof(src));
        src.sin_addr.s_addr = iph->saddr;

        memset(&dest, 0, sizeof(dest));
        dest.sin_addr.s_addr = iph->daddr;

        // Print out whether the packet is using IP protocol or not, if so, print out the src, dest addresses
        printf(", isIP: %c", isIP ? 'Y' : 'N');

        if (isIP) {
            printf(", src IP address: %s, dest IP address: %s",  inet_ntoa(src.sin_addr),  inet_ntoa(dest.sin_addr));
        }

        // Print out whether the packet is using TCP or UDP protocol, and print out the src, dest port of each packet
        if (isTCP) {
            printf(", TCP");
            tcph = (struct tcphdr *)(packet + SIZE_ETHERNET + (iph->ihl * 4));
            printf(", src TCP port: %d, dest TCP port: %d ", ntohs(tcph->source), ntohs(tcph->dest));
        } else {
            printf(", UDP");
            udph = (struct udphdr *)(packet + SIZE_ETHERNET + (iph->ihl * 4));
            printf(", src UDP port: %d, dest UDP port: %d ", ntohs(udph->source), ntohs(udph->dest));
        }

        // Add two lines between packets
        printf("\n\n");
    }

    //free
    pcap_close(handle);

    return 0;
}
