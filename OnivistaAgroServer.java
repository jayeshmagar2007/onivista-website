import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public class OnivistaAgroServer {
  private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "3000"));
  private static final String ADMIN_PASSWORD = System.getenv().getOrDefault("ONIVISTA_ADMIN_PASSWORD", "onivista123");
  private static final Path ROOT = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
  private static final Path DB = ROOT.resolve("data").resolve("reviews.json");
  private static final Object LOCK = new Object();

  public static void main(String[] args) throws Exception {
    Files.createDirectories(DB.getParent());
    if (!Files.exists(DB)) Files.writeString(DB, "[]", StandardCharsets.UTF_8);
    HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
    server.createContext("/api/reviews", OnivistaAgroServer::reviews);
    server.createContext("/api/admin/reviews", OnivistaAgroServer::adminReviews);
    server.createContext("/", OnivistaAgroServer::staticFiles);
    server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
    server.start();
    System.out.println("============================================");
    System.out.println(" ONIVISTA AGRO - JAVA DYNAMIC WEBSITE");
    System.out.println(" Website : http://localhost:" + PORT);
    System.out.println(" Admin   : http://localhost:" + PORT + "/admin.html");
    System.out.println(" Password: " + ADMIN_PASSWORD);
    System.out.println(" Reviews : stored in data\\reviews.json");
    System.out.println(" Keep this window open while presenting.");
    System.out.println("============================================");
  }

  private static void reviews(HttpExchange e) throws IOException {
    addCors(e);
    String method=e.getRequestMethod();
    if (method.equals("GET")) { List<Review> a=read(); a.removeIf(r->!r.status.equals("approved")); sort(a); json(e,200,"{\"reviews\":"+array(a)+"}"); return; }
    if (method.equals("POST")) {
      String body=body(e); String name=value(body,"name"), text=value(body,"review"); int rating=number(value(body,"rating"));
      if(name.isBlank()||text.isBlank()||rating<1||rating>5){json(e,400,"{\"error\":\"Please enter a name, select a rating from 1 to 5, and write a review.\"}");return;}
      Review r=new Review(UUID.randomUUID().toString(),name,rating,text,"pending",Instant.now().toString());
      synchronized(LOCK){List<Review>a=read();a.add(r);write(a);} json(e,201,"{\"message\":\"Review submitted successfully and is waiting for admin approval.\"}");return;
    }
    method(e);
  }

  private static void adminReviews(HttpExchange e) throws IOException {
    addCors(e);
    if(!isAdmin(e)){json(e,401,"{\"error\":\"Invalid admin password.\"}");return;}
    String path=e.getRequestURI().getPath(), base="/api/admin/reviews";
    if(e.getRequestMethod().equals("GET") && path.equals(base)){List<Review>a=read();sort(a);json(e,200,"{\"reviews\":"+array(a)+"}");return;}
    if((e.getRequestMethod().equals("PATCH")||e.getRequestMethod().equals("DELETE"))&&path.startsWith(base+"/")){
      String id=URLDecoder.decode(path.substring(base.length()+1),StandardCharsets.UTF_8);
      synchronized(LOCK){List<Review>a=read(); Review found=null;for(Review r:a)if(r.id.equals(id)){found=r;break;} if(found==null){json(e,404,"{\"error\":\"Review not found.\"}");return;}
        if(e.getRequestMethod().equals("DELETE")){a.remove(found);write(a);json(e,200,"{\"message\":\"Review deleted.\"}");return;}
        String status=value(body(e),"status");if(!Set.of("pending","approved","rejected").contains(status)){json(e,400,"{\"error\":\"Invalid review status.\"}");return;}found.status=status;write(a);json(e,200,"{\"review\":"+found.json()+"}");return;}
    }
    method(e);
  }

  private static boolean isAdmin(HttpExchange e){String p=e.getRequestHeaders().getFirst("X-Admin-Password");return p!=null&&ADMIN_PASSWORD.equals(p);}

  private static void staticFiles(HttpExchange e)throws IOException{
    if(!e.getRequestMethod().equals("GET")){method(e);return;}
    String req=e.getRequestURI().getPath();if(req.equals("/"))req="/index.html";if(req.contains("..")){text(e,403,"Forbidden");return;}
    Path f=ROOT.resolve(req.substring(1)).normalize();if(!f.startsWith(ROOT)||!Files.exists(f)||Files.isDirectory(f)){text(e,404,"Not found");return;}
    byte[] b=Files.readAllBytes(f);e.getResponseHeaders().set("Content-Type",type(f));e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(200,b.length);try(OutputStream o=e.getResponseBody()){o.write(b);}
  }

  private static List<Review> read(){try{String s=Files.readString(DB,StandardCharsets.UTF_8);return parseArray(s);}catch(Exception ex){return new ArrayList<>();}}
  private static void write(List<Review>a)throws IOException{StringBuilder s=new StringBuilder("[");for(int i=0;i<a.size();i++){if(i>0)s.append(',');s.append(a.get(i).json());}s.append(']');Path tmp=DB.resolveSibling("reviews.tmp");Files.writeString(tmp,s.toString(),StandardCharsets.UTF_8);Files.move(tmp,DB,StandardCopyOption.REPLACE_EXISTING);}
  private static List<Review> parseArray(String s){List<Review>out=new ArrayList<>();int i=0;while(i<s.length()){while(i<s.length()&&s.charAt(i)!='{')i++;if(i>=s.length())break;int end=findObjectEnd(s,i);if(end<0)break;String o=s.substring(i,end+1);String id=value(o,"id"),name=value(o,"name"),review=value(o,"review"),status=value(o,"status"),created=value(o,"createdAt");int rating=number(value(o,"rating"));if(!id.isBlank())out.add(new Review(id,name,rating,review,status,created));i=end+1;}return out;}
  private static int findObjectEnd(String s,int start){boolean str=false,esc=false;int depth=0;for(int i=start;i<s.length();i++){char c=s.charAt(i);if(str){if(esc)esc=false;else if(c=='\\')esc=true;else if(c=='\"')str=false;}else{if(c=='\"')str=true;else if(c=='{')depth++;else if(c=='}'&&--depth==0)return i;}}return -1;}
  private static String array(List<Review>a){StringBuilder s=new StringBuilder("[");for(int i=0;i<a.size();i++){if(i>0)s.append(',');s.append(a.get(i).json());}return s.append(']').toString();}
  private static void sort(List<Review>a){a.sort(Comparator.comparing((Review r)->r.createdAt,Comparator.nullsFirst(String::compareTo)).reversed());}
  private static String value(String json,String key){String target="\""+key+"\"";int k=json.indexOf(target);if(k<0)return"";int c=json.indexOf(':',k+target.length());if(c<0)return"";int i=c+1;while(i<json.length()&&Character.isWhitespace(json.charAt(i)))i++;if(i>=json.length())return"";if(json.charAt(i)=='\"'){StringBuilder out=new StringBuilder();boolean esc=false;for(i++;i<json.length();i++){char ch=json.charAt(i);if(esc){switch(ch){case'n':out.append('\n');break;case'r':out.append('\r');break;case't':out.append('\t');break;default:out.append(ch);}esc=false;}else if(ch=='\\')esc=true;else if(ch=='\"')break;else out.append(ch);}return out.toString();}int j=i;while(j<json.length()&&json.charAt(j)!=','&&json.charAt(j)!='}')j++;return json.substring(i,j).trim();}
  private static int number(String s){try{return Integer.parseInt(s.trim());}catch(Exception e){return 0;}}
  private static String body(HttpExchange e)throws IOException{return new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);}
  private static String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n").replace("\t","\\t");}
  private static void json(HttpExchange e,int code,String s)throws IOException{byte[]b=s.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(code,b.length);try(OutputStream o=e.getResponseBody()){o.write(b);}}
  private static void text(HttpExchange e,int code,String s)throws IOException{byte[]b=s.getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(code,b.length);try(OutputStream o=e.getResponseBody()){o.write(b);}}
  private static void method(HttpExchange e)throws IOException{json(e,405,"{\"error\":\"Method not allowed.\"}");}
  private static void addCors(HttpExchange e){e.getResponseHeaders().set("Access-Control-Allow-Origin","*");e.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type, X-Admin-Password");e.getResponseHeaders().set("Access-Control-Allow-Methods","GET, POST, PATCH, DELETE, OPTIONS");}
  private static String type(Path p){String n=p.getFileName().toString().toLowerCase();if(n.endsWith(".html"))return"text/html; charset=UTF-8";if(n.endsWith(".css"))return"text/css; charset=UTF-8";if(n.endsWith(".js"))return"application/javascript; charset=UTF-8";if(n.endsWith(".jpg")||n.endsWith(".jpeg"))return"image/jpeg";if(n.endsWith(".png"))return"image/png";if(n.endsWith(".svg"))return"image/svg+xml";return"application/octet-stream";}
  static class Review{String id,name,review,status,createdAt;int rating;Review(String i,String n,int r,String t,String s,String c){id=i;name=n;rating=r;review=t;status=s;createdAt=c;}String json(){return"{\"id\":\""+esc(id)+"\",\"name\":\""+esc(name)+"\",\"rating\":"+rating+",\"review\":\""+esc(review)+"\",\"status\":\""+esc(status)+"\",\"createdAt\":\""+esc(createdAt)+"\"}";}}
}
