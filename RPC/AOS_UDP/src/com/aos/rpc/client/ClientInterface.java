package com.aos.rpc.client;

import java.io.IOException;
import java.net.UnknownHostException;

import com.aos.rpc.helperClasses.MatrixResolver;


public class ClientInterface 
{
	private ClientStub newCS;
	private long counter = 0;
	public  ClientInterface () throws Exception
	{
		newCS = new ClientStub();
		
	}

	public double[] sort (double[] original) throws UnknownHostException, IOException
	{
		counter ++;
		double[][] result = null;
		//call the client stub
		for (int i = 0; i < 3; i++)
		{

			newCS.stub(1,1,3, counter, Long.valueOf(original.length),1,0,0,original, null);

			result = newCS.getResult();
			if (result != null)
				break;
		}

		if (result == null)
			return null;

		return result[0];
	}
	public double min (double[] original) throws UnknownHostException, IOException
	{
		counter ++;
		double[][] result = null;
		//call the client stub
		for (int i = 0; i < 3; i ++)
		{
			newCS.stub(1,1,1, counter, Long.valueOf(original.length),1,0,0, original, null);
			result = newCS.getResult();
			if (result != null)
				break;
		}

		if (result == null)
			return -1;

		return result[0][0];
	}
	public double max (double[] original) throws UnknownHostException, IOException
	{
		counter ++;
		double[][] result = null;
		//call the client stub
		for (int i = 0; i < 3; i ++)
		{
			newCS.stub(1,1,2, counter, Long.valueOf(original.length),1,0,0, original, null);
			result = newCS.getResult();

			if (result != null)
				break;
		}

		if (result == null)
			return -1;

		return result[0][0];

	}

	public double[][] multiplication (double[][] mat1, double[][] mat2) throws UnknownHostException, IOException
	{
		counter ++;
		double[][] result = null;
		double[] input1, input2;
		MatrixResolver mrMatrixResolver = new MatrixResolver();
		mrMatrixResolver.setMatrix(mat1);
		input1 = mrMatrixResolver.getVectorFromMatrix();
		mrMatrixResolver.setMatrix(mat2);
		input2 = mrMatrixResolver.getVectorFromMatrix();

		for (int i = 0; i < 3; i ++)
		{
			newCS.stub(1,1,4, counter, Long.valueOf(mat1[0].length), Long.valueOf(mat1.length), Long.valueOf(mat2[0].length), Long.valueOf(mat2.length), input1, input2);

			result = newCS.getResult();
			
			if (result != null)
				break;

		}
		
		if (result == null)
			return null;

		return result;
	}

}
