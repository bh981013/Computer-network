package client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;


class put{
	boolean isFile;
	String fileName;
	short size;
	
	put(){
		
	}
	
	void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	void checkFile() {
		File file = new File(fileName);
		if(file.exists() == true && file.isFile() == true) isFile = true;
		else isFile = false;
	}
	
	void getSize() {
		File file = new File(fileName);
		size = (short)file.length();
	}
	
}


public class MainClient {
	static void delay(int n){
		while(n != 0) {
			n--;
		}
	}
	
	
	public static void main(String argv[]) throws Exception
	{
		
	String serverIP = "127.0.0.1";
	int contPort = 2020;
	int dataPort = 2121;
	if(argv.length == 3) {
		serverIP = argv[0];
		contPort = Integer.parseInt(argv[1]);
		dataPort = Integer.parseInt(argv[2]);
	}
		
	String sentence;
	String serverSentence = new String("");
	
	BufferedReader inFromUser =
			new BufferedReader(new InputStreamReader(System.in));
	
	Socket clientSocket = new Socket(serverIP, contPort);
	
	DataOutputStream outToServer =
			new DataOutputStream(clientSocket.getOutputStream());
	
	BufferedReader inFromServer =
			new BufferedReader(new
					InputStreamReader(clientSocket.getInputStream()));
	
	

	do{
			System.out.print("명령을 입력하세요: ");
			sentence = inFromUser.readLine();
			
			int index = sentence.indexOf(" ");
			String ORDER;
			String VALUE = "";
			if(index != -1) {
				ORDER = sentence.substring(0,index);
				VALUE = sentence.substring(index + 1);
			}
			else ORDER = sentence;
			if( !ORDER.equals("GET") && !ORDER.equals("PUT") && !ORDER.equals("LIST")
					&& !ORDER.equals("CD") && !ORDER.equals("QUIT")) {
				System.out.println("505: invalid Order");
				continue;
			}
			
			if(ORDER.equals("QUIT")) break;
			put PUT = new put();
			if(ORDER.equals("PUT")) {
				PUT.setFileName(VALUE);
				PUT.checkFile();
				if(PUT.isFile) {
					PUT.getSize();
					sentence = sentence + "  size: " + PUT.size;
				}
				else {
					System.out.println("404: file not existing");
					continue;
				}
			}
			
			//sentence를 보냄.
			outToServer.writeBytes(sentence + '\n');
		
			
			delay(10000);
			serverSentence =inFromServer.readLine();
			if(serverSentence == null) {
				System.out.println("no response");
				continue;
			}
			serverSentence = serverSentence.replace("  ", "\r\n");
			
			System.out.println(serverSentence);
			
			int errindex = serverSentence.indexOf("404:");
			int hasSize = serverSentence.indexOf("size:");
			int size = 0;
			if(hasSize != -1) {
				String temp = serverSentence.substring(6);
				size = Integer.parseInt(temp);
			}
			//"GET" 혹은 "PUT"명령이 들어오면 새로운 연결
			if((ORDER.equals("GET") || ORDER.equals("PUT")) && errindex == -1)
			{
				String fileName;
				
				index = VALUE.lastIndexOf("/");
				if(index != -1) {
				fileName =  VALUE.substring(index + 1);
				}
				else fileName = VALUE;
					
				Socket dataSocket = new Socket(serverIP, dataPort);
			
				//"GET"명령이 들어오면, 파일 받을준비
				if(ORDER.equals("GET")) {
					boolean exist = false;
					
					Path myPath = Paths.get("."); 
					Path realPath = myPath.toRealPath();
					String filePath = realPath.toString();
					System.out.println(filePath);
					File file = new File(filePath + '/' + fileName);
						
					if(file.exists() == false) file.createNewFile();
					else exist = true;
					InputStream fromServer = 
							new DataInputStream(dataSocket.getInputStream());
					
					BufferedOutputStream fileOut = 
							new BufferedOutputStream(new
									FileOutputStream(file));
					
					int dataSize = 1005;
					int chunkSize = 1000;
					byte[] data = new byte[dataSize];
					byte[] chunk = new byte[chunkSize];
					int check;
					int sum = 0;
					
					System.out.print("file receiving....: ");
					while(true) {
						check = fromServer.read(data);
						System.out.print("#");
						System.arraycopy(data, 5, chunk, 0, chunkSize);
						sum = sum + check;
						if(exist == false) { 
							fileOut.write(chunk, 0, check - 5);
						}
						if(sum >= size) break;
					}
					System.out.print("\n");
					
					if(exist == true) System.out.println("err: existing filename");
					else System.out.println("file received successfully");
					fileOut.close();
				}
				
				if(ORDER.equals("PUT")) {
					byte SeqNo = 0;
					long CHKsum = 0;
					
					BufferedInputStream fileRead = 
							new BufferedInputStream(new FileInputStream(fileName));
					
					DataOutputStream fileToServer = 
							new DataOutputStream(dataSocket.getOutputStream());
					
					BufferedReader response =
							new BufferedReader(new
									InputStreamReader(dataSocket.getInputStream()));
				
					int dataSize = 1005;
					byte[] data = new byte[dataSize];
					data[0] = SeqNo;
					data[1] = (byte)(CHKsum >> 8);
					data[2] = (byte)CHKsum;
					data[3] = (byte)(PUT.size >> 8);
					data[4] = (byte)PUT.size;
				
					System.out.println("파일크기: " + PUT.size);
					System.out.print("file sending....: ");
					if(PUT.size != 0) {
						while(fileRead.read(data, 5, 1000) != -1) {
							
							System.out.print("#");
							fileToServer.write(data, 0, dataSize);
						}
					}
					else {
						fileToServer.write(data, 0, 5);
					}
					fileToServer.flush();
					System.out.print("\n");
					System.out.print("file sent successfully");
					
					String Sentence = response.readLine();
					System.out.print("\n");
					System.out.println("SERVER: " + Sentence);
					
					fileRead.close();
				}
				dataSocket.close();
			}
			System.out.print("\n");
		}while(true);
	
	clientSocket.close();
	System.out.println("명령어 채널 종료합니다");
	System.out.println("클라이언트 종료합니다");
	} 
}
