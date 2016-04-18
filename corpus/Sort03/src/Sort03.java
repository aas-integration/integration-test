
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

public class Sort03 {

	public static void main(String[] args) {
		List<Integer> array = new LinkedList<Integer>();
		for (int i=0; i<args.length;i++) {
			array.add(Integer.parseInt(args[i]));
		}
		sort(array);
		for (Integer i : array) {
			System.out.println(i);
		}
	}
	
	public static void sort(List<Integer> unsorted) {
		Collections.sort(unsorted);
	}

}
