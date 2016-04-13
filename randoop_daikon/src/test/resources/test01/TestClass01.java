/**
 * 
 */
package test01;

import java.util.Arrays;

/**
 * @author schaef
 *
 */
public class TestClass01 {

	public static void main(String[] args) {		
		int[] array = new int[args.length];
		for (int i=0; i<args.length;i++) {
			array[i] = Integer.parseInt(args[i]);
		}
		int[] sorted = sort(array);
		for (int i=0;i<sorted.length;i++) {
			System.out.println(sorted[i]);
		}
	}
	
	public static int[] sort(int[] unsorted) {
		int[] sorted = new int[unsorted.length];
		for (int i=0;i<unsorted.length;i++) {
			sorted[i] = unsorted[i];
		}
		Arrays.sort(sorted);
		return sorted;
	}

}