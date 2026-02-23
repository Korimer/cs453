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
    scnr.close();
    try {
      CharQueue queue = Tokenizer.classifyString(lines);
      ExprToken e = Tokenizer.tokenize(queue);
      System.out.println("content: " + e.getContentPost());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

class Tokenizer {

  public static ExprToken tokenize(CharQueue classified) throws GrammarException {
    ExprToken expr = new ExprToken(classified);
    return expr;
  }

  public static CharQueue classifyString(ArrayList<String> s) throws SyntaxParsingException {
    CharQueue classified = new CharQueue();
    ArrayList<String> preparsed_lines = preparse(s);
    int i=0;
    for (i=0; i<preparsed_lines.size(); i++) {
      String line = preparsed_lines.get(i).split("#")[0];
      for (int j=0; j<line.length(); j++) {
        ClassifiedString result = identify(line.substring(j,j+1),i+1);
        if (result == null)
          throw new SyntaxParsingException(i+1);
        classified.add(result);
      }
    }

    classified.add(new ClassifiedString("", StringType.EOF,i+1));

    return classified;
  }
  
  private static ArrayList<String> preparse(ArrayList<String> lines) throws SyntaxParsingException {
    for (int i=0; i<lines.size(); i++) {
      String line = lines.get(i);
      if (line.contains(">") || line.contains("<"))
        throw new SyntaxParsingException(i+1);
      lines.set(i,line
        .replace("++",">")
        .replace("--","<")
        .replace(" ","")
      );
    }
    
    return lines;
  }

  private static ClassifiedString identify(String s, int linenum) {
    for (StringType stype : StringType.values()) {
      if (stype.chars.contains(s))
        return new ClassifiedString(s, stype,linenum);
    }
    return null;
  }
}

enum TokenType {
  Expr,
  RecExpr,
  BaseExpr,
  RecBase,
  SingleExpr,
  PrefixExpr,
  PostfixExpr,
  RecPostfix,
  Atomic,
  LValue,
  ParenValue,
  Num,
  Incrop,
  Binop,
  _PrintableText;
}

class ExprToken extends Token {

  public ExprToken(CharQueue q) throws GrammarException {
    ClassifiedString c = q.peek();
    if (c == null) {throw new GrammarException(q.getLineNumber());}
    switch (c.type) {
      case Incrop: case Num: case OpnParen: case Ref:
        innerTokens.add(new BaseExprToken(q));
        innerTokens.add(new RecExprToken(q));
        break;
      default:
        throw new GrammarException(c.linenum);
    }
  }
}

class RecExprToken extends Token {
  public RecExprToken(CharQueue q) throws GrammarException {
    ClassifiedString c = q.peek();
    if (c == null) {throw new GrammarException(q.getLineNumber());}
    switch (c.type) {
      case Incrop: case Num: case OpnParen: case Ref:
        innerTokens.add(new StringConcatSignifier());
        innerTokens.add(new BaseExprToken(q));
        innerTokens.add(new RecExprToken(q));
        break;
      default:
        break;
    }
  }
  
  @Override
  public String getContentPost() {
    if (this.innerTokens.size() == 0)
      return "";
    return this.innerTokens.get(1).getContentPost()
      + this.innerTokens.get(0).getContentPost()
      + this.innerTokens.get(2).getContentPost();
  }
}

class BaseExprToken extends Token {
  public BaseExprToken(CharQueue q) throws GrammarException {
    ClassifiedString c = q.peek();
    if (c == null) {throw new GrammarException(q.getLineNumber());}
    switch (c.type) {
      case Incrop: case Num: case OpnParen: case Ref:
        innerTokens.add(new PostfixExprToken(q));
        innerTokens.add(new RecBaseToken(q));
        break;
      default:
        throw new GrammarException(c.linenum);
    }
    
  }
}

class RecBaseToken extends Token {
  public RecBaseToken(CharQueue q) throws GrammarException {
    ClassifiedString c = q.peek();
    if (c == null) {throw new GrammarException(q.getLineNumber());}
    switch (c.type) {
      case Binop:
        innerTokens.add(new BinopToken(q));
        innerTokens.add(new PostfixExprToken(q));
        innerTokens.add(new RecBaseToken(q));
        break;
      default:
        break;
    }
  }

  @Override
  public String getContentPost() {
    if (this.innerTokens.size() != 0)
      return this.innerTokens.get(1).getContentPost()
        + this.innerTokens.get(0).getContentPost()
        + this.innerTokens.get(2).getContentPost()
        ;
    return "";
  }
}


class PostfixExprToken extends Token {

  public PostfixExprToken(CharQueue q) throws GrammarException {
    ClassifiedString c = q.peek();
    if (c == null) {throw new GrammarException(q.getLineNumber());}
    switch (c.type) {
      case Incrop: case Num: case OpnParen: case Ref:
        innerTokens.add(new AtomicToken(q));
        innerTokens.add(new RecPostfixToken(q));
        break;
      default:
        throw new GrammarException(c.linenum);
    }
  }

  @Override
  public String getContentPost() {
    return this.innerTokens.get(1).getContentPost()
      + this.innerTokens.get(0).getContentPost();
  }
}

class RecPostfixToken extends Token {
  public RecPostfixToken(CharQueue q) throws GrammarException {
    ClassifiedString c = q.peek();
    if (c == null) {throw new GrammarException(q.getLineNumber());}
    switch (c.type) {
      case Incrop:
        innerTokens.add(new IncropToken(q,IncrementType.Post));
        innerTokens.add(new RecPostfixToken(q));
        break;
      default:
        break;
    }
  }
}

class AtomicToken extends Token {
  public AtomicToken(CharQueue q) throws GrammarException {
    ClassifiedString c = q.peek();
    if (c == null) {throw new GrammarException(q.getLineNumber());}
    switch (c.type) {
      case Incrop:
        innerTokens.add(new IncropToken(q,IncrementType.Pre));
        break;
      case Num:
        innerTokens.add(new NumToken(q));
        break;
      case OpnParen:
        innerTokens.add(new ParenValueToken(q));
        break;
      case Ref:
        innerTokens.add(new LValueToken(q));
        break;
      default:
        throw new GrammarException(c.linenum);
    }
  }
}

class PrefixExprToken extends Token {
  public PrefixExprToken(CharQueue q) throws GrammarException {
    ClassifiedString c = q.peek();
    if (c == null) {throw new GrammarException(q.getLineNumber());}
    switch (c.type) {
      case Incrop:
        innerTokens.add(new IncropToken(q,IncrementType.Pre));
        innerTokens.add(new AtomicToken(q));
        break;
      default:
        throw new GrammarException(c.linenum);
    }
  }

  @Override
  public String getContentPost() {
    return this.innerTokens.get(1).getContentPost()
      + this.innerTokens.get(0).getContentPost();
  }
}

class LValueToken extends Token {
  public LValueToken(CharQueue q) throws GrammarException {
    innerTokens.add(new TerminalTextToken(q,StringType.Ref));
    innerTokens.add(new AtomicToken(q));
  }

  @Override
  public String getContentPost() {
    return this.innerTokens.get(1).getContentPost()
      + this.innerTokens.get(0).getContentPost();
  }
}

class ParenValueToken extends Token {
  public ParenValueToken(CharQueue q) throws GrammarException {
    innerTokens.add(new TerminalTextToken(q,StringType.OpnParen));
    innerTokens.add(new ExprToken(q));
    innerTokens.add(new TerminalTextToken(q,StringType.ClsParen));
  }
}

class NumToken extends Token {
  public NumToken(CharQueue q) throws GrammarException {
    innerTokens.add(new TerminalTextToken(q,StringType.Num));
  }
}

class IncropToken extends Token {
  public IncropToken(CharQueue q, IncrementType incrtype) throws GrammarException {
    if (incrtype == IncrementType.Post)
      innerTokens.add(new TerminalPostIncrText(q));
    else
      innerTokens.add(new TerminalPreIncrText(q));
  }
}

class BinopToken extends Token {
  public BinopToken(CharQueue q) throws GrammarException {
    innerTokens.add(new TerminalTextToken(q,StringType.Binop));
  }
}

class TerminalPreIncrText extends Token {
  private String text;
  public TerminalPreIncrText(CharQueue q) throws GrammarException {
    ClassifiedString recieved = q.poll();
    if (recieved == null) {throw new GrammarException(q.getLineNumber());}
    if (!(StringType.Incrop.chars.contains(recieved.inner))) {
      throw new GrammarException(recieved.linenum);
    }
    if (recieved.inner.equals("<"))
      this.text = "--_ ";
    else
      this.text = "++_ ";
  }

  @Override
  public String getContent() {
    return text;
  }
  @Override
  public String getContentPost() {
    return this.getContent();
  }
}

class TerminalPostIncrText extends Token {
  private String text;
  public TerminalPostIncrText(CharQueue q) throws GrammarException {
    ClassifiedString recieved = q.poll();
    if (recieved == null) {throw new GrammarException(q.getLineNumber());}
    if (!(StringType.Incrop.chars.contains(recieved.inner))) {
      throw new GrammarException(recieved.linenum);
    }
    if (recieved.inner.equals("<"))
      this.text = "_-- ";
    else
      this.text = "_++ ";
  }

  @Override
  public String getContent() {
    return text;
  }
  public String getContentPost() {
    return this.getContent();
  }
}

class TerminalTextToken extends Token {
  private String text;
  public TerminalTextToken(CharQueue q, StringType toExpect) throws GrammarException {
    ClassifiedString recieved = q.poll();
    if (recieved == null) {throw new GrammarException(q.getLineNumber());}
    if (!(toExpect.chars.contains(recieved.inner))) {
      throw new GrammarException(recieved.linenum);
    }
    switch (toExpect) {
      case OpnParen: case ClsParen:
        this.text = "";
        break;
      case Incrop:
        if (recieved.inner.equals("<"))
          this.text = "-- ";
        else
          this.text = "++ ";
        break;
      default:
        this.text = recieved.inner + " ";
    }
  }

  @Override
  public String getContent() {
    return text;
  }
  public String getContentPost() {
    return this.getContent();
  }
}

class StringConcatSignifier extends Token {
  @Override
  public String getContent() {
    return "_ ";
  }
  @Override
  public String getContentPost() {
    return this.getContent();
  }
}


abstract class Token {
  InnerTokenSet innerTokens = new InnerTokenSet();
  public String getContent() {
    return innerTokens.getContent();
  }
  public String getContentPost() {
    return innerTokens.getContentPost();
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
  public final int linenum;
  public ClassifiedString(String content, StringType s, int linenum) {
    this.inner = content;
    this.type = s;
    this.linenum = linenum;
  }
}

class CharQueue extends ArrayDeque<ClassifiedString> {
  int lastline = 0;
  @Override
  public ClassifiedString poll() {
    ClassifiedString c = super.poll();
    if (c != null) lastline = c.linenum;
    return c; 
  }
  public int getLineNumber() {
    return lastline;
  }
}
class InnerTokenSet extends ArrayList<Token> {
  public String getContent() {
    String fullcontent = "";
    for (Token t : this) {
      fullcontent += t.getContent();
    }
    return fullcontent;
  }
  
  public String getContentPost() {
    String fullcontent = "";
    for (Token t : this) {
      fullcontent += t.getContentPost();
    }
    return fullcontent;
  }
}

enum IncrementType {
  Pre, Post
}

class SyntaxParsingException extends Exception {
  public SyntaxParsingException(String m) {
    super(m);
  }
  public SyntaxParsingException(int line) {
    super("Parse error in line " + line);
  }
}

class GrammarException extends Exception {
  public GrammarException(int line) {
    super("Parse error in line " + line);
  }
}