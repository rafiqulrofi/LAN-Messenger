/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 *
 * @author Hp
 */
public class SocketThread implements Runnable {
    
     Socket socket;
    MainForm main;
    DataInputStream dis;
    StringTokenizer st;
    String client, filesharing_username;
    
    public SocketThread(Socket socket, MainForm main){
        this.main = main;
        this.socket = socket;
        
        try {
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            main.appendMessage("[SocketThreadIOException]: "+ e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            while(true){
                /** Get Client Data **/
                String data = dis.readUTF();
                st = new StringTokenizer(data);
                String CMD = st.nextToken();
                /** Check CMD **/
                switch(CMD){
                    case "CMD_JOIN":
                        /** CMD_JOIN [clientUsername] **/
                        String clientUsername = st.nextToken();
                        client = clientUsername;
                        main.setClientList(clientUsername);
                        main.setSocketList(socket);
                        main.appendMessage("[Client]: "+ clientUsername +" joins the chatroom.!");
                        break;
                        
                    case "CMD_CHAT":
                        /** CMD_CHAT [from] [sendTo] [message] **/
                        String from = st.nextToken();
                        String sendTo = st.nextToken();
                        String msg = "";
                        while(st.hasMoreTokens()){
                            msg = msg +" "+ st.nextToken();
                        }
                        Socket tsoc = main.getClientList(sendTo);
                        try {
                            DataOutputStream dos = new DataOutputStream(tsoc.getOutputStream());
                            /** CMD_MESSAGE **/
                            String content = from +": "+ msg;
                            dos.writeUTF("CMD_MESSAGE "+ content);
                            main.appendMessage("[Message]: From "+ from +" To "+ sendTo +" : "+ msg);
                        } catch (IOException e) {  main.appendMessage("[IOException]: Unable to send message to "+ sendTo); }
                        break;
                    
                    case "CMD_CHATALL":
                        /** CMD_CHATALL [from] [message] **/
                        String chatall_from = st.nextToken();
                        String chatall_msg = "";
                        while(st.hasMoreTokens()){
                            chatall_msg = chatall_msg +" "+st.nextToken();
                        }
                        String chatall_content = chatall_from +" "+ chatall_msg;
                        for(int x=0; x < main.clientList.size(); x++){
                            if(!main.clientList.elementAt(x).equals(chatall_from)){
                                try {
                                    Socket tsoc2 = (Socket) main.socketList.elementAt(x);
                                    DataOutputStream dos2 = new DataOutputStream(tsoc2.getOutputStream());
                                    dos2.writeUTF("CMD_MESSAGE "+ chatall_content);
                                } catch (IOException e) {
                                    main.appendMessage("[CMD_CHATALL]: "+ e.getMessage());
                                }
                            }
                        }
                        main.appendMessage("[CMD_CHATALL]: "+ chatall_content);
                        break;
                    
                    case "CMD_SHARINGSOCKET":
                        main.appendMessage("CMD_SHARINGSOCKET : Client stablish a socket connection for file sharing...");
                        String file_sharing_username = st.nextToken();
                        filesharing_username = file_sharing_username;
                        main.setClientFileSharingUsername(file_sharing_username);
                        main.setClientFileSharingSocket(socket);
                        main.appendMessage("CMD_SHARINGSOCKET : File sharing connected...");
                        break;
                    
                    case "CMD_SENDFILE":
                        main.appendMessage("CMD_SENDFILE : Client sending a file...");
                        /*
                        Format: CMD_SENDFILE [Filename] [Recipient] [Consignee] 
                        */
                        String file_name = st.nextToken();
                        String sendto = st.nextToken();
                        String consignee = st.nextToken();
                        /**  Get the client Socket **/
                        main.appendMessage("CMD_SENDFILE : preparing connections..");
                        Socket cSock = main.getClientFileSharingSocket(sendto); /* Consignee Socket  */
                        /*   Now Check if the consignee socket was exists.   */
                        if(cSock != null){ /* Exists   */
                            try {
                                main.appendMessage("CMD_SENDFILE : Connected..!");
                                /** First Write the filename..  **/
                                main.appendMessage("CMD_SENDFILE : Sending file to client...");
                                DataOutputStream cDos = new DataOutputStream(cSock.getOutputStream());
                                cDos.writeUTF("CMD_SENDFILE "+ file_name);
                                /** Second send now the file content  **/
                                InputStream input = socket.getInputStream();
                                OutputStream sendFile = cSock.getOutputStream();
                                byte[] buffer = new byte[1024];
                                int cnt;
                                while((cnt = input.read(buffer)) > 0){
                                    sendFile.write(buffer, 0, cnt);
                                }
                                sendFile.flush();
                                sendFile.close();
                                /** Remove client list **/
                                main.removeClientFileSharing(sendto);
                                main.removeClientFileSharing(consignee);
                                main.appendMessage("CMD_SENDFILE : File was send to client...");
                            } catch (IOException e) {
                                main.appendMessage("[CMD_SENDFILE]: "+ e.getMessage());
                            }
                        }else{ /*   Not exists, return error  */
                            /*   FORMAT: CMD_SENDFILEERROR  */
                            main.removeClientFileSharing(consignee);
                            main.appendMessage("CMD_SENDFILE : Client '"+sendto+"' was not found.!");
                            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                            dos.writeUTF("CMD_SENDFILEERROR "+ "Client '"+sendto+"' was not found, File Sharing will exit.");
                        }                        
                        break;
                        
                    default: 
                        main.appendMessage("[CMDException]: Unknown Command "+ CMD);
                    break;
                }
            }
        } catch (IOException e) {
            /*   this is for chatting client, remove if it is exists..   */
            System.out.println(client);
            System.out.println("File Sharing: " +filesharing_username);
            main.removeFromTheList(client);
            if(filesharing_username != null){
                main.removeClientFileSharing(filesharing_username);
            }
            main.appendMessage("[SocketThread]: Client connection closed..!");
        }
    }
    
}
    
