import hashlib

def checksum(client_prefix, client_name, packet_id, datetime_of_sending):
    encoding = "utf-8"
    message = hashlib.sha256()
    message.update(bytes(client_prefix, encoding))
    message.update(bytes(client_name, encoding))
    message.update(bytes(packet_id, encoding))
    message.update(bytes(datetime_of_sending, encoding))
    return message.hexdigest()
