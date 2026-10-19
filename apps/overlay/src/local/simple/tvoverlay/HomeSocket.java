package local.simple.tvoverlay;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.net.ssl.*;

/** Bounded, synchronous WebSocket transport for HA registry reads on the worker thread. */
final class HomeSocket implements Closeable {
    private static final int LIMIT=8*1024*1024;
    private final Socket socket;private final InputStream in;private final OutputStream out;
    private final SecureRandom random=new SecureRandom();
    HomeSocket(String address) throws Exception {
        URI uri=new URI(address);boolean tls="https".equals(uri.getScheme());
        if(!tls&&!"http".equals(uri.getScheme()))throw new IOException("Unsupported scheme");
        String host=uri.getHost();int port=uri.getPort()<0?(tls?443:80):uri.getPort();
        Socket raw=new Socket();raw.connect(new InetSocketAddress(host,port),4000);raw.setSoTimeout(6000);
        Socket connected=raw;
        try{
            if(tls){SSLSocket secure=(SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(raw,host,port,true);connected=secure;
                SSLParameters parameters=secure.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");secure.setSSLParameters(parameters);secure.startHandshake();}
            socket=connected;in=socket.getInputStream();out=socket.getOutputStream();
            byte[] nonce=new byte[16];random.nextBytes(nonce);String key=Base64.getEncoder().encodeToString(nonce);
            String authority=(host.contains(":")&&!host.startsWith("[")?"["+host+"]":host)+":"+port;
            out.write(("GET /api/websocket HTTP/1.1\r\nHost: "+authority+"\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Key: "+key+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.flush();
            ByteArrayOutputStream header=new ByteArrayOutputStream();int matched=0;
            while(matched<4){int b=readByte();header.write(b);if(header.size()>16384)throw new IOException("Headers too large");int expected=new int[]{13,10,13,10}[matched];matched=b==expected?matched+1:(b==13?1:0);}
            String[] lines=new String(header.toByteArray(),StandardCharsets.US_ASCII).split("\r\n");
            if(!lines[0].matches("HTTP/1\\.[01] 101(?: .*)?"))throw new IOException("WebSocket upgrade failed");
            Map<String,String> headers=new HashMap<>();for(int i=1;i<lines.length;i++){int colon=lines[i].indexOf(':');if(colon>0)headers.put(lines[i].substring(0,colon).toLowerCase(Locale.ROOT),lines[i].substring(colon+1).trim());}
            String accept=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
            if(!accept.equals(headers.get("sec-websocket-accept"))||!"websocket".equalsIgnoreCase(headers.get("upgrade"))||!Arrays.asList(headers.getOrDefault("connection","").toLowerCase(Locale.ROOT).split("\\s*,\\s*")).contains("upgrade"))throw new IOException("Invalid WebSocket handshake");
        }catch(Exception error){connected.close();throw error;}
    }
    void send(String text) throws IOException {frame(1,text.getBytes(StandardCharsets.UTF_8));}
    String receive() throws IOException {
        ByteArrayOutputStream message=new ByteArrayOutputStream();boolean started=false;
        for(int frames=0;frames<4096;frames++){
            int first=readByte(),second=readByte(),opcode=first&15;boolean fin=(first&128)!=0;
            if((first&112)!=0||(second&128)!=0)throw new IOException("Invalid server frame");
            long length=second&127;if(length==126)length=((long)readByte()<<8)|readByte();else if(length==127){length=0;for(int i=0;i<8;i++){int b=readByte();if(i==0&&b>0)throw new IOException("Frame too large");length=(length<<8)|b;}}
            if(length>LIMIT||length<0||message.size()+length>LIMIT)throw new IOException("Message too large");
            if(opcode>=8&&(!fin||length>125))throw new IOException("Invalid control frame");
            byte[] body=new byte[(int)length];new DataInputStream(in).readFully(body);
            if(opcode==8)throw new EOFException("WebSocket closed");if(opcode==9){frame(10,body);continue;}if(opcode==10)continue;
            if(opcode==1&&!started)started=true;else if(opcode!=0||!started)throw new IOException("Expected text frame");
            message.write(body);if(fin)return new String(message.toByteArray(),StandardCharsets.UTF_8);
        }
        throw new IOException("Too many frames");
    }
    private int readByte() throws IOException {int b=in.read();if(b<0)throw new EOFException();return b;}
    private void frame(int opcode,byte[] body) throws IOException {
        if(body.length>LIMIT)throw new IOException("Frame too large");out.write(128|opcode);int length=body.length;
        if(length<126)out.write(128|length);else if(length<=65535){out.write(128|126);out.write(length>>8);out.write(length);}else{out.write(128|127);for(int i=7;i>=0;i--)out.write((int)(((long)length)>>(i*8)));}
        byte[] mask=new byte[4];random.nextBytes(mask);out.write(mask);byte[] masked=new byte[length];for(int i=0;i<length;i++)masked[i]=(byte)(body[i]^mask[i%4]);out.write(masked);out.flush();
    }
    @Override public void close() throws IOException {socket.close();}
}
