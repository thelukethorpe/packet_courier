from multiprocessing import Process
import socket
import sys

if __name__ == "__main__":
    if len(sys.argv) != 5:
        exit(1)

    # Get node name from arg 1.
    node_name = sys.argv[1]
    # Get ip address from arg 2.
    private_ip = sys.argv[2]
    # Get port from arg 3.
    port = int(sys.argv[3])
    # Get datagram buffer size from arg 4.
    datagram_buffer_size = int(sys.argv[4])

    # Create datagram socket.
    private_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    private_socket.bind((private_ip, port))


    # Receive and print packets for 15 seconds.
    def log(text):
        print(text)


    def listen_and_print():
        while True:
            data, address = private_socket.recvfrom(datagram_buffer_size)
            log(node_name + " has received data from " + str(address) + ": " + data.decode("utf-8"))


    log(node_name + " is listening on " + private_ip + ", port " + str(port))
    listener = Process(target=listen_and_print)
    listener.start()
    listener.join(timeout=15)
    listener.terminate()

    private_socket.close()
