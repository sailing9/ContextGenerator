package util;

import java.util.Random;
import java.util.ArrayList;


public class RandomNumber {

	/**
	 * @param args
	 */
	
	public static ArrayList random_difference(int bound, int number, long seed){
		ArrayList result = new ArrayList();
		int[] randomtemp = new int[bound];
		for(int i = 0;i<bound;i++){
			randomtemp[i]=1;			
		}
		int j = 0;
		Random random = new Random(seed);
		while(j<number){
			int x = random.nextInt(bound);
			if(randomtemp[x]!=-1){
				j++;
				randomtemp[x]=-1;
				result.add(x);
			}
		}
		return result;
		
	} 
	
	public static ArrayList random_difference_noseed(int bound, int number){
		ArrayList result = new ArrayList();
		int[] randomtemp = new int[bound];
		for(int i = 0;i<bound;i++){
			randomtemp[i]=1;			
		}
		int j = 0;
		Random random = new Random();
		while(j<number){
			int x = random.nextInt(bound);
			if(randomtemp[x]!=-1){
				j++;
				randomtemp[x]=-1;
				result.add(x);
			}
		}
		return result;
		
	} 
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ArrayList<Integer> numbers = new ArrayList<Integer>();
		numbers = random_difference(10, 5, 123456);
		System.out.println(numbers);
	}

}
