///////////////////////////////////////////////////////////////////////////////
/////////////////////////////Sample Group 1////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
#include "WLAN.h"
#include <sstream>
int main(int argc, char **argv) {
	if(argc != 6){
		cout << "Usage is " << argv[0] << " MAC server port num_messages micros_between_messages" << endl;
	}else{
		WLANHandler wh(argv[1], argv[2], atoi(argv[3])); //MAC, IP, SERVERPORT
		string input;
		std::ostringstream oss;
		int count = atoi(argv[4]);
		if(count > 0){
			for(int i = 0; i < count; i++){
				oss << i;
				input = "C|" + string( argv[1] ) + "|" + oss.str();
				//wh.lock();
				//cout << "Sending " << input << endl;
				//cout << "Current status per server: Type:" << wh.getModeType() << " Value:" << wh.getModeVal() << endl;
				//wh.unlock();
				wh.queueString(input); // returns int (1 means everything was fine, -1 means the socket broke on the send, 0 means it remains broken);
				oss.str(""); 
				usleep(atoi(argv[5]));			
			}
		}
		else{
			int i = 0;
			while(1){
				oss << i;
				input = "C|" + string( argv[1] ) + "|" + oss.str();
				//wh.lock();
				//cout << "Sending " << input << endl;
				//cout << "Current status per server: Type:" << wh.getModeType() << " Value:" << wh.getModeVal() << endl;
				//wh.unlock();
				wh.queueString(input); // returns int (1 means everything was fine, -1 means the socket broke on the send, 0 means it remains broken);
				oss.str(""); 
				usleep(atoi(argv[5]));
				i++;
			}
		}
	}
	return 0;
}

