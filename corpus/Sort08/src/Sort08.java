import java.util.Arrays;

public class Sort08 {

	public static void main(String[] args) {
		int [] array = {5,3,0,2,4,1,0,7,2,9,1,4}; 
		System.out.println("Before: " + Arrays.toString(array));
		sort(array);
		System.out.println("After:  " + Arrays.toString(array));
	}
	
	// odd-even sort
	public static void sort(int[] array) {
	     boolean sorted = false;
	     while (!sorted) {
	          sorted = true;
	          for (int i = 1; i < array.length - 1; i += 2) {
	               if (array[ i ] > array[ i + 1 ]) {
	                    swapKeys( array , i , i + 1 );
	                    sorted = false;
	               }
	          }
	          for (int i = 0; i < array.length - 1; i += 2) {
	               if (array[ i ] > array[ i + 1 ]) {
	                    swapKeys( array , i , i + 1 );
	                    sorted = false;
	               }
	          }
	     }
	}

	private static void swapKeys(int[] array, int i, int j) {
	     int temp;
	     temp = array[ i ];
	     array[ i ] = array[ j ];
	     array[ j ] = temp;
	} 

}
