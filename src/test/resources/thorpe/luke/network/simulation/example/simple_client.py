import json
import socket
import sys
from time import sleep

if __name__ == '__main__':
    if len(sys.argv) != 6:
        exit(1)

    # Get node name from arg 1.
    node_name = sys.argv[1]
    # Get ip address from arg 2.
    private_ip = sys.argv[2]
    # Get port from arg 3.
    port = int(sys.argv[3])
    # Get neighbours from arg 4.
    neighbours = json.loads(sys.argv[4])
    # Get number of messages to send from arg 5.
    n = int(sys.argv[5])

    # Create datagram socket.
    private_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    private_socket.bind((private_ip, port))


    # Send messages to neighbours.
    def log(text):
        print(text)


    log(node_name + " is sending messages to neighbours from " + private_ip + ", port " + str(port))
    for neighbour_name, public_ip in neighbours.items():
        for i in range(n):
            sleep(0.05)
            data = "~" + node_name + " message number " + str(i + 1) + "~"
            log(node_name + " is sending the following message to " + neighbour_name + " on ip " + public_ip + ": " + data)
            private_socket.sendto(data.encode('utf-8'), (public_ip, port))

    private_socket.close()
