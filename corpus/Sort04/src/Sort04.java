public class Sort04 {

	public static void main(String[] args) {
		int[] array = new int[]{3,5,1,9,2,7};
		int[] sorted = ss(array);
		for (int i=0;i<sorted.length;i++) {
			System.out.println(sorted[i]);
		}
	}
	
    public static int[] ss(int[] array) {
        int n = array.length;
        int k;
        for (int m = n; m >= 0; m--) {
            for (int i = 0; i < n - 1; i++) {
                k = i + 1;
                if (array[i] > array[k]) {
                    swapNumbers(i, k, array);
                }
            }
        }
		
		return array;
    }
  
    private static void swapNumbers(int i, int j, int[] array) {
        int temp;
        temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}
