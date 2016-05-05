
import java.util.Arrays;

public class Sort05 {

	public static void main(String[] args) {		
		int[] arr1 = {10,34,2,56,7,67,88,42};
		int[] arr2 = bs(arr1);
		for(int i:arr2){
		   System.out.print(i);
		   System.out.print(", ");
		}
	}
	
	// bucket sort
    public static int[] bs(int[] array){
	  int N = array.length;	
		
      if(N <= 0) return array;   

      int min = array[0];
      int max = min;
      
	  for( int i = 1; i < N; i++ ){                // Find the minimum and maximum
        if( array[i] > max )
          max = array[i];
        else if( array[i] < min )
          min = array[i];
	  }
	  
      int bucket[] = new int[max-min+1];          // Create buckets
    
      for( int i = 0; i < N; i++ )                // "Fill" buckets
        bucket[array[i]-min]++;                   // by counting each datum

      int i = 0;                                  
      for( int b = 0; b < bucket.length; b++ )    // "Empty" buckets
        for( int j = 0; j < bucket[b]; j++ )      // back into array
          array[i++] = b+min;                     // by creating one per count
	  
	  return array;
    }

}
