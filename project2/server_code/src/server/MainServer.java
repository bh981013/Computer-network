package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class cd{
	String input;
	String output;
	boolean isFile = false;
	
	cd(String input){
		this.input = input;
	}
	
	
	String init() throws IOException {
		Path path = Paths.get(".");
		Path realPath = path.toRealPath();
		return realPath.toString();
	}
	
	String getPath(String CurrentDir) {
		Path path = Paths.get(input);
		File dir = new File(path.toString());
		
		if(dir.isAbsolute() == false) {
			input = CurrentDir + "/" + input;
		}
		dir = new File(input);
		
		if(dir.exists() == false) {
			System.out.println("not existing");
			return "err";
		}
		if(dir.isFile() == true) isFile = true;
		Path path2 = Paths.get(input);
		
		try {
			path2 = path2.toRealPath();
		} catch (IOException e) {
			System.out.println("cannot get real path");
			return "err";
		}
		output = path2.toString();
		System.out.println("Directory found successful");
		return output;
	}
}



class ls{
	String path;
	String []list;
	
	ls(String path){
		this.path = path;
	}
	
	void getList(String CurrentDir) {
		File temp = new File(path); 
		File[] contents = temp.listFiles();
		list = new String[contents.length];
		int i = 0;
		for(File file : contents) {
			if(file.isDirectory()) {
				list[i] = file.getName() + "," + "-";
			}
			else {
				list[i] = file.getName() + "," + file.length();
			}
			i++;
		}
	}
}

class get{
	String path;
	
	byte SeqNo = 0;
	short CHKsum = 0;
	int Size = 0;
	
	get(String path){
		this.path = path;
	}
	
	get(){
		
	}
	
	void setPath(String path) {
		this.path = path;
	}
	
	short getSize() {
		short Size;
		File file = new File(path);
		Size = (short)(file.length());
		return Size;
	}
	
	void getFile(int dataPort) throws IOException {
		

		ServerSocket dataSocket = new ServerSocket(dataPort);
		
		
		Socket connectionSocket = new Socket();
		connectionSocket = dataSocket.accept();
	
		
		File file = new File(path);
		BufferedInputStream fileRead = 
				new BufferedInputStream(new FileInputStream(file));
		
		
		DataOutputStream fileToClient = 
				new DataOutputStream(connectionSocket.getOutputStream());
		
		
		int dataSize = 1005;
		long fileSize = file.length();
		byte[] data = new byte[dataSize];
		data[0] = SeqNo;
		data[1] = (byte)(CHKsum >> 8);
		data[2] = (byte)CHKsum;
		data[3] = (byte)(fileSize >> 8);
		data[4] = (byte)fileSize;
	
		System.out.print("file sending....: ");
		while(fileRead.read(data, 5, 1000) != -1) {
			
			System.out.print("#");
			fileToClient.write(data, 0, dataSize);
		}
		System.out.print("\n");
		System.out.print("file sent successfully");
		fileToClient.flush();
		
		
		fileRead.close();
		connectionSocket.close();
		dataSocket.close();
		
	}
	
}

class put{
	int size;
	String fileName;
	
	void setPUT(int size, String fileName){
		this.size = size;
		this.fileName = fileName;
	}
	
	void getFile(int dataPort) throws IOException {
		
		ServerSocket dataSocket = new ServerSocket(dataPort);
		
		
		Socket connectionSocket = new Socket();
		connectionSocket = dataSocket.accept();
	
		
		boolean exist = false;
		File file = new File(fileName);
		if(file.exists() == false) file.createNewFile();
		else exist = true;
		
		InputStream fromClient = 
				new DataInputStream(connectionSocket.getInputStream());
		
		BufferedOutputStream fileOut = 
				new BufferedOutputStream(new
						FileOutputStream(file));
		
		DataOutputStream response =
				new DataOutputStream(connectionSocket.getOutputStream());
		
		int dataSize = 1005;
		int chunkSize = 1000;
		byte[] data = new byte[dataSize];
		byte[] chunk = new byte[chunkSize];
		int check;
		int sum = 0;
		
		System.out.print("file receiving....: ");
		while(true) {
			check = fromClient.read(data);
			System.out.print("#");
			System.arraycopy(data, 5, chunk, 0, chunkSize);
			sum = sum + check;
			if(exist == false) { 
				fileOut.write(chunk, 0, check - 5);
			}
			if(sum >= size) break;
		}
		System.out.print("\n");
	
		String serverSentence = "";
		if(exist == true) {
			serverSentence = "err: existing filename";
			System.out.println(serverSentence);
			
		}
		else {
			serverSentence = "PUT successful"; 
			System.out.println(serverSentence);
			response.writeBytes(serverSentence + "\n");
		}
		
		fileOut.close();
		connectionSocket.close();
		dataSocket.close();
	}
	
}


public class MainServer {
	public static void main(String argv[]) throws Exception{
		int contPort = 2020;
		int dataPort = 2121;
		if(argv.length == 2) {
			contPort = Integer.parseInt(argv[0]);
			dataPort = Integer.parseInt(argv[1]);
		}
		
		String clientSentence;
		
		ServerSocket welcomeSocket = new ServerSocket(contPort);
		
		while(true) {
			Socket connectionSocket = new Socket();
			cd init = new cd(".");
			String CurrentDir = new String(init.init());
			
			while(true) {
				connectionSocket = welcomeSocket.accept();
				break;
			}
			while(true) {
				try{
					BufferedReader inFromClient =
							new BufferedReader(new
									InputStreamReader(connectionSocket.getInputStream())); 
					
					DataOutputStream outToClient = 
							new DataOutputStream(connectionSocket.getOutputStream());
					//Writer outToClient = new OutputStreamWriter(outStream);
					
					clientSentence = inFromClient.readLine();
					if(clientSentence.indexOf("  ") != -1) {
						clientSentence = clientSentence.replace("  ", "\r\n");
					}
					//clientSentence 받아옴
					System.out.println("Request: " + clientSentence);
					if(clientSentence == null) continue;
					int index = clientSentence.indexOf(" ");
					
					String ORDER = new String();
					String VALUE = new String();
					String OUTPUT = new String();
					
					get GET = new get();
					put PUT = new put();
					
					if(index != -1) {
						ORDER = clientSentence.substring(0, index);
						VALUE = clientSentence.substring(index + 1);
					}
					else {
						ORDER = clientSentence;
						VALUE = "";
					}
					
					
					
					//만약 "CD"가 입력됐으면
					if(ORDER.equals("CD")) {
						cd CD = new cd(VALUE);
						OUTPUT = CD.getPath(CurrentDir);
						if(CD.isFile == true) {
							OUTPUT = "404: a file, not directory";
						}
						else if(OUTPUT != "err") {
							CurrentDir = OUTPUT;
							OUTPUT = "Current Dir: " + OUTPUT;
						}
						else {
							OUTPUT = "404: Invalid Directory";
						}
					}
					
					//만약 "LIST가 입력됐다면"
					else if(ORDER.equals("LIST")){
						cd CD = new cd(VALUE);
						String path = CD.getPath(CurrentDir);
						

						if(path.equals("err")) {
							System.out.println("err발생");
							OUTPUT = "404: Invalid Directory";
						}
						else{
							File file = new File(path);
						
						
							if(!path.equals("err")) 
							{
								
								if(file.isDirectory() == false) {
									OUTPUT = "404: Not a directory: a file";
								}
								else {
									CurrentDir = path;
									ls LS = new ls(path);
									LS.getList(CurrentDir);
									OUTPUT = "";
									for(String temp: LS.list) 
									{
										OUTPUT = OUTPUT + temp + "  ";
									}
								}
							}	
						}
					}
					
					//만약 "GET" 입력됐다면
					
					else if(ORDER.equals("GET")) {
	
						cd CD = new cd(VALUE);
						
						String path = CD.getPath(CurrentDir);
						System.out.println(path);
						File file = new File(path);
						if(path == "err") {
							OUTPUT = "404: not existing";
						}
						else {
							if(file.isDirectory() == true) {
								OUTPUT = "404: Not a file, a directory";
							}	
							else{
								GET.setPath(path);
								OUTPUT = "size: " + GET.getSize();
							}
						}
					}
					
					//"PUT"입력
					
					else if(ORDER.equals("PUT")) {
						
						String fileName = VALUE.substring(0, VALUE.indexOf("\r\n"));
						System.out.println("filename: " + fileName);
						if(VALUE.indexOf("size: ") != -1) {
							int sizeIndex = VALUE.lastIndexOf(" ");
							int size = Integer.parseInt(VALUE.substring(sizeIndex + 1));
			
							PUT.setPUT(size, fileName);
						}
						
						
					}
					
					else {
						OUTPUT = "err: INVALID ORDER";
					}
					OUTPUT = OUTPUT + '\n';
					System.out.print("Response: " + OUTPUT);
					outToClient.writeBytes(OUTPUT); 
					
					//추가적인 연결
					int isError = OUTPUT.indexOf("404:");
					if(ORDER.equals("GET") && isError == -1) {
						GET.getFile(dataPort);	
					}
					if(ORDER.equals("PUT")) {
						PUT.getFile(dataPort);
					}
					
					System.out.print("\n");
				}
				catch(SocketException e) {
					connectionSocket.close();
					break;
				}
				catch(NullPointerException e) {
					connectionSocket.close();
					break;
				}
			}
		}
	}
}


