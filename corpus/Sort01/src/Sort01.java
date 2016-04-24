
import java.util.Arrays;

public class Sort01 {

	public static void main(String[] args) {
		int[] array = new int[]{3,5,1,9,2,7};
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
