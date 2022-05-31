import analysis_checksum
import datetime
import json
import socket
import sys
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
    # Get datagram buffer size from arg 4.
    datagram_buffer_size = int(sys.argv[4])
    # Get neighbours from arg 5.
    neighbours = json.loads(sys.argv[5])
    # Get number of messages to send per second from arg 6.
    packet_output_per_second = int(sys.argv[6])
    # Get number of messages to send per millisecond from arg 7.
    timeout_in_minutes = int(sys.argv[7])

    # Create datagram socket.
    private_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    private_socket.bind((private_ip, port))


    # Send messages to neighbours.
    def log(text):
        print(text)


    client_prefix = "!"
    data_delimiter = "~"
    datetime_format = "%Y-%m-%d %H:%M:%S.%f"

    packet_id = 1
    packet_output_interval_in_seconds = 1.0 / packet_output_per_second
    finish_time = datetime.datetime.now() + datetime.timedelta(minutes=timeout_in_minutes)

    while datetime.datetime.now() < finish_time:
        for neighbour_name, public_ip in neighbours.items():
            datetime_of_sending = datetime.datetime.now().strftime(datetime_format)
            packet_id_as_str = str(packet_id)
            checksum = analysis_checksum.checksum(client_prefix, node_name, packet_id_as_str, datetime_of_sending)
            data = data_delimiter.join(
                [client_prefix, node_name, packet_id_as_str, datetime_of_sending, checksum]) + data_delimiter
            padding_length = datagram_buffer_size - len(data)
            data += "\0" * padding_length
            private_socket.sendto(data.encode("utf-8"), (public_ip, port))
            log(data)
            sleep(packet_output_interval_in_seconds)
            packet_id += 1
