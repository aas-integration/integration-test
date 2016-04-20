import java.util.Arrays;

public class Sort10 {

	public static void main(String[] args) {
		int [] array = {5,3,0,2,4,1,0,7,2,9,1,4}; 
		System.out.println("Before: " + Arrays.toString(array));
		sort(array);
		System.out.println("After:  " + Arrays.toString(array));
	}
	
	// CombSort
	public static void sort(int[] array) {
	     int gap = array.length;
	     double shrink = 1.3;
	     boolean swapped = false;
	     int i;
	     while (gap != 1 || swapped) {
	          gap = (int) (gap / shrink);
	          if (gap < 1)
	               gap = 1;
	          i = 0;
	          swapped = false;
	          while (i + gap < array.length) {
	               if (array[ i ] > array[ i + gap ]) {
	                    swapKeys( array , i , i + gap );
	                    swapped = true;
	               }
	               i++;
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
