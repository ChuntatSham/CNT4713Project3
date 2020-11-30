/*
Team members and IDs:
Junjie Yang 6138470
Chun Tat Sham 6161063
Yuhan Qin 5595982

Github link:
https://github.com/ChuntatSham/CNT4713Project3.git
*/

package net.floodlightcontroller.myrouting;

public class MyRoutingUtils {


	
	/**
	 * cal the cost of the two switch
	 * @param srcSwitchId
	 * @param destSwitchId
	 * @return
	 */
	public static int calSwitchCost(long srcSwitchId,long destSwitchId) {
		boolean isSrcEven=(srcSwitchId%2==0);
		boolean isDestEven=(destSwitchId%2==0);
		
		int cost=0;
		if(isSrcEven&&isDestEven) {
			//both even
			cost=100;
		}else if((!isSrcEven) && (!isDestEven)) {
			//both odd
			cost=1;
		}else {
			cost=10;
		}
		MyRoutingLog.debug("cal cost:"+srcSwitchId+","+destSwitchId+","+cost);
		return cost;
	}

}
