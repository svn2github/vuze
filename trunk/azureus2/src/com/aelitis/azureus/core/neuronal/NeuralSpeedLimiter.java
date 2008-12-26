package com.aelitis.azureus.core.neuronal;

public class NeuralSpeedLimiter {
	
	//in bytes / sec
	long maxDlSpeed;
	long maxUlSpeed;
	long ulSpeed;
	long dlSpeed;
	
	//in ms
	long minLatency;
	long maxLatency;
	long latency;
	
	NeuralNetwork neuralNetwork;
	
	private boolean dirty;
	
	double trainingSet[][] = new double[][] {
			//dl speed,	ul speed,	latency,	no_down_limit,	down_limit,	no_up_limit,	up_limit
			
			//no latency => no limits
			{0.0,		0.0,		0.0,		0.9,			0.9,		0.9,			0.9},
			{0.0,		1.0,		0.0,		0.9,			0.9,		0.9,			0.9},
			{1.0,		0.0,		0.0,		0.9,			0.9,		0.9,			0.9},
			{1.0,		1.0,		0.0,		0.9,			0.9,		0.9,			0.9},
			
			//no download, high upload, mid-high latency => limit upload
			{0.0,		1.0,		1.0,		0.9,			0.9,		0.1,			0.1},
			{0.0,		1.0,		0.5,		0.9,			0.9,		0.1,			0.1},
			
			//no download, high upload, low-mid latency => some limit upload
			{0.0,		1.0,		0.2,		0.9,			0.9,		0.1,			0.8},
			{0.0,		1.0,		0.3,		0.9,			0.9,		0.1,			0.6},
			
			//no upload, high download, mid-high latency => limit download
			{1.0,		0.0,		1.0,		0.1,			0.7,		0.9,			0.9},
			{1.0,		0.0,		0.5,		0.1,			0.7,		0.9,			0.9},
			
			//no upload, high download, low-mid latency => some limit download
			{1.0,		0.0,		0.2,		0.1,			0.9,		0.9,			0.9},
			{1.0,		0.0,		0.3,		0.1,			0.8,		0.9,			0.9},
	};
	
	public NeuralSpeedLimiter() {
		neuralNetwork = new NeuralNetwork(3,4,4);
		neuralNetwork.setLearningRate(0.05);
		neuralNetwork.setMomentum(true, 0.9);
		neuralNetwork.setActivationFunction(new LogisticActivationFunction());
		train();
		dirty = false;
	}

	private void train() {
		//System.out.println(neuralNetwork);
		double error = 1.0;
		int c = 0;
		while(error > 0.002 && c < 200000) {
			
			error = 0;
			for(int i = 0 ; i <trainingSet.length ; i++) {
				neuralNetwork.setInput(0, trainingSet[i][0]);
				neuralNetwork.setInput(1, trainingSet[i][1]);
				neuralNetwork.setInput(2, trainingSet[i][2]);
				
				neuralNetwork.setDesiredOutput(0, trainingSet[i][3]);
				neuralNetwork.setDesiredOutput(1, trainingSet[i][4]);
				neuralNetwork.setDesiredOutput(2, trainingSet[i][5]);
				neuralNetwork.setDesiredOutput(3, trainingSet[i][6]);
				
				neuralNetwork.feedForward();
				error += neuralNetwork.calculateError();
				neuralNetwork.backPropagate();
				
			}
			
			error /= trainingSet.length;
			
			c++;
		}
		
		
		//System.out.println(neuralNetwork);
		
//		for(int i = 0 ; i <trainingSet.length ; i++) {
//			neuralNetwork.setInput(0, trainingSet[i][0]);
//			neuralNetwork.setInput(1, trainingSet[i][1]);
//			neuralNetwork.setInput(2, trainingSet[i][2]);
//			
//			neuralNetwork.feedForward();
//			
//			System.out.print(trainingSet[i][0] + ", " + trainingSet[i][1] + ", " + trainingSet[i][2] + " > expecting :\n\t\t");
//			System.out.print(trainingSet[i][3] + ",\t" + trainingSet[i][4] + ",\t" + trainingSet[i][5] + ",\t" + trainingSet[i][6] + "\t> output :\n\t\t");
//			System.out.println(neuralNetwork.getOutput(0) + ",\t" + neuralNetwork.getOutput(1) + ",\t" + neuralNetwork.getOutput(2) + ",\t" + neuralNetwork.getOutput(3));
//			
//			neuralNetwork.feedForward();
//			
//		}
		
	}

	private void resetInput() {
		try {
			if(ulSpeed > maxUlSpeed) maxUlSpeed = ulSpeed;
			if(dlSpeed > maxDlSpeed) maxDlSpeed = dlSpeed;
			if(latency > maxLatency) maxLatency = latency;
			if(latency < minLatency) minLatency = latency;
			
			double downloadFactor = (double)dlSpeed / maxDlSpeed;
			double uploadFactor = (double)ulSpeed / maxUlSpeed;
			double latencyFactor = ((double)latency - (double)minLatency) / maxLatency;
			
			neuralNetwork.setInput(0, downloadFactor);
			neuralNetwork.setInput(1, uploadFactor);
			neuralNetwork.setInput(2, latencyFactor);
			
			dirty = true;
		} catch(Throwable t) {
			//Ignore
		}
	}
	
	private void feedForward() {
		neuralNetwork.feedForward();
		dirty = false;
		System.out.println("input : " + (double)dlSpeed/maxDlSpeed + ", " +  (double)ulSpeed/maxUlSpeed + ", " + ((double)latency-(double)minLatency)/maxLatency);
		System.out.println("output : " + neuralNetwork.getOutput(0) + ", " + 
				neuralNetwork.getOutput(1) + ", " +
				neuralNetwork.getOutput(2) + ", " +
				neuralNetwork.getOutput(3));
		
	}
	
	public void setMaxDlSpeed(long maxDlSpeed) {
		if(maxDlSpeed > 0) {
			this.maxDlSpeed = maxDlSpeed;
		}
		resetInput();
	}

	public void setMaxUlSpeed(long maxUlSpeed) {
		if(maxUlSpeed > 0) {
			this.maxUlSpeed = maxUlSpeed;
		}
		resetInput();
	}

	public void setMinLatency(long minLatency) {
		if(minLatency >= 0) {
			this.minLatency = minLatency;
		}
		resetInput();
	}

	public void setUlSpeed(long ulSpeed) {
		if(ulSpeed >= 0) {
			this.ulSpeed = ulSpeed;
		}
		resetInput();
	}

	public void setDlSpeed(long dlSpeed) {
		if(dlSpeed >= 0) {
			this.dlSpeed = dlSpeed;
		}
		resetInput();
	}

	public void setLatency(long latency) {
		if(latency >= 0) {
			this.latency = latency;
		}
		resetInput();
	}

	public void setMaxLatency(long maxLatency) {
		if(maxLatency > 0) {
			this.maxLatency = maxLatency;
		}
		resetInput();
	}
	
	public boolean shouldLimitDownload() {
		if(dirty) feedForward();
		return neuralNetwork.getOutput(0) < 0.5;
	}
	
	public long getDownloadLimit() {
		if(dirty) feedForward();
		return (long) ( 1.2* maxDlSpeed * neuralNetwork.getOutput(1));
		
	}
	
	public boolean shouldLimitUpload() {
		if(dirty) feedForward();
		return neuralNetwork.getOutput(2) < 0.5;
	}
	
	public long getUploadLimit() {
		if(dirty) feedForward();
		return (long) ( 1.2* maxUlSpeed * neuralNetwork.getOutput(3));
	}
	
	public static void main(String args[]) {
		new NeuralSpeedLimiter();
	}

}
