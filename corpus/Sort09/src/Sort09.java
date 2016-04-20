import java.util.Arrays;

public class Sort09 {

	public static void main(String[] args) {
		int [] array = {5,3,0,2,4,1,0,7,2,9,1,4}; 
		System.out.println("Before: " + Arrays.toString(array));
		sort(array);
		System.out.println("After:  " + Arrays.toString(array));
	}
	
	// Gnome Sort
	public static void sort(int[] array) {
	     int pos = 1;
	     while (pos < array.length) {
	          if (array[ pos ] >= array[ pos - 1 ]) {
	               pos++;
	          } else {
	               swapKeys( array , pos , pos - 1);
	               if (pos > 1)
	                    pos--;
	          }
	     }
	}

	public static void swapKeys(int[] array, int i, int j) {
	     int temp;
	     temp = array[ i ];
	     array[ i ] = array[ j ];
	     array[ j ] = temp;
	} 

}
