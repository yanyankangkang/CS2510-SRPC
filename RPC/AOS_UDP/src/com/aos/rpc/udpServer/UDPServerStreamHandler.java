package com.aos.rpc.udpServer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.aos.rpc.dataMarshalling.UDPUnmarshaller;
import com.aos.rpc.dataMarshalling.UDPMarshaller;

public class UDPServerStreamHandler
{
	private int clientPort, burstSize;
	private long numberOfPacketsToReceive, numberOfPacketsToSend, transactionID;
	private InetAddress clientAddress; 
	private Socket tcpConnection;
	private DatagramSocket udpSocket;
	private UDPUnmarshaller unmarshaller;
	private UDPMarshaller marshaller;
	private UDPUnmarshaller[] recievedParametersPackets;
	private UDPMarshaller[] toSendResultsPackets;

	public UDPServerStreamHandler(Socket tcpConnection) throws IOException
	{
		this.tcpConnection = tcpConnection;
		unmarshaller = new UDPUnmarshaller();
		marshaller = new UDPMarshaller();
		burstSize = 5;
		udpSocket = new DatagramSocket(0);
		udpSocket.setSoTimeout(10000);
		sendPortAndCloseTCP();
	}
	
	private void sendPortAndCloseTCP() throws IOException
	{
		DataOutputStream out = new DataOutputStream(tcpConnection.getOutputStream());
		out.writeInt(udpSocket.getLocalPort());
		out.flush();
		tcpConnection.close();
	}
	
	private void sendAck(long neededPacket) throws IOException
	{
		marshaller.setTransactionID(transactionID);
		marshaller.setSequenceID(neededPacket);
		marshaller.setVectorSize(0);
		marshaller.setType((short)2);
		marshaller.formStream();
		byte[] stream = marshaller.getStream();
		DatagramPacket ack = new DatagramPacket(stream, stream.length, clientAddress, clientPort);
		udpSocket.send(ack);
	}
	
	private byte[] getTheRightStream(byte[] bufferStream, int length)
	{
		byte[] result = new byte[length];
		
		for (int i = 0; i < length; i++)
			result[i] = bufferStream[i];
		
		return result;
	}

	public void sendUdpResultsPackets() throws IOException
	{
		boolean flag = true;
		int attempts = 0;
		long packetToSendNum = 0;

		while((attempts < 3) && (packetToSendNum < numberOfPacketsToSend) && flag)
		{
			for (int i = 0; i < burstSize; i++)
			{
                if (packetToSendNum + i >= numberOfPacketsToSend) {
                    packetToSendNum = packetToSendNum + i;
                    break;
                }
                byte[] stream = toSendResultsPackets[(int)packetToSendNum + i].getStream();
				DatagramPacket packetToSend = new DatagramPacket(stream, stream.length, clientAddress, clientPort);
				udpSocket.send(packetToSend);
			}
			try
			{
				byte[] buffer = new byte[26];
				DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
				udpSocket.receive(receivePacket);
				unmarshaller.resetAll();
                
				unmarshaller.setStream(getTheRightStream(receivePacket.getData(), receivePacket.getLength()));

				if(!unmarshaller.isCRCError()) {
                    packetToSendNum = unmarshaller.getSequenceID() - 1;
                    attempts = 0;
                }
			}
			catch (SocketTimeoutException ste)
			{
				//timeout occured
				attempts++;
				if (attempts == 3)
					flag = false;
			}
		}

		//we reached the maximum number of tries to re-connect
		if (attempts == 3)
		{
			//give up and wait for client to ask for the results in another request
            System.out.println("Error: lost connection with the client");
        }
	}

	public void recieveUdpParametersPackets() throws IOException
	{
		boolean flag = true;
		int attempts = 0;
		long neededPacket = 1;
		recievedParametersPackets = new UDPUnmarshaller[(int)numberOfPacketsToReceive];
		
		while((attempts < 3) && ((neededPacket - 1) < numberOfPacketsToReceive))
		{
			flag = true;
			for (int i = 0; i < burstSize && flag && (neededPacket <= numberOfPacketsToReceive); i++)
			{
				try
				{
					byte[] buffer = new byte[4122];
					DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
					udpSocket.receive(receivePacket);
					clientPort = receivePacket.getPort();
					clientAddress = receivePacket.getAddress();
					unmarshaller.setStream(getTheRightStream(receivePacket.getData(), receivePacket.getLength()));
					if(!unmarshaller.isCRCError())
					{
						if(unmarshaller.getSequenceID() == neededPacket)
						{
							recievedParametersPackets[(int)neededPacket-1] = unmarshaller;
							unmarshaller = new UDPUnmarshaller();
							attempts = 0;
							neededPacket++;
						}
						else
							flag = false;
					}
					else
						flag = false;
				}
				catch (SocketTimeoutException ste)
				{
					attempts++;
					if (attempts == 3)
						flag = false;
				}
			}
			if(attempts != 3)
			{
				sendAck(neededPacket);
			}
		}

		if (attempts == 3)
		{
			System.out.println("exceeded the attempts to recieve");
		}
	}
	
	public String getClientAddress()
	{
		return clientAddress.getHostAddress();
	}


	public void setTcpConnection(Socket tcpConnection) {
		this.tcpConnection = tcpConnection;
	}
	
	public UDPUnmarshaller[] getRecievedParametersPackets()
	{
		return this.recievedParametersPackets;
	}
	
	public void setUDPMarshallers(UDPMarshaller[] results)
	{
		this.toSendResultsPackets = results;
	}

	public void setNumberOfPacketsToReceive(long numberOfPacketsToReceive) {
		this.numberOfPacketsToReceive = numberOfPacketsToReceive;
	}
	
	public void setNumberOfPacketsToSend(long numberOfPacketsToSend) {
		this.numberOfPacketsToSend = numberOfPacketsToSend;
	}

	public void setTransactionID(long transactionID)
	{
		this.transactionID = transactionID;
	}
}
