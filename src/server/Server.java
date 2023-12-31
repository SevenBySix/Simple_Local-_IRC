package server;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {

	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		
		boolean run = true;
		int PORT = 194;
		
		DatagramSocket socket = new DatagramSocket(PORT);
		DataManager serverData = new DataManager();
		
		System.out.println("Server started");
		
		
		while(run){
			try {
				
				byte[] buffer = new byte[256];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				socket.receive(request);
				
				System.out.println("Recieved connection at " + request.getAddress());
				
				String text = new String(request.getData(), 0, request.getLength());
				System.out.println(text);
				new ClientHandler(request, socket, serverData);
				//
				
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
		
		}
		
	}
	
	

}

class DataManager{
	
	ArrayList<String> global;
	ArrayList<String> users;
	ArrayList<InetAddress> ips;
	ArrayList<Integer> ports;
	//make list of clients
	
	DataManager(){
		global = new ArrayList<String>();
		users = new ArrayList<String>();
		ips = new ArrayList<InetAddress>();
		ports = new ArrayList<Integer>();
	}
	
	public void addToTextLog(String text) {
		global.add(text);
		
	}
	
	public boolean attemptaddUser(DatagramPacket request) {
		
		if(!ips.contains(request.getAddress())) {
			ips.add(request.getAddress());
			ports.add(request.getPort());
			users.add((new String(request.getData(), 0, request.getLength()).split(" ")[0]));
			
			addToTextLog(request.getAddress().toString() + " joined the room\n");

			System.out.println(new String(request.getData(), 0, request.getLength()).split(" ")[0]);

			
			return true;
		}else {
			return false;
		}
		
	}
	public ArrayList<String> getUsers(){
		return users;
	}
	public ArrayList<InetAddress> getips(){
		return ips;
	}
	public ArrayList<String> getChatLog(){
		return global;
	}
	public ArrayList<Integer> getports(){
		return ports;
	}
	
	public void removeUser(String username) {
		int index = this.users.indexOf(username);
		users.remove(index);
		ips.remove(index);
		ports.remove(index);
	}
}

class ClientHandler implements Runnable {
	
	DatagramSocket socket;
	DatagramPacket packet;
	
	DataManager manager;
	
	DatagramSocket clientsocket;
	DatagramPacket clientpacket;
	
	ClientHandler(DatagramPacket pkt, DatagramSocket sock, DataManager data){
		this.packet = pkt;
		this.socket = sock;
		
		this.manager = data;
		
		
		
		
		
		new Thread(this).start();
		System.out.println("created thread");
		
		
	}
	
	public void run() {
		try {
			
			if(this.manager.attemptaddUser(this.packet)) {
				this.sendHistory(this.packet);

				this.sendWelcomeMessage(this.packet);

			}
			
			String text = new String(this.packet.getData(), 0, this.packet.getLength());
			try {
				this.manager.addToTextLog(text);
			}catch(Exception e) {
				
				this.manager.addToTextLog(text);
			}
			
			
			
			if(text.contains("USER_REQUEST")) {
				System.out.println("user data sent");
				this.sendUserInfo(this.packet);
			}else if(text.contains("GIVE_HISTORY")){
				
				this.sendHistory(this.packet);

				
			}else if(text.contains("GIVE_IP: ")){
				String[] uwords = text.split(" ");
				String uname = uwords[uwords.length -1 ];
				this.sendUserIP(this.packet, uname);
			
			}else if(text.contains("SENDPM: ")){
				String[] uwords = text.split(" ");
				//format 
				String fromuser = uwords[1];
				String touser = uwords[3];
				
				System.out.println("TO USER: "+ touser);
				
				System.out.println("debug");
				String message = text.substring(text.indexOf(touser) + touser.length());
				String formattedMessage = "PM from " + fromuser + ": " + message;
				String senderMessage = "PM to " + touser + ": " + message;
				System.out.println(message);
				//bob (06:54:43 PM): send_pm otherUser this is the pm
				
				this.sendPM(this.packet, fromuser, touser, formattedMessage, senderMessage);
			

			}else if(text.contains("CLIENTEXIT")){
				
				String exitMessage = this.manager.getUsers().get(this.manager.getips().indexOf(this.packet.getAddress())) + " Left the room";
				byte[] bytes = exitMessage.getBytes();
				this.packet.setData(bytes);
				this.broadcast(this.packet, this.manager);
				this.handleExit(this.packet);
			}else{
				this.broadcast(this.packet, this.manager);
			}
				
			
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
	}//message, address, port
	public void broadcast(DatagramPacket packet, DataManager data) throws UnknownHostException {
		int i = 0;
		System.out.println("broadcasting...");
		for(InetAddress entry : this.manager.getips()){
			
			
			
			try {
				DatagramPacket toclient = new DatagramPacket(packet.getData(), packet.getLength(), 
						entry, manager.ports.get(i));
				this.socket.send(toclient);
				System.out.println("broadcast sent to " + entry + "/"+ manager.ports.get(i));
			}catch(Exception e){
				System.out.println(e.getMessage());
				System.out.println("failed broadcast to" + entry);
			}
			
			i++;
		}
	}
	
	public void sendHistory(DatagramPacket packet) {
		
		for(String oldmessage : this.manager.getChatLog()) {
			try {
				byte[] data = oldmessage.getBytes();
				DatagramPacket toclient = new DatagramPacket(data, data.length, 
				packet.getAddress(), packet.getPort());
				this.socket.send(toclient);
				System.out.println("sending chat history message" + oldmessage);
			}catch(Exception e){
				System.out.println(e.getMessage());
				System.out.println("failed to send history message to" + packet.getAddress());
			}
		}
		
	}
	

	public void sendWelcomeMessage(DatagramPacket packet) {
		
		
			try {
				String welcome = "Welcome to the chat room, " + this.manager.getUsers().get(this.manager.getips().indexOf(packet.getAddress()));
				byte[] data = welcome.getBytes();
				DatagramPacket toclient = new DatagramPacket(data, data.length, 
				packet.getAddress(), packet.getPort());
				this.socket.send(toclient);
				System.out.println("Sending welcome message to " + packet.getAddress());
			}catch(Exception e){
				System.out.println(e.getMessage());
				System.out.println("failed to send welcome message" + packet.getAddress());
			}
		
		
	}
	
	public void sendUserInfo(DatagramPacket packet) {
		ArrayList<String> userinfo = new ArrayList<String>();
		int i = 0;
		
		for(String user : this.manager.getUsers()) {
			String info = "DATA: " + user + " " + this.manager.getips().get(i).toString() + " " + this.manager.getports().get(i);
			userinfo.add(info);
			i++;
		}
		
		for(String user : userinfo) {
			try {
				byte[] data = user.getBytes();
				DatagramPacket toclient = new DatagramPacket(data, data.length, 
										packet.getAddress(), packet.getPort());
				this.socket.send(toclient);
				System.out.println("sending user from list: " + user);
			}catch(Exception e){
				System.out.println(e.getMessage());
				System.out.println("failed to send username to " + packet.getAddress());
			}
		}
	}
	
	public void sendUserIP(DatagramPacket packet, String username) {
		try {
			String ip = this.manager.getips().get(this.manager.getUsers().indexOf(username)).toString();
			byte[] data = ip.getBytes();
			DatagramPacket toclient = new DatagramPacket(data, data.length, 
			packet.getAddress(), packet.getPort());
			this.socket.send(toclient);
			System.out.println("Sending ip for user " + username + " to " + packet.getAddress());
		}catch(Exception e){
			System.out.println(e.getMessage());
			System.out.println("failed to send ip to " + username + " to " + packet.getAddress());
		}
	}
	
	public void sendPM(DatagramPacket packet, String fromUsername, String toUsername, String message, String textForSender) {
		try {
			
			byte[] data = message.getBytes();
			byte[] dataforsender = textForSender.getBytes()
;			InetAddress address = this.manager.getips().get(this.manager.getUsers().indexOf(toUsername));
			
			int port = this.manager.getports().get(this.manager.getUsers().indexOf(toUsername));
			
			DatagramPacket toclient = new DatagramPacket(data, data.length, 
					address, port);
			
			DatagramPacket tosender = new DatagramPacket(dataforsender, dataforsender.length, 
					packet.getAddress(), packet.getPort());
			
			this.socket.send(toclient);
			this.socket.send(tosender);
			System.out.println("Sending PM to/from user " + fromUsername + " to " + toUsername);
		}catch(Exception e){
			System.out.println(e.getMessage());
			System.out.println("failed to send PM to " + fromUsername + " to " + packet.getAddress());

		}
	}
	
	public void handleExit(DatagramPacket packet) {
		try {
			String username = this.manager.getUsers().get(this.manager.getips().indexOf(packet.getAddress()));
			this.manager.removeUser(username);
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
}

