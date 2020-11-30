/*
Team members and IDs:
Junjie Yang 6138470
Chun Tat Sham 6161063
Yuhan Qin 5595982

Github link:
https://github.com/ChuntatSham/CNT4713Project3.git
*/

package net.floodlightcontroller.myrouting;

public class MyRoutingLog {
	
	//print debug log
	public final static boolean ENABLE_DEBUG=false;
	
	public static void debug(String str) {
		if(ENABLE_DEBUG) {
			System.out.println("[DEBUG]"+str);
		}
	}
}
