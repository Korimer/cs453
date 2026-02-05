import java.util.ArrayList;
import java.util.Scanner;

public class Parse {

  public static void main(String[] args) {
    Scanner scnr = new Scanner(System.in);
    ArrayList<String> lines = new ArrayList<>();
    while (scnr.hasNext()) {
      lines.add(scnr.nextLine());
    }
  }
}

enum Operator {
  Plus("+"),
  Minus("-"),
  Mult("*"),
  Div("/")
  ;
  public final String symbol;
  Operator(String symbol) {
    this.symbol = symbol;
  }
}
