import java.util.Arrays;

public class Sort16 {
  public static void main(String[] args) {
    String[] fruits = new String[] {"Pineapple","Apple", "Orange", "Banana"};

    sort(fruits);

    int i=0;
    for(String temp: fruits){
      System.out.println("fruits " + (++i) + " : " + temp);
    }
  }

  public static void sort(String[] fruits){
    Arrays.sort(fruits);
  }
}