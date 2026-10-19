package local.simple.tvoverlay;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/** Loopback integration checks: masked client frames, UTF-8 fragments, ping, and rejected input. */
public final class HomeSocketTest {
    interface Session {void run(Socket socket) throws Exception;}
    interface Client {void run(String url) throws Exception;}
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    static void exchange(Session handler,Client client) throws Exception {
        try(ServerSocket listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){
            AtomicReference<Throwable> error=new AtomicReference<>();Thread server=new Thread(()->{try(Socket socket=listener.accept()){socket.setSoTimeout(3000);handler.run(socket);}catch(Throwable e){error.set(e);}});server.start();
            client.run("http://127.0.0.1:"+listener.getLocalPort());server.join(5000);check(!server.isAlive());if(error.get()!=null)throw new AssertionError(error.get());
        }
    }
    static void handshake(Socket socket,boolean valid) throws Exception {
        InputStream in=socket.getInputStream();ByteArrayOutputStream data=new ByteArrayOutputStream();String text;
        do{int b=in.read();if(b<0)throw new EOFException();data.write(b);text=data.toString("US-ASCII");}while(!text.endsWith("\r\n\r\n"));
        check(text.startsWith("GET /api/websocket HTTP/1.1\r\n"));String key="";for(String line:text.split("\r\n"))if(line.startsWith("Sec-WebSocket-Key: "))key=line.substring(19);
        String accept=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
        socket.getOutputStream().write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "+(valid?accept:"invalid")+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();
    }
    static String clientFrame(Socket socket,int opcode) throws Exception {
        DataInputStream in=new DataInputStream(socket.getInputStream());check(in.readUnsignedByte()==(128|opcode));int length=in.readUnsignedByte();check((length&128)!=0);length&=127;if(length==126)length=in.readUnsignedShort();
        byte[] mask=new byte[4],body=new byte[length];in.readFully(mask);in.readFully(body);for(int i=0;i<length;i++)body[i]^=mask[i%4];return new String(body,StandardCharsets.UTF_8);
    }
    static void send(Socket socket,int opcode,boolean fin,byte[] body) throws IOException {OutputStream out=socket.getOutputStream();out.write((fin?128:0)|opcode);out.write(body.length);out.write(body);out.flush();}
    public static void main(String[] args) throws Exception {
        exchange(s->{handshake(s,true);check(clientFrame(s,1).equals("read registry"));byte[] text="房间列表".getBytes(StandardCharsets.UTF_8);send(s,1,false,java.util.Arrays.copyOfRange(text,0,4));send(s,9,true,"ping".getBytes(StandardCharsets.UTF_8));send(s,0,true,java.util.Arrays.copyOfRange(text,4,text.length));check(clientFrame(s,10).equals("ping"));},url->{try(HomeSocket socket=new HomeSocket(url)){socket.send("read registry");check(socket.receive().equals("房间列表"));}});
        exchange(s->handshake(s,false),url->{try(HomeSocket ignored=new HomeSocket(url)){throw new AssertionError("Bad handshake accepted");}catch(IOException expected){}});
        exchange(s->{handshake(s,true);s.getOutputStream().write(new byte[]{(byte)129,127,0,0,0,0,1,0,0,0});s.getOutputStream().flush();},url->{try(HomeSocket socket=new HomeSocket(url)){try{socket.receive();throw new AssertionError("Oversized frame accepted");}catch(IOException expected){}}});
        System.out.println("Home Assistant WebSocket checks passed");
    }
}
