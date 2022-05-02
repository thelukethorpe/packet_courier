import json
import socket
import sys
from multiprocessing import Process
from threading import Lock
from time import sleep

if __name__ == '__main__':
    if len(sys.argv) != 8:
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
    datagram_buffer_size = int(sys.argv[5])
    # Get number of messages to send from arg 6.
    n = int(sys.argv[6])
    # Get the name of the parent topology group from arg 7.
    topology_group_name = sys.argv[7]

    # Create datagram socket.
    private_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    private_socket.bind((private_ip, port))

    # Establish lock to ensure that logging is atomic.
    log_mutex = Lock()


    # Send messages to neighbours.
    def log(text):
        log_mutex.acquire()
        print(text)
        log_mutex.release()


    def send_to_neighbours(data):
        for neighbour_name, public_ip in neighbours.items():
            private_socket.sendto(data, (public_ip, port))


    def listen_and_print():
        while True:
            data, address = private_socket.recvfrom(datagram_buffer_size)
            message = node_name + " from " + topology_group_name + " has received data from " + str(
                address) + ": " + data.decode("utf-8")
            if "Ring 1" in message and "Ring 2" in message:
                log("Leaked message! " + message)
            else:
                log(message)
            send_to_neighbours(data)


    log(node_name + " from " + topology_group_name + " is listening on " + private_ip + ", port " + str(port))
    listener = Process(target=listen_and_print)
    listener.start()

    log(node_name + " is sending messages to neighbours from " + private_ip + ", port " + str(port))
    for i in range(n):
        sleep(0.25)
        data = "~" + node_name + " from " + topology_group_name + " message number " + str(i + 1) + "~"
        send_to_neighbours(data.encode("utf-8"))

    listener.join(timeout=15)
    listener.terminate()

    private_socket.close()
