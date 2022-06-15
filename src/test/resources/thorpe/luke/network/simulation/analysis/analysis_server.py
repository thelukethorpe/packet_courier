import analysis_checksum
import datetime
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


    server_prefix = "?"
    data_delimiter = "~"
    datetime_format = "%Y-%m-%d %H:%M:%S.%f"

    while True:
        raw_data, address = private_socket.recvfrom(datagram_buffer_size)
        datetime_of_receipt = datetime.datetime.now()
        try:
            data = raw_data.decode("utf-8")
        except:
            log("Junk!")
            continue
        data_elements = data.split(data_delimiter)

        if len(data_elements) != 6:
            log("Junk: " + data)
            continue

        client_prefix = data_elements[0]
        client_name = data_elements[1]
        packet_id = data_elements[2]
        datetime_of_sending = data_elements[3]
        checksum = data_elements[4]
        is_checksum_corrupted = checksum != analysis_checksum.checksum(client_prefix, client_name, packet_id,
                                                                       datetime_of_sending)

        log(data_delimiter.join(
            [server_prefix, node_name, datetime_of_receipt.strftime(datetime_format), client_prefix, client_name,
             packet_id,
             datetime_of_sending, str(is_checksum_corrupted)]))
