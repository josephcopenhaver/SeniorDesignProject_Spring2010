/* Author: Ricardo Daconceicao
*
*/
#include <iostream>
#include <cstdlib> //Reason: For functions rand and srand
#include <pthread.h>
#include <fstream>
#include <string>
#include <stdlib.h>
#include <time.h>
#include "WLAN.h"


using namespace std;
WLANHandler* wh; //(argv[1], argv[2], atoi(argv[3])); //MAC, IP, SERVERPORT

//constants
const int SIZE = 2000;

//global 
int counts = 0;
char mode;
string macAddress;// = "0023fa32bc12";
int sequence = 0;
string sendString = "";
string recieveString = "";
bool messageRecieved = false; //flag to see if recieved a message that must be unpackaged
int sensor1DataArray[SIZE];
int sensor2DataArray[SIZE];
int interval = 0;
double intervalDouble = 0.0;

string convertInt(int number){ //int to string converter
    if (number == 0)
        return "0";
    string temp="";
    string returnvalue="";
    while (number>0)
    {
        temp+=number%10+48;
        number/=10;
    }
    for (int i=0;i<temp.length();i++)
        returnvalue+=temp[temp.length()-i-1];
    return returnvalue;
}

int polling(){ //fills up an array with 1 seconds worth of free data

	int dataUnclean = (rand()%(10-1+1))+1;
	sleep(1);
	if (dataUnclean > 7)
	   return 1;
	return 0;

	//return dataUnclean;
}

int cleanData(int dataUnclean){ //clean data algorithm to be implemented when gumstix recieved

	int dataClean = dataUnclean;

	return dataClean;
}

int unpackaging(){
	
	int temp = 0; // NEW
	int modeInt = 0; // NEW
	int h=0; // NEW
	int cn=0; // NEW
	wh->lock();
	//int temp = 0; // REMOVED
	//int modeInt = 0; // REMOVED
	//int h=0; // REMOVED
	//int cn=0;	if( (temp = wh->getModeType()) != -1 ){ // REMOVED
	temp = wh->getModeType(); // NEW
	if( temp != -1 ){ // NEW
		interval = wh->getModeVal(); // NEW
		wh->unlock(); // NEW
		if(temp == 0){
			mode = 'S';
		}
		else if(temp == 1){
			mode = 'I';
		}
		else{
			mode = 'A';
		}
		//interval = wh->getModeVal(); // REMOVED
		h=1;
	    cout << "interval is:";
	    cout << interval;
	    cout << "\n";
		cout << "Mode is: "<< mode << endl;
		intervalDouble = interval; 
	}
	else // NEW
	{ // NEW
		wh->unlock(); // NEW
	} // NEW
/*
	int size = package.length();
	mode = package[0];
	string timeStr = package.substr(2, (size-1)); //2 b/c dont want to include pipe
	int len = timeStr.length();
	interval = 0;
	
	int i = 0;
	while(i < len){
	
		int temp = (timeStr[i] - 48);
		int j = i+1;
		while(j < len){
			temp = temp*10;
			j++;
		}
		interval = interval + temp;
		i++;
	}
	//cout << mode;
	intervalDouble = interval; 
	/*
	cout << "interval is:";
	cout << interval;
	cout << "\n";
	*/
	return h;
}

string package(string messageType){
	string dataPackaged;
	dataPackaged.append(messageType);
	dataPackaged.append("|"); //*
	string seq = convertInt(sequence);
	
	int x = 2 - seq.length();
	int i = 0;
	while(i < x){
		dataPackaged.append("0");
		i++;
	}
	dataPackaged.append(seq);
	dataPackaged.append("|"); //*
	dataPackaged.append(macAddress);
	dataPackaged.append("|"); //*
	string countStr = convertInt(counts);
	
	int y = 4 - countStr.length();
	int j = 0;
	while(j < y){
		dataPackaged.append("0");
		j++;
	}
	dataPackaged.append(countStr);

	return dataPackaged;
}

string communication(string package){return "";}

void sendPackagedData(string package){
	
	cout << "packaged data sent!\n";
	cout << package;
	cout << "\n";
	
	//Call group 3's send procedure HERE!
	wh->queueString(package);
	
	sequence++;
	if(sequence == 100){ //this rolls the sequence back to 0 so as to have a sequence form 0-99 inclusive
		sequence = 0;
	} 
	
}

void registration(){

	cout << "Registration confirmation sent!\n";

	string dataPackaged = package("R"); //create registration package
	sendPackagedData(dataPackaged); //send packaged registration
	
	while(unpackaging() !=1){ //go into a sleep mode and wait for message back.
	  sleep(1);
	}
	//unpackaging();
	
	cout << "Registration confirmation recieved!\n";
	
}

int main(int argc, char **argv){
	time_t start,end;
	time_t start2, end2;
	double dif;
	int cn=0;
	
	cerr << "entering wlan init" << endl;
	wh = &WLANHandler(argv[1], argv[2], atoi(argv[3])); //MAC, IP, SERVERPORT
	cerr << "Done: entering wlan init" << endl;
    macAddress = argv[1];
	//mode = 'S';
	//messageRecieved = true;
	//recieveString = "S|30";
	//recieveString = "A|10";
	//recieveString = "I|30";
	
	registration(); 
	
	time (&start);
	bool go = true;
	int test;
	//messageRecieved = false; //for testing
	while(go){
	
		if(mode == 'S'){
            if (counts != 0){ //this is for the case that count != 0 when entering sleep, server decides to ignore message or keep it
			   string dataPackaged = package("C");
			   sendPackagedData(dataPackaged);
               counts = 0; //reset count to zero for clean wakeup
			}     
			
			sleep(1);
		}
		else{ //mode == 'A' | mode == 'I'
			//cout<< "Made it to A | I mode";
			//cout<< flush;
			
			int dataUnclean = polling(); //TBA fills 2 arrays 1 for each sensor but for now just a single random number till we work with the gumstix device
			int dataClean = cleanData(dataUnclean); //returns a cleaned up count 
			counts = counts + dataClean;
			
			
			if ((mode == 'I') && (counts > 0)){
			    string dataPackaged = package("C");
			    sendPackagedData(dataPackaged);
				time(&start);
				counts = 0;
				cn =0;
			}
			sleep(1);
			cn++;
			time (&end);
			dif = difftime (end,start);
			//if (test != dif){
			//cout<< dif;
			//cout<< "\n";
			//}
			//if(dif >= intervalDouble){
			if (cn >= intervalDouble){
				if(counts == 0){ //for heartbeat
					string dataPackaged = package("H");
					sendPackagedData(dataPackaged);
				}
				else{
					string dataPackaged = package("C");
					sendPackagedData(dataPackaged);
				}

				counts = 0;
				time (&start);
				cn =0;
			} 

			
		}
		//if(messageRecieved == true){ //get the recieved message/string and unpackage it and use the info to update globals
			unpackaging(); 
		//}
		//test = dif; // test purposes
	}
	
	return 0;
}

