import java.util.ArrayDeque;
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

class Tokenizer {

  public static ExprToken tokenize(ArrayDeque<ClassifiedString> classified) {

    return null;
  }

  public static ArrayDeque<ClassifiedString> classifyString(String s) throws SyntaxParsingException {
    ArrayDeque<ClassifiedString> classified = new ArrayDeque<>();
    String[] preparsed_lines = preparse(s);

    for (int i=0; i<preparsed_lines.length; i++) {
      String line = preparsed_lines[i];
      for (int j=0; j<line.length(); j++) {
        ClassifiedString result = identify(s.substring(j,j+1));
        if (result == null)
          throw new SyntaxParsingException(i+1);
        classified.add(result);
      }
    }

    classified.add(new ClassifiedString("", StringType.EOF));

    return classified;
  }
  private static String[] preparse(String s) throws SyntaxParsingException {
    String[] lines = s.split("\\n");
    for (int i=0; i<lines.length; i++) {
      String line = lines[i];
      if (line.contains(">") || line.contains("<"))
        throw new SyntaxParsingException(i+1);
      lines[i] = line
        .replace("++",">")
        .replace("--","<")
        .replace(" ","")
      ;
    }
    
    return lines;
  }

  private static ClassifiedString identify(String s) {
    for (StringType stype : StringType.values()) {
      if (stype.chars.contains(s))
        return new ClassifiedString(s, stype);
    }
    return null;
  }
}

enum TokenType {
  Epxr,
  BaseExpr, RecExpr,
  MonoExpr,
  PostfixExpr, RecPostfix,
  Literal,
  Lvalue,
  Num,
  Incrop,
  Binop,
}

class ExprToken implements TokenInterface {
  public Token getNext(ClassifiedString c) {
    switch (c.type) {
      case Num:
        return null;
      default:
        return null;
    }
  }
}


interface TokenInterface {
  public Token getNext(ClassifiedString c);
}

abstract class Token {
  public final TokenOrString inner;
  protected final TokenType type;

  public Token(Token t, TokenType type) {
    this(new TokenOrString(t), type);
  }
  public Token(String s, TokenType type) {
    this(new TokenOrString(s), type);
  }

  private Token(TokenOrString ts, TokenType type) {
    this.inner = ts;
    this.type = type;
  }

  public String getContent() {
    return inner.getContent();
  }
}

class TokenOrString {
  private boolean isToken;
  private Token token = null;
  private String string = null;
  
  public TokenOrString(Token t) {
    this.isToken = true;
    this.token = t;
  }
  public TokenOrString(String s) {
    this.isToken = false;
    this.string = s;
  }

  public String getContent() {
    if (this.isToken) {
      return this.token.getContent();
    }
    else {
      return this.string;
    }
  }
}

enum StringType {
  Num("0123456789"),
  Incrop("<>"),
  Binop("+-"),
  OpnParen("("),
  ClsParen(")"),
  Ref("$"),
  EOF(""),
  ;
  public final String chars;
  private StringType(String chars) {
    this.chars = chars;
  }
}

class ClassifiedString {
  public final String inner;
  public final StringType type;
  public ClassifiedString(String content, StringType s) {
    this.inner = content;
    this.type = s;
  }
}


class SyntaxParsingException extends Exception {
  public SyntaxParsingException(String m) {
    super(m);
  }
  public SyntaxParsingException(int line) {
    super("Parse error in line " + line);
  }
}