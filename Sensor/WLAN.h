// VTAG-FINAL-QUERKY-ACKING
#ifndef SAMSOCKET_H
#define SAMSOCKET_H
#include <cstdlib>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <pthread.h>
#include <string>
#include <string.h>
#include <vector>
#include <fstream>
#include <iostream>
#include <semaphore.h>
#include <arpa/inet.h>
#define RECVBUFSIZE 80
#define SENDBUFFSIZE 50
#define QUEUE_SIZE 50
#define RECOVERDELAY 5
#define MODE_SLEEP 0
#define MODE_IMMEDIATE 1
#define MODE_AGGREGATE 2
using namespace std;
/**
 *wlan_Buffer
 *
 *Implements a rotating queue to buffer send data in the event of a disconnection. Pushing more data onto the buffer than
 *its capacity overwrites earlier elements in the buffer. Buffer is thread safe. Not safe for copying.
 */

template<class T>
class wlan_Buffer
{
	public:
		wlan_Buffer(const size_t size)
		{

			elements = 0;
			start = 0;
			end = 0;
			buf_size = size;
			queue = new T[size];
			mutex1 = new pthread_mutex_t;
			pthread_mutex_init(mutex1, NULL);
			mutex2 = new pthread_mutex_t;
			pthread_mutex_init(mutex2, NULL);
			waiting = false;

		}
		;
		
		~wlan_Buffer()
		{
			delete[] queue;
			pthread_mutex_destroy(mutex1);
		}
		;

		T peek() const {
			//cerr << "peekn " << elements << endl;
			return queue[start];
		}
		;
		void initLock()
		{
			pthread_mutex_lock(mutex2);
		}
		;
		
		void waitForInput()
		{
			pthread_mutex_lock(mutex1);
			//cerr << "WFI: " << start << ", " << end << ", " << elements << endl;
			if (elements == 0)
			{
				waiting = true;
				pthread_mutex_unlock(mutex1);
				//cerr << "WLAN: waiting for data to send" << endl;
				//block and wait for input
				pthread_mutex_lock(mutex2); //mutex Times out here on cygwin-windows,???
				//cerr << "WLAN: got some data to send" << endl;
			}
			else
			{
				pthread_mutex_unlock(mutex1);
			}
		}
		;

		T pop()
		{
			pthread_mutex_lock(mutex1);
			
			//cerr << "Pop: " << start << ", " << end << ", " << elements << endl;
			T ret = queue[start];
			start = (start + 1) % buf_size;
			//Error generated here on cygwin-windows (Mutexes timed out in waitForInput(), ??????)
			if (elements - 1 > elements)
				throw "wlan_Buffer.pop(): Popping from empty buffer!";
			elements--;
			pthread_mutex_unlock(mutex1);
			return ret;
		}
		;

		void push(const T & data)
		{
			pthread_mutex_lock(mutex1);
			
			//cerr << "Push: " << start << ", " << end << ", " << elements << endl;
			queue[end] = data;
			end = (end + 1) % buf_size;
			if (elements == buf_size)
				start = (start + 1) % buf_size;
			else
				elements++;
			if (waiting)
			{
				waiting = false;
				pthread_mutex_unlock(mutex2);
			}
			pthread_mutex_unlock(mutex1);
		}
		;

		bool empty()
		{
			return elements == 0;
		}
		;

		size_t size()
		{
			return elements;
		}
		;

	private:
		size_t buf_size;
		size_t elements;
		T * queue;
		unsigned int start;
		unsigned int end;
		pthread_mutex_t * mutex1;
		pthread_mutex_t * mutex2;
		bool waiting;
};

struct wlandata
{
	string host;
	int port;
	int modeType;
	int modeVal;
	int socket2;
	wlan_Buffer<string> outbuffer;
	string nodeID;
	string inBuff;
	string outBuff;
	pthread_mutex_t * mutex;
	pthread_mutex_t * sendMutex;
	pthread_mutex_t * ackMutex;
	pthread_mutex_t * ackHandleMutex;
	bool isWaitingForAck;
	wlandata(const string &nodeID, const string & serverhost, const int serverport) : outbuffer(QUEUE_SIZE)
	{
		this->nodeID = nodeID;
		host = serverhost;
		port = serverport;
		socket2=-1;
		modeType = -1;
		modeVal = -1;
		inBuff="";
		isWaitingForAck = false;
		mutex = new pthread_mutex_t;
		pthread_mutex_init(mutex, NULL);
		sendMutex = new pthread_mutex_t;
		pthread_mutex_init(sendMutex, NULL);
		ackMutex = new pthread_mutex_t;
		pthread_mutex_init(ackMutex, NULL);
		ackHandleMutex = new pthread_mutex_t;
		pthread_mutex_init(ackHandleMutex, NULL);
	}
	;
};

// signature that appears to be required by pthread create constructs
void * wlan_listen(void *);
bool wlan_recvLastest(wlandata * wdata);
void wlan_makeTCPSocket(wlandata * wdata);
void wlan_closeTCPSocket(wlandata * wdata);
void wlan_lock(wlandata * wdata);
void wlan_unlock(wlandata * wdata);
void wlan_lock_send(wlandata * wdata);
void wlan_unlock_send(wlandata * wdata);
int wlan_send(wlandata * wdata, const string & s);
void * wlan_sender(void*);

class WLANHandler
{

	public:
		WLANHandler(const string &);
		WLANHandler(const string &, const string &);
		WLANHandler(const string &, const string &, const int);
		int getModeType();
		int getModeVal();
		void queueString(string);
		void lock();
		void unlock();
		
	private:
		void init();
		wlandata * wdata;
		
};

WLANHandler::WLANHandler(const string & mac)
{
	wdata = new wlandata(mac, "127.0.0.1", 1429);
	init();
}

WLANHandler::WLANHandler(const string & mac, const string & serverhost)
{
	wdata = new wlandata(mac, serverhost, 1429);
	init();
}

WLANHandler::WLANHandler(const string & mac, const string & serverhost, const int serverport)
{
	wdata = new wlandata(mac, serverhost, serverport);
	init();
}

void WLANHandler::init()
{
	//cout << "WLANH v. 1.4 started" << endl;
	pthread_attr_t attrLB;
	pthread_t tidLB;
	//cout << "Creating new com listener thread..." << endl;
	pthread_attr_init(&attrLB);
	pthread_attr_setdetachstate(&attrLB, PTHREAD_CREATE_DETACHED);
	pthread_create(&tidLB, &attrLB, wlan_listen, (void *) wdata); // create the thread, note call requirements
	//cout << "New thread created to listen for server messages!" << endl;

	pthread_attr_t attrLB2;
	pthread_t tidLB2;
	//cout << "Creating new com writer thread..." << endl;
	pthread_attr_init(&attrLB2);
	pthread_attr_setdetachstate(&attrLB2, PTHREAD_CREATE_DETACHED);
	pthread_create(&tidLB2, &attrLB2, wlan_sender, (void *) wdata); // create the thread, note call requirements
	//cout << "New thread created to write messages from queue!" << endl;
	//cout.flush();
}

void * wlan_sender(void* data)
{
   wlandata * wdata = (wlandata *) data;
   bool isActive = false;
   pthread_mutex_lock(wdata->ackMutex);
   wdata->outbuffer.initLock();
   while (true)
   {
		while (!isActive)
		{
			wlan_lock_send(wdata);
			isActive = (wdata->socket2 != -1);
			wlan_unlock_send(wdata);
			if (!isActive)
				sleep(RECOVERDELAY);
		}
		while (true)
		{
			//cerr << "waiting for input to send" << endl;
			wdata->outbuffer.waitForInput();
			//cerr << "got input to send" << endl;
			wlan_lock_send(wdata);
			pthread_mutex_lock(wdata->ackHandleMutex);
			if (wlan_send(wdata, wdata->outbuffer.peek())==1)
			{
				wdata->isWaitingForAck = true;
				wlan_unlock_send(wdata);
				//cerr << "Waiting for ack" << endl;
				pthread_mutex_unlock(wdata->ackHandleMutex);
				pthread_mutex_lock(wdata->ackMutex);
				//cerr << "Got ack" << endl;
				if (wdata->socket2 != -1) // Connection still active
				{
					wdata->outbuffer.pop();
				}
				else
				{
					//cerr << "Socket died" << endl;
				}
			}
			else
			{
				pthread_mutex_unlock(wdata->ackHandleMutex);
				wlan_unlock_send(wdata);
				//cerr << "Socket went bang when trying to send data, it's all good" << endl;
				break;
			}
		}
		isActive = false;
   }
}

int wlan_send(wlandata * wdata, const string & s)
{
	if (wdata->socket2 == -1)
	{
		return 0;
	}
	char msg[SENDBUFFSIZE];
	memset(&msg, 0, SENDBUFFSIZE);// Should be safe to comment this out

	int x;
	
	if (s.length() >=SENDBUFFSIZE)
	{
		cerr<< "We can only support messages of the size " << (SENDBUFFSIZE - 1) << " or greater not " << s.length() << endl;
		exit(1);
	}
	
	for (x = 0; (x < SENDBUFFSIZE-1) && (x < s.length()); x++)
	{
		msg[x] = s.c_str()[x];
	}
	msg[x++] = '\n';
	
	int w;
	
	if ((w=(write(wdata->socket2, msg, x))) == -1)
	{
		wlan_closeTCPSocket(wdata);
		//cerr << "Error on sending" << endl;
		return -1;
	}

	if (w == x)
		return 1;
	
	// consolidate if the sysbuff is full by blocking
	int l;
	while (w < x)
	{
		l = x-w;
		for (x=0;x<l;x++)
		{
			msg[x] = msg[w++];
		}
		if ((w=(write(wdata->socket2, msg, x))) == -1)
		{
			wlan_closeTCPSocket(wdata);
			//cerr << "Error on sending" << endl;
			return -1;
		}
	}

	return 1;
}

void WLANHandler::lock()
{
	wlan_lock(wdata);
}

void WLANHandler::unlock()
{
	wlan_unlock(wdata);
}

bool wlan_recvLastest(wlandata * wdata)
{
	if (wdata->socket2 == -1)
	{
		return false;
	}
	char buffer[RECVBUFSIZE]; // used for incoming string
	int b;
	bool gotMsg=false;
	while (!gotMsg)
	{
		b = read(wdata->socket2, buffer, RECVBUFSIZE);
		if (b <= 0)
		{
			if (b == -1)
         {
				//perror("read socket");
         }
			wlan_closeTCPSocket(wdata);
			wdata->inBuff = "";
			pthread_mutex_lock(wdata->ackHandleMutex);
			if (wdata->isWaitingForAck)
			{
				//cerr << "Releasing ack mutex" << endl;
				wdata->isWaitingForAck = false;
				pthread_mutex_unlock(wdata->ackMutex);
				//cerr << "Released ack mutex" << endl;
			}
			pthread_mutex_unlock(wdata->ackHandleMutex);
			return false;
		}
		
		for (int x = 0; x < b; x++)
		{
			if (buffer[x] != '\n')
			{
				(wdata->inBuff) += buffer[x];
			}
			else
			{
				int inBuffLength = wdata->inBuff.length();
				if (inBuffLength == 0)
				{
					pthread_mutex_lock(wdata->ackHandleMutex);
					if (wdata->isWaitingForAck)
					{
						//cerr << "_Releasing ack mutex" << endl;
						wdata->isWaitingForAck = false;
						pthread_mutex_unlock(wdata->ackMutex);
						//cerr << "_Released ack mutex" << endl;
					}
					pthread_mutex_unlock(wdata->ackHandleMutex);
				}
				else if (inBuffLength > 2)
				{
					wdata->outBuff = (wdata->inBuff);
					gotMsg = true;
				}
				else
				{
					//cerr << "useless input(" << (wdata->inBuff) << ")" << endl;
				}
				wdata->inBuff = "";
			}
		}
	}
	
	return true;
}

// signature that appears to be required by pthread create constructs
void * wlan_listen(void * data)
{
	wlandata * wdata = (wlandata *) data;
	//cerr << "Instatiating Socket" << endl;
	wlan_makeTCPSocket(wdata);

	while (true)
	{
		while (!wlan_recvLastest(wdata)) // Socket is dead
		{
			do
			{
				//cerr << "Sleeping for " << RECOVERDELAY << " seconds" << endl;
				sleep(RECOVERDELAY);
				wlan_makeTCPSocket(wdata);
			} while (wdata->socket2 == -1);
			
			//cerr << "Connection Established" << endl;
		}

		//time_t rawtime;
		//cout << ctime(&rawtime) << " :" << wdata->outBuff << endl;
		
		if ((wdata->outBuff)[1] == '|')
		{
			
			char modeType=(wdata->outBuff)[0];
			int modeTypeInt = -1;
			
			if (modeType == 's' || modeType == 'S')
			{ 
				modeTypeInt = MODE_SLEEP;
			}
			else if (modeType == 'i' || modeType == 'I')
			{
				modeTypeInt = MODE_IMMEDIATE;
			}
			else if (modeType == 'a' || modeType == 'A')
			{
				modeTypeInt = MODE_AGGREGATE;
			}
			
			string modeVal=(wdata->outBuff).substr(2,(wdata->outBuff).length() - 1);
			int modeValInt = atoi(modeVal.c_str());
			if (modeTypeInt != -1 && modeValInt >= 0)
			{
				wlan_lock(wdata);
				wdata->modeType = modeTypeInt;
				wdata->modeVal = modeValInt;
				wlan_unlock(wdata);
			}
		}
		else
		{
			//cerr << "[" << (wdata->outBuff)[1] << "] != [|]" << endl;
		}

	}

}

void wlan_makeTCPSocket(wlandata * wdata)//Creates a TCP socket
{

	int socketDesc;
	bool valid = false;
	struct sockaddr_in pin;

	if (wdata->socket2 != -1)
	{
		//cerr << "Dead socket needs to be dstroyed" << endl;
		wlan_closeTCPSocket(wdata);
	}

	if ((socketDesc = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) == -1)
	{
		//cerr << "socket creation failed" << endl;
		//perror("socket");
	}
	else
	{
		memset(&pin, 0, sizeof(pin)); /* Clear struct */
		pin.sin_family = AF_INET; /* Internet/IP */
		pin.sin_addr.s_addr = inet_addr(wdata->host.c_str()); /* IP address */
		pin.sin_port = htons(wdata->port); /* server port */

		//cout << "connecting to " << wdata->host << " on port " << wdata->port << endl;
		//cout.flush();
		
		if (connect(socketDesc, (struct sockaddr *) &pin, sizeof(pin)) == -1)
		{
			//cerr << "connection failed :-(" << endl;
			//perror("connect");
		}
		else
		{
			//cout << "\nSocket created!\n" << endl;
			valid = true;
		}
		
	}

	if (valid)
	{
		wlan_lock_send(wdata);
		wdata->socket2 = socketDesc;
		if (wlan_send(wdata, wdata->nodeID) != 1)
		{
			wdata->socket2 = -1;
		}
		wlan_unlock_send(wdata);
	}

}

void wlan_closeTCPSocket(wlandata * wdata)
{
	close(wdata->socket2);
	wdata->socket2 = -1;
	//cout << "\nConnection closed" << endl;
}

void wlan_lock_send(wlandata * wdata)
{
	pthread_mutex_lock(wdata->sendMutex);
}

void wlan_unlock_send(wlandata * wdata)
{
	pthread_mutex_unlock(wdata->sendMutex);
}

void wlan_lock(wlandata * wdata)
{
	pthread_mutex_lock(wdata->mutex);
}

void wlan_unlock(wlandata * wdata)
{
	pthread_mutex_unlock(wdata->mutex);
}

int WLANHandler::getModeType()
{
	int rval = wdata->modeType;
	wdata->modeType = -1;
	return rval;
}

int WLANHandler::getModeVal()
{
	int rval = wdata->modeVal;
	wdata->modeVal = -1;
	return rval;
}

void WLANHandler::queueString(string s)
{
   wdata->outbuffer.push(s);
}
#endif
