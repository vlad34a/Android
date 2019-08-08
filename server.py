import socket
import time
import threading

def start_connection(port):
	s = socket.socket()
	print("Successfully created")

	p = port
	s.bind(('', p))

	s.listen(5)
	print("Waiting for clients")

	i = 0
	while True:
		c, addr = s.accept()
		print(addr)

		threadd = threading.Thread(target = handle_conn, args = (c, ))
		print("The thread " + str(i) + " is starting")
		i += 1
		threadd.start()

def handle_conn(c):
	i = 0
	while True:
		print("Send the " + str(i+1) + " message")
		time.sleep(3)
		stri = str(i)
		i += 1
		c.send(b'123\n')
		print("The message was sent")
	c.close()

if __name__ == '__main__':
	start_connection(12345)