#include <stdio.h>
#include <stdlib.h>
#include <pcap.h>

void pcap_callback(u_char *arg, const struct pcap_pkthdr *header, const u_char *content);

int main(int argc, const char * argv[]) {
    char errbuf[PCAP_ERRBUF_SIZE];
    pcap_t *handle = NULL;

    handle = pcap_open_offline("assets/test.pcap", errbuf);
    if (!handle) {
        fprintf(stderr, "pcap_open_offline: %s\n", errbuf);
        exit(1);
    }

    //read from file
    if(-1 == pcap_loop(handle, -1, pcap_callback, NULL)) {
        fprintf(stderr, "pcap_loop: %s\n", pcap_geterr(handle));
    }

    //free
    pcap_close(handle);

    return 0;
}

void pcap_callback(u_char *arg, const struct pcap_pkthdr *header, const u_char *content) {
    static int d = 0;
    printf("No. %d\n", ++d);
}
