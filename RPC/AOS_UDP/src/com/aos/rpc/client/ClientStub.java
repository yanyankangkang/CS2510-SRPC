package com.aos.rpc.client;


import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.management.relation.Relation;

import com.aos.rpc.dataMarshalling.TCPMapperRequestMarshaller;
import com.aos.rpc.dataMarshalling.TCPReplyUnmarshaller;
import com.aos.rpc.dataMarshalling.TCPRequestMarshaller;
import com.aos.rpc.dataMarshalling.UDPUnmarshaller;
import com.aos.rpc.helperClasses.ClientDesegmentation;
import com.aos.rpc.helperClasses.ClientSegmentation;

public class ClientStub
{
	private ClientRPCRuntime runtime;
	private double[][] result = null;
	
	public ClientStub () throws Exception
	{
		 runtime = new ClientRPCRuntime ();
	}

	public void stub(long prog, long progV, long proc, long tranID, long elem1c, long elem1r, long elem2c, long elem2r,double[] vector1, double[] vector2)
			throws UnknownHostException, IOException
	{

		
		//-------------------------------------------------
		//marshal the request to the PortMapper
		//-------------------------------------------------
        TCPMapperRequestMarshaller requestToPMMarshaller = new TCPMapperRequestMarshaller();
        requestToPMMarshaller.setType((short) 1);
        requestToPMMarshaller.setRequestType((short)1);
        requestToPMMarshaller.setProgramNumber(prog);
        requestToPMMarshaller.setProgramVersion(progV);
        requestToPMMarshaller.setProcedureNumber(proc);
        requestToPMMarshaller.formStream();

        byte[] requestToPortMapper = requestToPMMarshaller.getStream();
   		
        
	    runtime.requestMethodToPortMapper(requestToPortMapper);
		


        //segmentation

        ClientSegmentation seg = new ClientSegmentation(vector1, vector2, tranID);
        seg.processingSegmentation();


        //------------


        //-------------------------------------------------
        //marshal the request to the Server
        //-------------------------------------------------

	    TCPRequestMarshaller requestToServerMarshaller = new TCPRequestMarshaller();

	    requestToServerMarshaller.setTransactionID(tranID);
	    requestToServerMarshaller.setProgramNumber(prog);
	    requestToServerMarshaller.setProgramVersion(progV);
	    requestToServerMarshaller.setProcedureNumber(proc);
	    requestToServerMarshaller.setNumberOfElements1_c(elem1c);
	    requestToServerMarshaller.setNumberOfElements1_r(elem1r);
	    requestToServerMarshaller.setNumberOfElements2_c(elem2c);
	    requestToServerMarshaller.setNumberOfElements2_r(elem2r);
	    requestToServerMarshaller.setType((short)1);
	    requestToServerMarshaller.setNumOfPackets(seg.getUDPMarshallers().length);
	    if(requestToServerMarshaller.formStream() == false)
            System.out.println("Failed in form the stream of 'TCP request to the server' marshaller !!!");

        byte[] requestToServer = requestToServerMarshaller.getStream();
   		
	    runtime.requestMethodToServer(this, requestToServer);

        int resultSizeToReceive;
        if (elem2c == 0)
        {
            if (proc == 3)
                resultSizeToReceive = (int) elem1c;
            else
                resultSizeToReceive = 1;

        }
        else
            resultSizeToReceive = (int)(elem1r * elem2c);

        UDPUnmarshaller[] udpUnmarshallers = runtime.dataToServer(seg.getUDPMarshallers(), resultSizeToReceive , tranID);
        ClientDesegmentation deseg = new ClientDesegmentation(udpUnmarshallers);
        deseg.setRowSize(elem1r);

        deseg.setColumnSize(resultSizeToReceive/elem1r);
        if (udpUnmarshallers != null)
        	deseg.reorganize();
        
        result = deseg.getVectorFinal();

    }
	
//	public void unmarshalReplyFromServer (byte[] response)
//	{
//		long tranID;
//		long elem1c, elem1r, elem2c, elem2r;
//		double[] vector1, vector2;
//
//		TCPReplyUnmarshaller replyUnmarshaller = new TCPReplyUnmarshaller();
//        replyUnmarshaller.setStream(response);
//        elem1c = replyUnmarshaller.getNumberOfElements1_c();
//        elem1r = replyUnmarshaller.getNumberOfElements1_r();
//        elem2c = replyUnmarshaller.getNumberOfElements2_c();
//        elem2r = replyUnmarshaller.getNumberOfElements2_r();
//        tranID = replyUnmarshaller.getTransactionID();
//        vector1 = replyUnmarshaller.getResultVector1();
//        vector2 = replyUnmarshaller.getResultVector2();
//
//
//        result1 = new double[(int) elem1r][(int)elem1c];
//        result2 = new double[(int) elem2r][(int)elem2r];
//
//        for (long i = 0; i < elem1r; i ++)
//        	for (long j = 0; j < elem1c; j ++)
//        		result1[(int) i][(int) j] = vector1[(int) (i*elem1c + j)];
//
//        for (long i = 0; i < elem2r; i ++)
//        	for (long j = 0; j < elem2c; j ++)
//        		result2[(int) i][(int) j] = vector2[(int) (i*elem1c + j)];
//
//
//	}
	
	public double[][]  getResult ()
	{
		return result;
	}
	
	

}
